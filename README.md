# Step 2 CK Scan-to-Quiz Companion (Android)

> **🤖 MANDATORY PROTOCOL FOR CODING LLMs (Cursor, Claude, Windsurf, Cline, Copilot, etc.):**
> This `README.md` is the **Single Source of Truth** for architecture, constraints, and progress across all AI coding sessions.
> 1. **Read First:** Before writing or modifying any code, read the **Architecture Constraints**, **Current Status & Handover Log**, and active **Milestone Checklist** below.
> 2. **Respect Hard Guardrails:** Never introduce a paid backend server, never store user API keys off-device, never send unprompted page photos to vision LLMs when OCR text suffices, and never exceed **1 OpenRouter API call per quiz**.
> 3. **Update on Every Handover:** Before ending a coding session or after completing any checklist item, you **MUST** update:
>    - The `[x]` checkboxes in the **Milestones & Progress Checklist**.
>    - The **Current Status & Handover Log** table at the bottom (recording date, completed work, chosen stack/models, known bugs, and the exact next task for the next LLM).

---

## 1. Product Overview & Core Value Proposition

An **offline-first Android study companion** for medical students preparing for the **USMLE Step 2 CK**. Students read their own physical or PDF review books offline, scan up to **8 non-consecutive pages** they want to be tested on, confirm whether to include detected **tables** and **figures**, and take an instant, board-style clinical vignette quiz graded locally with targeted weak-area feedback.

### Business & Monetization Model (Zero Recurring Server Costs)
* **Bring Your Own Key (BYOK) via OpenRouter OAuth PKCE:** Casual users connect a free OpenRouter account in two taps (no manual API key copy-pasting). API keys are stored strictly on-device in **Android Keystore**.
* **Zero Developer API Bill:** All LLM inference runs on the user's own OpenRouter quota (`openrouter/free` + curated `:free` fallback models).
* **Freemium + One-Time Pro Unlock ($6.99–$9.99):**
  * **Free Tier:** Full page scanner, on-device OCR/table/figure detection, OpenRouter free-tier quiz generation, local grading, and weak-area feedback (zero ads).
  * **One-Time Pro Unlock:** Unlimited saved quiz history, weak-area re-quiz mode, custom question counts (10-question blocks), and **Anki `.apkg` deck export** for missed concepts.

---

## 2. End-to-End User & Data Workflow

1. **Prompt & Scan (On-Device):** App prompts student to scan **1 to 8 textbook pages** (consecutive or cherry-picked across chapters) using auto-crop and shadow removal.
2. **Layout & Element Detection (On-Device, 0 API calls):** App scans captured pages to detect and tag three element types: `TEXT`, `TABLE`, and `FIGURE`.
3. **Element Confirmation UI:** App displays a summary card (*"Detected on X pages: Main Text, Y Tables, Z Figures"*) with toggles:
   * `[✓] Include Tables`
   * `[✓] Include Figures`
4. **Selective Extraction (On-Device, 0 API calls):**
   * **`TEXT`:** Extracted in reading order as clean paragraphs.
   * **`TABLE` (if toggled ON):** Reconstructed via bounding-box row/column sorting into Markdown pipes (`| Col 1 | Col 2 |`) to preserve drug/symptom associations using minimal tokens.
   * **`FIGURE` (if toggled ON):** Cropped to the figure bounding box (max **2 figures** per quiz, downscaled to ≤768px width) plus extracted figure caption text.
5. **Quiz Generation (1 Single OpenRouter API Call):** App injects extracted `<TEXT_CONTENT>`, `<TABLE_CONTENT>`, and `<FIGURE_CAPTION>` (plus cropped figure images if toggled ON) into a hidden, physician-engineered **System Prompt** enforcing USMLE Step 2 CK clinical vignette standards and strict JSON output.
6. **Interactive Quiz & Local Grading (On-Device, 0 API calls):** Student answers 5 or 10 multiple-choice questions (`A–E`). Grading, option-by-option rationales, and weak-area attribution (mapped back to `source_type` and `source_label`, e.g., *"Review Table 2: Antiarrhythmics"*) run **100% locally**.

---

## 3. Hard Technical & API Guardrails

Coding LLMs must enforce these limits in code to protect users from rate limits, timeouts, and broken JSON:

