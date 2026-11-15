
package com.example.smartresume.data.database

import com.example.smartresume.data.database.ResumeDataMapper.toResumeData
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

class ResumeSyncManager(
    private val resumeRepository: ResumeRepository,
    private val cloudRepository: ResumeCloudRepository,
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private var sessionJob: Job? = null
    private var localSyncJob: Job? = null
    private var cloudListener: ListenerRegistration? = null

    private val syncMutex = Mutex()

    private val syncedVersions =
        ConcurrentHashMap<String, Long>()

    // Prevent deleted resumes from returning during the active session.
    private val deletedResumeIds =
        ConcurrentHashMap.newKeySet<String>()

    private var activeUserId: String? = null

    private fun userResumesCollection(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("resumes")

    suspend fun startSync(
        userId: String,
        scope: CoroutineScope
    ) {
        require(userId.isNotBlank()) {
            "User ID cannot be empty"
        }

        stopSync()

        activeUserId = userId

        syncedVersions.clear()
        deletedResumeIds.clear()

        // Make the sync session a child of the supplied scope.
        val job = SupervisorJob(scope.coroutineContext[Job])
        val newScope = CoroutineScope(
            scope.coroutineContext + job + Dispatchers.IO
        )

        sessionJob = job

        val collection = userResumesCollection(userId)

        // Initial synchronization.
        syncMutex.withLock {

            val localResumes =
                resumeRepository.getAllResumes().first()

            val cloudSnapshot = collection.get().await()

            val cloudResumes = cloudSnapshot.documents
                .mapNotNull { it.toResumeEntity() }

            val localMap = localResumes.associateBy { it.id }
            val cloudMap = cloudResumes.associateBy { it.id }

            // Merge cloud resumes into Room.
            for (cloudEntity in cloudResumes) {

                if (deletedResumeIds.contains(cloudEntity.id)) {
                    continue
                }

                val cloudResume = cloudEntity.toResumeData()
                val localEntity = localMap[cloudEntity.id]

                if (localEntity == null) {

                    syncedVersions[cloudEntity.id] =
                        cloudResume.updatedAt

                    resumeRepository.insertResume(cloudEntity)

                } else {

                    val localResume = localEntity.toResumeData()

                    when {
                        cloudResume.updatedAt > localResume.updatedAt -> {

                            syncedVersions[cloudEntity.id] =
                                cloudResume.updatedAt

                            resumeRepository.insertResume(cloudEntity)
                        }

                        localResume.updatedAt > cloudResume.updatedAt -> {

                            cloudRepository.uploadResume(
                                userId,
                                localResume
                            )

                            syncedVersions[localEntity.id] =
                                localResume.updatedAt
                        }

                        else -> {
                            syncedVersions[cloudEntity.id] =
                                cloudResume.updatedAt
                        }
                    }
                }
            }

            // Upload local resumes missing from Firestore.
            for (localEntity in localResumes) {

                if (
                    !cloudMap.containsKey(localEntity.id) &&
                    !deletedResumeIds.contains(localEntity.id)
                ) {
                    val resume = localEntity.toResumeData()

                    cloudRepository.uploadResume(
                        userId,
                        resume
                    )

                    syncedVersions[localEntity.id] =
                        resume.updatedAt
                }
            }
        }

        // Observe local Room database changes.
        localSyncJob = newScope.launch {

            resumeRepository.getAllResumes()
                .debounce(1000)
                .collect { entities ->

                    for (entity in entities) {

                        try {
                            syncMutex.withLock {

                                if (deletedResumeIds.contains(entity.id)) {
                                    return@withLock
                                }

                                // Recheck Room to avoid uploading an old emission.
                                val currentEntity =
                                    resumeRepository.getResumeById(entity.id)
                                        ?: return@withLock

                                val resume =
                                    currentEntity.toResumeData()

                                val lastSyncedVersion =
                                    syncedVersions[entity.id] ?: 0L

                                if (resume.updatedAt > lastSyncedVersion) {

                                    cloudRepository.uploadResume(
                                        userId,
                                        resume
                                    )

                                    syncedVersions[entity.id] =
                                        resume.updatedAt
                                }
                            }

                        } catch (exception: Exception) {
                            exception.printStackTrace()
                        }
                    }
                }
        }

        // Listen for changes made by other devices.
        cloudListener = collection.addSnapshotListener(
            MetadataChanges.EXCLUDE
        ) { snapshot, error ->

            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }

            if (snapshot == null) {
                return@addSnapshotListener
            }

            newScope.launch {

                syncMutex.withLock {

                    for (change in snapshot.documentChanges) {

                        val document = change.document

                        try {

                            when (change.type) {

                                DocumentChange.Type.REMOVED -> {

                                    deletedResumeIds.add(document.id)
                                    syncedVersions.remove(document.id)

                                    resumeRepository.deleteResume(
                                        document.id
                                    )
                                }

                                DocumentChange.Type.ADDED,
                                DocumentChange.Type.MODIFIED -> {

                                    if (
                                        deletedResumeIds.contains(document.id)
                                    ) {
                                        continue
                                    }

                                    val cloudEntity =
                                        document.toResumeEntity()
                                            ?: continue

                                    val cloudResume =
                                        cloudEntity.toResumeData()

                                    val localEntity =
                                        resumeRepository.getResumeById(
                                            cloudEntity.id
                                        )

                                    if (localEntity == null) {

                                        syncedVersions[cloudEntity.id] =
                                            cloudResume.updatedAt

                                        resumeRepository.insertResume(
                                            cloudEntity
                                        )

                                    } else {

                                        val localResume =
                                            localEntity.toResumeData()

                                        when {
                                            cloudResume.updatedAt >
                                                    localResume.updatedAt -> {

                                                syncedVersions[cloudEntity.id] =
                                                    cloudResume.updatedAt

                                                resumeRepository.insertResume(
                                                    cloudEntity
                                                )
                                            }

                                            localResume.updatedAt >
                                                    cloudResume.updatedAt -> {

                                                cloudRepository.uploadResume(
                                                    userId,
                                                    localResume
                                                )

                                                syncedVersions[localEntity.id] =
                                                    localResume.updatedAt
                                            }

                                            else -> {
                                                syncedVersions[cloudEntity.id] =
                                                    cloudResume.updatedAt
                                            }
                                        }
                                    }
                                }
                            }

                        } catch (exception: Exception) {
                            exception.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    // All authenticated app deletions should use this method.
    suspend fun deleteResume(
        userId: String,
        resumeId: String
    ) {

        require(userId.isNotBlank()) {
            "User ID cannot be empty"
        }

        require(resumeId.isNotBlank()) {
            "Resume ID cannot be empty"
        }

        syncMutex.withLock {

            deletedResumeIds.add(resumeId)
            syncedVersions.remove(resumeId)

            var cloudDeleted = false

            try {

                // Delete from Firestore first.
                cloudRepository.deleteResume(
                    userId,
                    resumeId
                )

                cloudDeleted = true

                // Then delete from the local Room database.
                resumeRepository.deleteResume(resumeId)

            } catch (exception: Exception) {

                // Only remove the tombstone if cloud deletion failed.
                // If cloud deletion succeeded, keep protecting this ID.
                if (!cloudDeleted) {
                    deletedResumeIds.remove(resumeId)
                }

                throw exception
            }
        }
    }

    private fun DocumentSnapshot.toResumeEntity():
            SavedResumeEntity? {

        return try {

            val resumeJson = getString("resumeJson")
                ?: return null

            SavedResumeEntity(
                id = getString("id") ?: id,
                title = getString("title") ?: "My Resume",
                lastModified = getLong("lastModified") ?: 0L,
                resumeJson = resumeJson
            )

        } catch (exception: Exception) {

            exception.printStackTrace()
            null
        }
    }

    fun stopSync() {

        localSyncJob?.cancel()
        localSyncJob = null

        cloudListener?.remove()
        cloudListener = null

        sessionJob?.cancel()
        sessionJob = null

        activeUserId = null

        syncedVersions.clear()
        deletedResumeIds.clear()
    }
}
