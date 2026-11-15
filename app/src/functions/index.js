
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

const MODEL = "gpt-6-astra";
const MAX_INPUT_LENGTH = 18000;

const TASKS = {
  summary: {
    instruction:
      "Write a professional resume summary using only the supplied information. Do not invent qualifications, achievements, employers, dates, or skills."
  },
  improve: {
    instruction:
      "Improve the supplied resume text for clarity, grammar, professionalism, and impact. Preserve its meaning and never invent facts."
  },
  coverLetter: {
    instruction:
      "Write a professional, personalized cover letter using the supplied resume information and job description. Do not invent experience or qualifications."
  },
  tailor: {
    instruction:
      "Suggest how the supplied resume can be tailored to the job description. Identify relevant existing skills, missing keywords, and honest improvements. Never fabricate experience."
  },
  ats: {
    instruction:
      "Analyze the supplied resume against the job description. Return: relevant keywords found, relevant keywords missing, readability suggestions, section completeness, and honest improvement recommendations. Do not claim to reproduce a proprietary ATS score."
  }
};

function getOutputText(response) {
  const parts = [];

  for (const item of response.output || []) {
    if (item.type !== "message") continue;

    for (const content of item.content || []) {
      if (content.type === "output_text" && content.text) {
        parts.push(content.text);
      }
    }
  }

  return parts.join("\n").trim();
}

exports.smartResumeAI = onCall(
  {
    region: "asia-south1",
    secrets: [OPENAI_API_KEY],
    timeoutSeconds: 120,
    memory: "256MiB"
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Please sign in before using SmartResume AI."
      );
    }

    const { task, resumeText, jobDescription } = request.data || {};

    if (!Object.prototype.hasOwnProperty.call(TASKS, task)) {
      throw new HttpsError(
        "invalid-argument",
        "Unsupported AI operation."
      );
    }

    if (typeof resumeText !== "string" || !resumeText.trim()) {
      throw new HttpsError(
        "invalid-argument",
        "Resume information is required."
      );
    }

    if (resumeText.length > MAX_INPUT_LENGTH) {
      throw new HttpsError(
        "invalid-argument",
        "Resume information is too long."
      );
    }

    if (
      jobDescription !== undefined &&
      (typeof jobDescription !== "string" ||
        jobDescription.length > MAX_INPUT_LENGTH)
    ) {
      throw new HttpsError(
        "invalid-argument",
        "Invalid job description."
      );
    }

    const apiKey = OPENAI_API_KEY.value();

    if (!apiKey) {
      throw new HttpsError(
        "failed-precondition",
        "The OpenAI API key has not been configured."
      );
    }

    const instructions = TASKS[task].instruction;

    const input = [
      "Resume information:",
      resumeText,
      "",
      "Job description:",
      jobDescription || "Not supplied."
    ].join("\n");

    let response;

    try {
      response = await fetch("https://api.openai.com/v1/responses", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`
        },
        body: JSON.stringify({
          model: MODEL,
          instructions,
          input,
          max_output_tokens: 1800,
          store: false
        })
      });
    } catch (error) {
      console.error("OpenAI network error:", error.message);

      throw new HttpsError(
        "unavailable",
        "The AI service is temporarily unavailable."
      );
    }

    const result = await response.json();

    if (!response.ok) {
      console.error("OpenAI API error:", result.error?.message);

      throw new HttpsError(
        "internal",
        "AI generation failed. Please try again."
      );
    }

    const text = getOutputText(result);

    if (!text) {
      throw new HttpsError(
        "internal",
        "The AI returned an empty response."
      );
    }

    return {
      success: true,
      task,
      result: text
    };
  }
);
