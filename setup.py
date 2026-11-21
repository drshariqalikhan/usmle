"""
Setup and initialization script for Step 2 CK Scan-to-Quiz Companion benchmarks.
Generates prompt templates, JSON schema, and test clinical excerpts.
"""

import os
import json

# Ensure directories exist
os.makedirs("benchmarks/milestone1_llm", exist_ok=True)
os.makedirs("benchmarks/milestone2_ocr", exist_ok=True)
os.makedirs("app", exist_ok=True)

# 1. Physician-Engineered System Prompt
SYSTEM_PROMPT = """You are an expert academic physician educator and senior question writer for the USMLE Step 2 CK (Clinical Knowledge) examination.
Your task is to generate high-yield, board-style clinical vignette multiple-choice questions based EXCLUSIVELY on the provided textbook excerpt.

CRITICAL RULES:
1. STRICT GROUNDING:
   - Every question, correct answer, and explanation must be directly grounded in the provided <TEXT_CONTENT>, <TABLE_CONTENT>, or <FIGURE_CAPTION>.
   - NEVER introduce outside medical trivia, esoteric variants, or unlisted drug dosages that are not explicitly stated or directly inferred from the provided excerpt.
   - If a table or figure is provided, at least one question MUST test high-yield relationships from that table or figure.

2. VIGNETTE STRUCTURE & QUESTION FORMAT:
   - Classic USMLE Step 2 CK clinical vignette style:
     * Patient age, sex, clinical setting (e.g., Emergency Department, Outpatient Clinic, Labor & Delivery).
     * Chief complaint, duration of symptoms, relevant past medical history.
     * Pertinent physical examination findings, complete vital signs (BP, HR, RR, Temp, SpO2).
     * Relevant diagnostic labs or imaging findings (if applicable).
   - Question Stem: Clear, actionable, focused "next best step" or diagnostic question (e.g., "Which of the following is the most appropriate next step in management?", "Which of the following is the most likely diagnosis?").
   - Exactly 5 options: "A", "B", "C", "D", "E".
   - Explanations: Comprehensive rationale for why the correct option is right, and specifically why EACH distractor (incorrect option) is incorrect based on the provided text.

3. SOURCE ATTRIBUTION:
   - "source_type": Must be one of ["TEXT", "TABLE", "FIGURE"].
   - "source_label": Specific heading, table name, or figure title tested (e.g., "Table 4-2: Antihypertensive Therapy in Pregnancy", "Section: Acute Aortic Dissection").

4. OUTPUT FORMAT:
   - Return a single, valid raw JSON object matching the requested schema.
   - Do NOT wrap in markdown code fences (no ```json or ```). Return pure JSON only.
"""

with open("benchmarks/milestone1_llm/system_prompt.txt", "w", encoding="utf-8") as f:
    f.write(SYSTEM_PROMPT)

# 2. Strict JSON Schema for Quiz Output
QUIZ_SCHEMA = {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "UsmleStep2Quiz",
    "type": "object",
    "required": ["quiz_title", "questions"],
    "properties": {
        "quiz_title": {
            "type": "string",
            "description": "Title summarizing the clinical domain tested."
        },
        "questions": {
            "type": "array",
            "minItems": 1,
            "maxItems": 10,
            "items": {
                "type": "object",
                "required": [
                    "id",
                    "source_type",
                    "source_label",
                    "subtopic",
                    "vignette",
                    "question_stem",
                    "options",
                    "correct_answer",
                    "explanations"
                ],
                "properties": {
                    "id": {"type": "integer"},
                    "source_type": {
                        "type": "string",
                        "enum": ["TEXT", "TABLE", "FIGURE"]
                    },
                    "source_label": {"type": "string"},
                    "subtopic": {"type": "string"},
                    "vignette": {"type": "string"},
                    "question_stem": {"type": "string"},
                    "options": {
                        "type": "object",
                        "required": ["A", "B", "C", "D", "E"],
                        "properties": {
                            "A": {"type": "string"},
                            "B": {"type": "string"},
                            "C": {"type": "string"},
                            "D": {"type": "string"},
                            "E": {"type": "string"}
                        },
                        "additionalProperties": False
                    },
                    "correct_answer": {
                        "type": "string",
                        "enum": ["A", "B", "C", "D", "E"]
                    },
                    "explanations": {
                        "type": "object",
                        "required": ["A", "B", "C", "D", "E"],
                        "properties": {
                            "A": {"type": "string"},
                            "B": {"type": "string"},
                            "C": {"type": "string"},
                            "D": {"type": "string"},
                            "E": {"type": "string"}
                        },
                        "additionalProperties": False
                    }
                },
                "additionalProperties": False
            }
        }
    },
    "additionalProperties": False
}

with open("benchmarks/milestone1_llm/schema.json", "w", encoding="utf-8") as f:
    json.dump(QUIZ_SCHEMA, f, indent=2)

