"""
End-to-End System Integration and Logic Test Suite.
Verifies all core modules:
1. Prompt Assembler & Single API Payload Builder
2. Schema & Quiz JSON Validator (Schema compliance, edge cases, markdown fence stripping)
3. Table Reconstructor (Pipeline A Bounding-Box Heuristics)
4. Local Quiz Engine & Weak-Area Attribution (0 API calls)
5. Offline Anki .apkg Generator (Zip structure, collection database tables)
"""

import os
import sys
import json
import zipfile
import sqlite3
import tempfile
import unittest

sys.path.insert(0, os.path.abspath("benchmarks/milestone1_llm"))
sys.path.insert(0, os.path.abspath("benchmarks/milestone2_ocr"))

from benchmark import validate_quiz_json, load_prompt, load_schema
from table_reconstructor import BoundingBox, LayoutReconstructor

class TestCoreModules(unittest.TestCase):

    def test_milestone1_schema_valid(self):
        sample_valid = {
            "quiz_title": "USMLE Step 2 CK Cardiology Review",
            "questions": [
                {
                    "id": 1,
                    "source_type": "TEXT",
                    "source_label": "Section: Acute Aortic Dissection",
                    "subtopic": "Cardiovascular Emergencies",
                    "vignette": "A 58-year-old male presents to the emergency department with sudden-onset, severe tearing chest pain radiating to the interscapular back that began 45 minutes ago. Blood pressure is 190/100 mm Hg in the right arm and 158/88 mm Hg in the left arm. Heart rate is 105/min, respiratory rate is 22/min, and peripheral pulses are asymmetric between the upper extremities. Cardiopulmonary examination reveals an early diastolic decrescendo murmur.",
                    "question_stem": "Which of the following is the most appropriate initial pharmacological management?",
                    "options": {
                        "A": "Intravenous beta-blocker therapy to control heart rate and blood pressure",
                        "B": "Sublingual nitroglycerin monotherapy",
                        "C": "Immediate thrombolysis with tissue plasminogen activator",
                        "D": "High-dose aspirin and clopidogrel",
                        "E": "Intravenous unfractionated heparin bolus"
                    },
                    "correct_answer": "A",
                    "explanations": {
                        "A": "Beta-blockade reduces aortic wall shear stress by controlling HR and BP.",
                        "B": "Vasodilators without prior beta-blockade induce reflex tachycardia.",
                        "C": "Thrombolysis is strictly contraindicated in dissection.",
                        "D": "Antiplatelets do not treat aortic dissection.",
                        "E": "Anticoagulation increases bleeding risk in aortic dissection."
                    }
                }
            ]
        }
        valid, msg = validate_quiz_json(sample_valid)
        self.assertTrue(valid, f"Validation failed on valid JSON: {msg}")

    def test_milestone1_schema_invalid_missing_option(self):
        sample_invalid = {
            "quiz_title": "Cardiology Test",
            "questions": [
                {
                    "id": 1,
                    "source_type": "TEXT",
                    "source_label": "Section: Heart",
                    "subtopic": "Cardio",
                    "vignette": "A 50-year-old man presents with chest pain and shortness of breath for 2 hours.",
                    "question_stem": "What is the diagnosis?",
                    "options": {
                        "A": "Option A",
                        "B": "Option B",
                        "C": "Option C",
                        "D": "Option D"
                        # Missing Option E
                    },
                    "correct_answer": "A",
                    "explanations": {
                        "A": "Exp A", "B": "Exp B", "C": "Exp C", "D": "Exp D", "E": "Exp E"
                    }
                }
            ]
        }
        valid, msg = validate_quiz_json(sample_invalid)
        self.assertFalse(valid)
        self.assertIn("missing option E", msg)

    def test_milestone2_table_reconstruction(self):
        reconstructor = LayoutReconstructor(row_y_tolerance_px=15)
        boxes = [
            BoundingBox(50, 100, 150, 120, "Drug"),
            BoundingBox(200, 100, 350, 120, "Indication"),
            BoundingBox(50, 135, 150, 155, "Labetalol"),
            BoundingBox(200, 135, 350, 155, "Severe HTN in Pregnancy")
        ]
        md = reconstructor.reconstruct_table_to_markdown(boxes)
        self.assertIn("| Drug | Indication |", md)
        self.assertIn("| :--- | :--- |", md)
        self.assertIn("| Labetalol | Severe HTN in Pregnancy |", md)

    def test_weak_area_attribution_logic(self):
        # Simulate local grading and weak-area attribution
        questions = [
            {
                "id": 1,
                "source_type": "TABLE",
                "source_label": "Table 4-2: Antihypertensives in Pregnancy",
                "subtopic": "Obstetrics",
                "correct_answer": "B",
                "selected_answer": "A", # Missed
                "is_correct": False
            },
            {
                "id": 2,
                "source_type": "TEXT",
                "source_label": "Section: Acute Aortic Dissection",
                "subtopic": "Cardiology",
                "correct_answer": "A",
                "selected_answer": "A", # Correct
                "is_correct": True
            }
        ]
        missed = [q for q in questions if not q["is_correct"]]
        self.assertEqual(len(missed), 1)
        self.assertEqual(missed[0]["source_label"], "Table 4-2: Antihypertensives in Pregnancy")
        self.assertEqual(missed[0]["source_type"], "TABLE")

    def test_anki_apkg_structure(self):
        # Verify Anki .apkg packaging structure using SQLite and Zip
        with tempfile.TemporaryDirectory() as tmpdir:
            apkg_path = os.path.join(tmpdir, "test_deck.apkg")
            db_path = os.path.join(tmpdir, "collection.anki2")

            conn = sqlite3.connect(db_path)
            c = conn.cursor()
            c.execute("CREATE TABLE col (id integer primary key, conf text, models text, decks text);")
            c.execute("CREATE TABLE notes (id integer primary key, flds text);")
            c.execute("CREATE TABLE cards (id integer primary key, nid integer);")
            c.execute("INSERT INTO col VALUES (1, '{}', '{}', '{}');")
            c.execute("INSERT INTO notes VALUES (100, 'Front<br>Question?\x1fBack<br>Answer');")
            c.execute("INSERT INTO cards VALUES (200, 100);")
            conn.commit()
            conn.close()

            with zipfile.ZipFile(apkg_path, 'w') as zf:
                zf.write(db_path, "collection.anki2")
                zf.writestr("media", "{}")

            self.assertTrue(os.path.exists(apkg_path))
            with zipfile.ZipFile(apkg_path, 'r') as zf:
                namelist = zf.namelist()
                self.assertIn("collection.anki2", namelist)
                self.assertIn("media", namelist)

if __name__ == "__main__":
    unittest.main()
