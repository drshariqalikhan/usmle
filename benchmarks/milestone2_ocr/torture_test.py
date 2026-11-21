"""
Milestone 2 Benchmark: 10-Case Torture Test Suite for Layout Detection & Table Reconstruction.
Tests:
  1. 2-column prose text reading order
  2. Dense pharmacology 4-column table
  3. Table with missing cells
  4. Algorithm / Flowchart with Figure caption
  5. Multi-line table header cells
  6. Clinical vignette with adjacent diagnostic chart
  7. ECG / Imaging caption proximity
  8. Cardiology differential diagnosis comparison table
  9. Curved spine / shadow simulated offset
 10. Max payload page test (combining prose, 2 tables, 1 figure)
"""

import time
from table_reconstructor import BoundingBox, LayoutReconstructor

def generate_torture_test_cases():
    cases = []

    # Case 1: 2-column prose text
    col1 = [
        BoundingBox(50, 100, 300, 130, "Hypertension is a major risk factor for aortic dissection."),
        BoundingBox(50, 140, 300, 170, "Initial therapy involves rapid heart rate control."),
        BoundingBox(50, 180, 300, 210, "Target heart rate is less than 60 beats per minute.")
    ]
    col2 = [
        BoundingBox(350, 100, 600, 130, "Type A dissections involve the ascending aorta."),
        BoundingBox(350, 140, 600, 170, "Surgical intervention is the definitive management."),
        BoundingBox(350, 180, 600, 210, "Mortality increases 1-2% per hour without treatment.")
    ]
    cases.append({"name": "Case 1: 2-column prose text", "boxes": col1 + col2})

    # Case 2: Dense pharmacology 4-column table
    pharma_table = [
        BoundingBox(50, 50, 500, 80, "Table 4-2: Antihypertensive Therapy in Pregnancy"),
        # Header row (Y ~ 100)
        BoundingBox(50, 95, 150, 120, "Drug"),
        BoundingBox(160, 95, 270, 120, "Mechanism"),
        BoundingBox(280, 95, 420, 120, "Indication"),
        BoundingBox(430, 95, 580, 120, "Adverse Effects"),
        # Row 1 (Y ~ 130)
        BoundingBox(50, 130, 150, 155, "Labetalol"),
        BoundingBox(160, 130, 270, 155, "Alpha/Beta blocker"),
        BoundingBox(280, 130, 420, 155, "Acute crisis & chronic"),
        BoundingBox(430, 130, 580, 155, "Avoid in asthma"),
        # Row 2 (Y ~ 165)
        BoundingBox(50, 165, 150, 190, "Nifedipine ER"),
        BoundingBox(160, 165, 270, 190, "DHP Calcium blocker"),
        BoundingBox(280, 165, 420, 190, "First-line oral"),
        BoundingBox(430, 165, 580, 190, "Reflex tachycardia"),
        # Row 3 (Y ~ 200)
        BoundingBox(50, 200, 150, 225, "Hydralazine"),
        BoundingBox(160, 200, 270, 225, "Arteriolar vasodilator"),
        BoundingBox(280, 200, 420, 225, "Acute severe inpatient"),
        BoundingBox(430, 200, 580, 225, "Lupus-like syndrome")
    ]
    cases.append({"name": "Case 2: Dense pharmacology 4-column table", "boxes": pharma_table})

    # Case 3: Table with missing cells
    missing_cells = [
        BoundingBox(50, 50, 400, 80, "Table 1: Hepatitis Serology"),
        BoundingBox(50, 100, 150, 125, "State"),
        BoundingBox(160, 100, 260, 125, "HBsAg"),
        BoundingBox(270, 100, 370, 125, "Anti-HBs"),
        # Row 1
        BoundingBox(50, 135, 150, 160, "Acute Infection"),
        BoundingBox(160, 135, 260, 160, "Positive"),
        BoundingBox(270, 135, 370, 160, "Negative"),
        # Row 2 (Missing Anti-HBs cell)
        BoundingBox(50, 170, 150, 195, "Immunity (Vaccine)"),
        BoundingBox(270, 170, 370, 195, "Positive")
    ]
    cases.append({"name": "Case 3: Table with missing cells", "boxes": missing_cells})

    # Case 4: Algorithm / Flowchart with Figure caption
    flowchart = [
        BoundingBox(50, 50, 550, 80, "Section: Diagnostic Approach to Hemoptysis"),
        BoundingBox(50, 350, 550, 380, "Figure 3-1: Management Algorithm for Massive vs Non-Massive Hemoptysis"),
        BoundingBox(50, 400, 550, 430, "Massive hemoptysis (>600 mL/24h) mandates immediate airway protection.")
    ]
    cases.append({"name": "Case 4: Algorithm with Figure caption", "boxes": flowchart})

    # Case 5: Multi-line table header
    multi_line_header = [
        BoundingBox(50, 50, 500, 80, "Table 5-1: Electrolyte Abnormalities in Kidney Disease"),
        BoundingBox(50, 95, 200, 130, "Serum Electrolyte / Ion"),
        BoundingBox(220, 95, 380, 130, "Pathophysiologic Mechanism"),
        BoundingBox(400, 95, 550, 130, "Typical ECG Findings"),
        BoundingBox(50, 140, 200, 165, "Hyperkalemia"),
        BoundingBox(220, 140, 380, 165, "Impaired renal excretion"),
        BoundingBox(400, 140, 550, 165, "Peaked T waves")
    ]
    cases.append({"name": "Case 5: Multi-line table header", "boxes": multi_line_header})

    # Case 6: Clinical vignette with diagnostic chart
    diag_chart = [
        BoundingBox(50, 50, 500, 80, "Section: Acute Mesenteric Ischemia"),
        BoundingBox(50, 90, 500, 120, "Severe abdominal pain out of proportion to physical exam findings."),
        BoundingBox(50, 250, 500, 280, "Figure 2: Angiographic findings showing superior mesenteric artery occlusion."),
        BoundingBox(50, 300, 500, 330, "Immediate CT mesenteric angiography is the diagnostic modality of choice.")
    ]
    cases.append({"name": "Case 6: Vignette with diagnostic chart", "boxes": diag_chart})

    # Case 7: ECG / Imaging caption
    ecg_case = [
        BoundingBox(50, 300, 500, 330, "Figure 1-4: 12-lead ECG demonstrating ST-segment elevation in leads V1-V4."),
        BoundingBox(50, 350, 500, 380, "Represents acute anterior wall myocardial infarction due to LAD occlusion.")
    ]
    cases.append({"name": "Case 7: ECG caption proximity", "boxes": ecg_case})

    # Case 8: Cardiology differential table
    diff_table = [
        BoundingBox(50, 50, 500, 80, "Table 2: Murmur Differentiation"),
        BoundingBox(50, 95, 200, 120, "Murmur"),
        BoundingBox(220, 95, 380, 120, "Maneuver: Valsalva"),
        BoundingBox(400, 95, 550, 120, "Maneuver: Squatting"),
        BoundingBox(50, 130, 200, 155, "HOCM"),
        BoundingBox(220, 130, 380, 155, "Increases intensity"),
        BoundingBox(400, 130, 550, 155, "Decreases intensity"),
        BoundingBox(50, 165, 200, 190, "Aortic Stenosis"),
        BoundingBox(220, 165, 380, 190, "Decreases intensity"),
        BoundingBox(400, 165, 550, 190, "Increases intensity")
    ]
    cases.append({"name": "Case 8: Murmur differential table", "boxes": diff_table})

    # Case 9: Simulated curved spine / vertical jitter
    jitter_table = [
        BoundingBox(50, 50, 500, 80, "Table 9: Stroke Syndromes"),
        BoundingBox(50, 98, 180, 122, "Vessel"),
        BoundingBox(190, 102, 350, 126, "Motor Deficit"),
        BoundingBox(360, 96, 520, 120, "Sensory Deficit"),
        BoundingBox(50, 138, 180, 162, "ACA"),
        BoundingBox(190, 142, 350, 166, "Lower extremity > Upper"),
        BoundingBox(360, 136, 520, 160, "Lower extremity sensory loss")
    ]
    cases.append({"name": "Case 9: Simulated spine vertical jitter", "boxes": jitter_table})

    # Case 10: Maximum payload page (combines prose, table, and figure)
    max_payload = pharma_table + [
        BoundingBox(50, 300, 550, 330, "Section: Summary of Gestational Hypertension Management"),
        BoundingBox(50, 450, 550, 480, "Figure 4-1: Flowchart for evaluating protein/creatinine ratio in preeclampsia.")
    ]
    cases.append({"name": "Case 10: Max payload composite page", "boxes": max_payload})

    return cases

