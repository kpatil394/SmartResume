# Smart Resume Studio (`Smart.Resume`)

A modern native Android application built with **Kotlin** and **Material Design 3 XML Layouts**.

## How to Open & Run in Android Studio
1. Extract this `.zip` file to any folder on your computer.
2. Open **Android Studio** (Iguana / Jellyfish / Koala / Ladybug or newer).
3. Click **Open** and select the extracted **`SmartResume`** folder.
4. Wait for Android Studio to automatically download Gradle dependencies and finish **Gradle Sync**.
5. Select an Android Emulator or physical Android device (API 24+) and click **Run ('app')**.

## Features Included
- **Package Name:** `Smart.Resume` (Capital `S` and Capital `R`)
- **Modern Login & Sign Up (`AuthActivity`):** Animated toggle between Login and Sign Up, field validation, persistent local session storage, and a **1-Tap Instant Demo Login**.
- **Real-Time ATS Compatibility Engine (`AtsAnalyzerEngine`):** Live 0-100% score calculation across Contact, Impact Metrics (%, $, +), Action Verbs, and Skills.
- **Target Job Description (JD) Keyword Matcher:** Paste any job posting to compare matched and missing technical keywords.
- **Smart Bullet Polisher & AI Executive Summary Generator:** 1-tap enhancement of weak phrases into executive bullet points.
- **Native A4 Vector PDF Generator (`PdfResumeExporter`):** Generates a crisp, multi-theme A4 PDF resume (Executive Indigo, Tech Emerald, Minimalist Slate) with Android's native `PdfDocument` API and shares/saves via `FileProvider`.