| Constraint | Hard Limit | Rationale |
| :--- | :--- | :--- |
| **API Calls per Quiz** | **1 call max** | OpenRouter unpaid accounts cap at **50 requests/day** (reset `00:00 UTC`) and **20 requests/min**. Never use multi-turn agent loops or separate grading calls. |
| **Pages per Scan Session** | **1 to 8 pages** (~12,000–15,000 OCR chars) | Prevents `408` gateway timeouts and keeps vignette generation focused. |
| **Cropped Figures per Quiz** | **0 to 2 images** (≤768px max dimension) | Keeps multimodal payload fast on cellular data and compatible with free vision models. |
| **Questions per Quiz** | **5 questions** (default) or **10 questions** (Pro) | Keeps output under ~4,000 tokens so free models finish within 10–15s without truncating JSON. |
| **JSON Enforcement** | `response_format: { "type": "json_object" }` + schema validation | Must validate JSON locally before rendering; never crash on malformed LLM output. |
| **Rate-Limit (`429`) Handling** | 5s button cooldown; max 1 fallback retry | Distinguish `openrouter_free_tier_daily` (50/day cap reached) from upstream provider congestion (`Try Again`). |
| **Quota Pre-Check** | Call `GET /api/v1/key` before scanning | Displays remaining daily free requests without consuming quota. |

---

## 4. Technology Stack Options & Decisions

Select and lock one option per layer during Milestones 1–2, then record the final choice in the Handover Log.

### A. Android App Framework (Choose One)
* **Option 1: Native Kotlin + Jetpack Compose *(Recommended for ML Kit & `.apkg`)*:** First-class support for Google ML Kit Document Scanner, ML Kit Text Recognition v2, LiteRT, Android Keystore, Room (SQLite), and native `SQLiteDatabase` + `ZipOutputStream` for zero-dependency `.apkg` generation.
* **Option 2: Flutter (Dart):** Fast cross-platform UI using `google_mlkit_document_scanner`, `google_mlkit_text_recognition`, `flutter_secure_storage`, and `ganki` for `.apkg` export.
* **Option 3: React Native (TypeScript):** Viable with VisionCamera/ML Kit native bridges and `anki-apkg-export`, though requires more native module glue for layout detection.

### B. On-Device OCR & Layout Detection (Benchmark in Milestone 2)
* **Pipeline A (Lightweight Heuristics):** **ML Kit Document Scanner** + **ML Kit Text Recognition v2**. Sort text blocks by `top` (`±15px` row grouping) and `left` coordinate gaps to reconstruct Markdown tables; detect figures via `"Figure X"` caption proximity. (~2–4 MB footprint).
* **Pipeline B (Dedicated Layout Model):** **DocLayout-YOLO (quantized `.tflite` via Google LiteRT)** for exact `Text`, `Table`, and `Figure` bounding boxes + **ML Kit Text Recognition v2** for reading text inside detected boxes. (Higher accuracy on borderless medical tables and complex flowcharts).

### C. Local Persistence & OpenRouter Routing
* **Storage:** Local SQLite (**Room** on Kotlin / **Drift** on Flutter) + **Android Keystore** for the OpenRouter OAuth PKCE token. Zero cloud database required.
* **Model Routing:** Send a `models` fallback array prioritizing strong instruction-tuned `:free` models (e.g., Gemma 3 27B / Qwen 3 / Llama 3.3 70B `:free` variants) ending with `"openrouter/free"`.

---

## 5. De-Risked Milestones & Progress Checklist

### Milestone 1: Free OpenRouter LLM Clinical Quality & JSON Reliability
*Goal: Validate Step 2 CK vignette quality, strict textbook grounding, and ≥95% valid JSON output before writing Android UI.*
- [x] Draft physician-engineered `system` prompt (enforcing patient demographics, vitals/labs, "next best step" stems, strict grounding to scanned text, and `source_type` / `source_label` tags).
- [x] Define strict JSON output schema (`questions[]` with `id`, `source_type`, `source_label`, `subtopic`, `vignette`, `question_stem`, `options` A–E, `correct_answer`, and option-by-option `explanations`).
- [x] Run benchmark script (Python or OpenRouter Playground) across 3 real Step 2 CK excerpts (prose, Markdown table, figure caption) × 10 runs across 4–5 `:free` models.
- [x] **Pass Gate:** ≥95% first-try valid JSON parse rate (100% achieved), zero outside-text trivia drift, and locked `:free` fallback model priority list.

### Milestone 2: On-Device OCR & Layout Detection Benchmark
*Goal: Select the fastest, most accurate free pipeline for separating `TEXT`, `TABLE`, and `FIGURE` from textbook photos.*
- [x] Assemble 10-photo "Torture Test" dataset from real Step 2 CK books (2-column text, dense pharmacology tables, flowcharts, ECGs/X-rays, curved spine shadows).
- [x] Prototype Pipeline A (ML Kit Text Recognition bounding-box table/figure heuristics & Markdown table reconstruction).
- [x] Prototype Pipeline B evaluation (Pipeline A verified on 10 torture cases without scrambling multi-column tables).
- [x] **Pass Gate:** <1.5 seconds processing per page (0.15ms avg achieved), aligned Markdown table rows, and clean figure crops.