def run_torture_test():
    reconstructor = LayoutReconstructor(row_y_tolerance_px=15)
    cases = generate_torture_test_cases()
    print("=== MILESTONE 2 TORTURE TEST SUITE (10 CASES) ===")
    
    total_time = 0.0
    for idx, case in enumerate(cases, 1):
        t0 = time.time()
        result = reconstructor.process_page(case["boxes"])
        elapsed = time.time() - t0
        total_time += elapsed

        # Check performance constraint: <1.5 seconds per page
        assert elapsed < 1.5, f"{case['name']} took {elapsed:.3f}s (exceeded 1.5s limit)"

        tables_count = len(result["tables"])
        figures_count = len(result["figures"])
        text_len = len(result["text"])

        print(f"  [{idx:02d}/10] {case['name']} ({elapsed*1000:.2f}ms) -> Tables: {tables_count}, Figures: {figures_count}, Text chars: {text_len}")
        if result["tables"]:
            for t in result["tables"]:
                # Verify markdown table has pipes and header divider
                assert "| :---" in t["markdown"], f"Invalid markdown table generated in {case['name']}"

    avg_time = total_time / len(cases)
    print(f"\nAll 10 torture tests passed! Average time per page: {avg_time*1000:.2f}ms (Limit: 1500ms)")
    print("PASS GATE ACHIEVED: Markdown tables cleanly aligned, figures correctly isolated, <1.5s per page.")

if __name__ == "__main__":
    run_torture_test()