# 3. 3 Real Step 2 CK Clinical Excerpts
EXCERPTS = [
    {
        "id": "excerpt_1_prose",
        "name": "Cardiology: Acute Aortic Dissection",
        "content": """<TEXT_CONTENT>
Section: Acute Aortic Dissection
Pathophysiology: Tear in aortic intima leading to dissection of blood through media creating a false lumen.
Risk Factors: Systemic hypertension (most common), Marfan syndrome, Turner syndrome, bicuspid aortic valve, cocaine use.
Clinical Presentation: Sudden-onset severe sharp or tearing retrosternal or interscapular chest/back pain. Unequal blood pressures between arms (>20 mm Hg difference) or pulse deficits. May present with acute aortic regurgitation murmur (early diastolic murmur) or cardiac tamponade (hypotension, distended neck veins, muffled heart sounds).
Diagnosis:
- Hemodynamically stable: CT angiography of the chest (preferred; rapid and sensitive).
- Hemodynamically unstable or severe renal insufficiency: Transesophageal echocardiography (TEE).
Treatment:
- Initial medical stabilization: IV beta-blockers (e.g., esmolol or labetalol) to achieve heart rate <60 bpm and systolic blood pressure between 100-120 mm Hg before vasodilators to prevent reflex tachycardia and aortic shear stress.
- Stanford Type A (involves ascending aorta): Emergency open surgical repair.
- Stanford Type B (descending aorta distal to left subclavian artery): Medical management with IV beta-blockers and blood pressure control. Surgery only if persistent pain, end-organ ischemia, or dissection expansion/rupture.
</TEXT_CONTENT>"""
    },
    {
        "id": "excerpt_2_table",
        "name": "Pharmacology: Antihypertensives in Pregnancy",
        "content": """<TEXT_CONTENT>
Section: Hypertensive Disorders of Pregnancy
Hypertension in pregnancy is defined as SBP >=140 mm Hg or DBP >=90 mm Hg. Severe hypertension is defined as SBP >=160 mm Hg or DBP >=110 mm Hg and requires urgent antihypertensive treatment to prevent maternal stroke.
</TEXT_CONTENT>
<TABLE_CONTENT>
Table 4-2: Antihypertensive Therapy in Pregnancy
| Drug Class / Agent | Mechanism of Action | Clinical Indications / Notes | Contraindications / Adverse Effects |
| :--- | :--- | :--- | :--- |
| Labetalol | Combined alpha-1 and non-selective beta-blocker | Preferred first-line agent for acute severe hypertension (IV) and chronic maintenance (oral) | Avoid in asthma, symptomatic bradycardia, and uncompensated heart failure |
| Nifedipine ER | Dihydropyridine calcium channel blocker | Preferred first-line oral agent for acute severe hypertension and chronic hypertension | Can cause reflex tachycardia and peripheral edema; avoid sublingual immediate-release nifedipine |
| Hydralazine | Direct arteriolar vasodilator | Acute severe hypertension (IV inpatient administration) | Maternal hypotension, reflex tachycardia, headaches, drug-induced lupus with chronic use |
| Methyldopa | Central alpha-2 adrenergic agonist | Chronic gestational hypertension; long-term pediatric safety record | Sedation, depression, positive direct Coombs test, elevated transaminases; slow onset, not for acute crisis |
| ACE inhibitors / ARBs | Renin-angiotensin-aldosterone system blockers | STRICTLY CONTRAINDICATED IN ALL TRIMESTERS | Fetal renal dysgenesis, oligohydramnios, pulmonary hypoplasia, neonatal anuria, skull hypoplasia |
</TABLE_CONTENT>"""
    },
    {
        "id": "excerpt_3_figure",
        "name": "Pulmonology: Solitary Pulmonary Nodule Algorithm",
        "content": """<TEXT_CONTENT>
Section: Solitary Pulmonary Nodule (SPN) Evaluation
A solitary pulmonary nodule is defined as a discrete, round opacity <=3 cm in diameter surrounded entirely by normal lung parenchyma without associated atelectasis, lymphadenopathy, or pleural effusion. Larger lesions (>3 cm) are defined as pulmonary masses and carry high malignancy probability.
</TEXT_CONTENT>
<FIGURE_CAPTION>
Figure 7-1: Diagnostic Decision Algorithm for Solitary Pulmonary Nodule
Step 1: Check prior chest radiographs or CT scans.
- If nodule is unchanged for >=24 months: Demonstrates stability / benign nature -> No further workup needed.
- If nodule is new, growing, or no prior imaging available: Proceed to high-resolution chest CT.

Step 2: Malignancy Risk Stratification based on CT features and clinical risk:
- Low Malignancy Risk (<5% probability: patient age <40, non-smoker, nodule size <8 mm, smooth borders):
  * Action: Serial non-contrast low-dose CT scans at 3, 6, 12, and 24 months.
- Intermediate Malignancy Risk (5% to 65% probability: age 40-60, former/light smoker, nodule size 8-20 mm, scalloped margins):
  * Action: Functional imaging with 18F-FDG PET/CT scan, or tissue biopsy (transthoracic needle aspiration for peripheral nodules; bronchoscopy for central nodules).
- High Malignancy Risk (>65% probability: age >60, heavy active smoker, nodule size >20 mm, corona radiata or spiculated margins, upper lobe location):
  * Action: Surgical excision with video-assisted thoracoscopic surgery (VATS) or exploratory thoracotomy without delay for biopsy.
</FIGURE_CAPTION>"""
    }
]

with open("benchmarks/milestone1_llm/excerpts.json", "w", encoding="utf-8") as f:
    json.dump(EXCERPTS, f, indent=2)

print("Setup completed successfully: system_prompt.txt, schema.json, excerpts.json written.")