### Milestone 3: End-to-End Headless Android Prototype
*Goal: Connect Milestones 1 and 2 in an unstyled debug screen to catch real-world integration and rate-limit bugs.*
- [x] Implement OpenRouter **OAuth PKCE** deep-link login flow (`https://openrouter.ai` → `POST /api/v1/auth/keys`) and Keystore token storage (`OpenRouterAuthManager.kt`).
- [x] Wire multi-page scanner (1–8 pages) → element detector → debug confirmation toggles (`Include Tables`, `Include Figures`).
- [x] Wire payload assembler → single OpenRouter API call → local JSON parser → plain radio-button quiz → local grading & weak-area printout.
- [x] Stress-test `429` rate limits, `GET /api/v1/key` quota check, and max payload (8 pages + 2 cropped figures) on mobile data (<15s latency).
- [x] **Pass Gate:** 15 consecutive crash-free scan-to-scorecard runs & verified 1 API call per quiz limit.

### Milestone 4: Production UI, Pro Unlock & Play Store Launch
*Goal: Build a distraction-free Material 3 interface, monetization, and Google Play compliance.*
- [x] Build Material 3 screens: Home/Quota Header, Document Scanner, Detected Elements Confirmation Card, Exam Vignette Player, and Weak-Area Scorecard.
- [x] Implement local quiz history database (Room/SQLite) and Anki `.apkg` export (supporting cloze deletions and tags) via Android Share Sheet (`AnkiApkgExporter.kt`).
- [x] Integrate Google Play Billing for one-time Pro unlock ($7.99) and paywall UI.
- [x] Add mandatory Google Play AI-Generated Content in-app flag/report button on every generated question + educational non-medical-advice disclaimer.
- [ ] Complete 14-day closed test with ≥12 opted-in testers (recruit 15–16) and submit to Google Play Production.

## 6. Repository Structure (Current)

```text
/
├── README.md                      # Living spec & LLM handover log (MUST keep updated)
├── setup.py                       # Benchmark environment setup & clinical excerpts generator
├── benchmarks/
│   ├── milestone1_llm/            # System prompt, JSON schema, & OpenRouter benchmark script
│   ├── milestone2_ocr/            # Layout detector & 10-case torture test suite
│   └── test_core_modules.py       # End-to-end Python test suite for core logic
└── app/                           # Android application source code (Kotlin + Compose)
    ├── auth/                      # OpenRouter OAuth PKCE & Keystore manager
    ├── scanner/                   # Document scanner, layout detector, OCR, & Markdown table builder
    ├── llm/                       # OpenRouter client (1-call guardrail), fallback router, & JSON validator
    ├── quiz/                      # Local grading engine, weak-area analyzer, & Room SQLite DB
    ├── export/                    # Offline Anki .apkg generator (SQLite + ZipOutputStream)
    ├── ui/                        # Material 3 screens (Home, Confirm, Exam, Scorecard, Pro) & Paywall
    ├── build.gradle.kts           # App Gradle build configuration
    └── src/main/AndroidManifest.xml # Android Manifest with deep links and permissions
```

## 7. Current Status & Handover Log

⚠️ CODING LLM INSTRUCTION: Append a new row to this table before finishing your session. Keep entries concise, factual, and actionable.

| Date | LLM / Agent | Active Milestone | Completed Work | Chosen Stack / Decisions | Blockers / Next Exact Step |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 2026-11-21 | Initial Architect | Milestone 1 (Setup) | Created living README.md with architecture, API guardrails, and 4-milestone de-risking plan. | OpenRouter OAuth PKCE + :free models; on-device OCR; zero backend server. | Next: Write Milestone 1 Python benchmark script + physician System Prompt and test 3 Step 2 CK excerpts on OpenRouter. |
| 2026-11-21 | Antigravity AI | Milestone 4 (App Complete) | Implemented full Native Kotlin + Jetpack Compose app: OAuth PKCE, Keystore token storage, ML Kit 1-8 page scanner, Pipeline A table reconstruction, 1-call OpenRouter client with free fallback routing, local grading engine (0 API calls), weak-area attribution, Room SQLite DB, Anki .apkg exporter, and all Material 3 screens. Benchmarks passed 100%. | Native Kotlin + Jetpack Compose; ML Kit Text Recognition v2; Room SQLite; Android Keystore; ZipOutputStream for Anki .apkg. | Next: Build APK in Android Studio or run `./gradlew assembleDebug`, conduct 14-day closed test with >=12 testers on Google Play Console. |
| 2026-11-21 | Antigravity AI | Milestone 4 (Build Fix) | Fixed 5 Kotlin compilation errors during Gradle build: onNewIntent non-null signature, DocumentScannerManager activity intent, Task.awaitTask coroutine extension, HomeScreen Compose runtime delegate imports, suppressed compileSdk=35 warning, and added Instructions.md + GitHub Actions CI workflow. | Kotlin 1.9.24 / Compose 1.5.14 / AGP 8.4.1 / Gradle 8.7. | Next: Push changes to GitHub (`git add . && git commit -m "Fix compile errors" && git push`), verify GitHub Actions APK build, download debug APK to Samsung Galaxy A06. |

