"""
On-Device Layout & Element Detector + Markdown Table Reconstructor (Pipeline A).
Processes 2D text bounding boxes (mimicking Google ML Kit Text Recognition v2 output)
and separates content into:
  - TEXT (in natural reading order)
  - TABLE (reconstructed into clean Markdown pipe tables)
  - FIGURE (detected by 'Figure X' caption proximity and bounding box tagging)
"""

import re
from typing import List, Dict, Any, Tuple

class BoundingBox:
    def __init__(self, left: int, top: int, right: int, bottom: int, text: str):
        self.left = left
        self.top = top
        self.right = right
        self.bottom = bottom
        self.text = text.strip()

    @property
    def height(self) -> int:
        return self.bottom - self.top

    @property
    def width(self) -> int:
        return self.right - self.left

    @property
    def cy(self) -> float:
        return (self.top + self.bottom) / 2.0


class LayoutReconstructor:
    def __init__(self, row_y_tolerance_px: int = 15):
        self.row_y_tolerance_px = row_y_tolerance_px

    def is_figure_caption(self, text: str) -> bool:
        """Detects whether a text block represents a figure label/caption."""
        pattern = r"^(Fig(\.|ure)?\s*\d+[\.\:\-]|Algorithm\s*\d+|Chart\s*\d+)"
        return bool(re.search(pattern, text.strip(), re.IGNORECASE))

    def is_table_header_or_title(self, text: str) -> bool:
        """Detects whether a text block represents a table title or header."""
        pattern = r"^(Table\s*\d+[\.\:\-]|Summary\s+Table)"
        return bool(re.search(pattern, text.strip(), re.IGNORECASE))

    def reconstruct_table_to_markdown(self, table_boxes: List[BoundingBox]) -> str:
        """
        Groups bounding boxes by vertical proximity (row_y_tolerance_px),
        sorts each row by left X-coordinate, and formats as a Markdown pipe table.
        """
        if not table_boxes:
            return ""

        # Sort all boxes primarily by top Y
        sorted_by_y = sorted(table_boxes, key=lambda b: b.top)
        
        rows: List[List[BoundingBox]] = []
        for box in sorted_by_y:
            placed = False
            for row in rows:
                # Compare to median or first box in row
                avg_y = sum(b.cy for b in row) / len(row)
                if abs(box.cy - avg_y) <= self.row_y_tolerance_px:
                    row.append(box)
                    placed = True
                    break
            if not placed:
                rows.append([box])

        # Sort elements within each row by X (left)
        for row in rows:
            row.sort(key=lambda b: b.left)

        if not rows:
            return ""

        # Determine max columns
        max_cols = max(len(r) for r in rows)
        if max_cols == 0:
            return ""

        markdown_lines = []
        
        # Header row
        header_boxes = rows[0]
        header_cells = [b.text.replace("\n", " ").replace("|", "\\|") for b in header_boxes]
        while len(header_cells) < max_cols:
            header_cells.append("")
        markdown_lines.append("| " + " | ".join(header_cells) + " |")
        
        # Separator row
        markdown_lines.append("| " + " | ".join([":---"] * max_cols) + " |")

        # Body rows
        for row in rows[1:]:
            cells = [b.text.replace("\n", " ").replace("|", "\\|") for b in row]
            while len(cells) < max_cols:
                cells.append("")
            markdown_lines.append("| " + " | ".join(cells) + " |")

        return "\n".join(markdown_lines)

    def process_page(self, boxes: List[BoundingBox]) -> Dict[str, Any]:
        """
        Processes a full page of bounding boxes and categorizes into TEXT, TABLES, and FIGURES.
        """
        figures = []
        table_blocks = []
        regular_text = []

        i = 0
        n = len(boxes)
        while i < n:
            b = boxes[i]
            # Check figure caption
            if self.is_figure_caption(b.text):
                # Detected figure caption: associate with bounding box area above or below
                fig_bbox = {
                    "caption": b.text,
                    "crop_area": {
                        "left": max(0, b.left - 20),
                        "top": max(0, b.top - 200),
                        "right": b.right + 20,
                        "bottom": b.top
                    }
                }
                figures.append(fig_bbox)
                i += 1
                continue

            # Check table header
            if self.is_table_header_or_title(b.text):
                title = b.text
                table_cells = []
                j = i + 1
                # Collect table cells until next large vertical break or section header
                while j < n and not self.is_table_header_or_title(boxes[j].text) and not self.is_figure_caption(boxes[j].text):
                    if boxes[j].text.startswith("Section:") or boxes[j].text.startswith("Chapter:"):
                        break
                    table_cells.append(boxes[j])
                    j += 1
                
                md = self.reconstruct_table_to_markdown(table_cells)
                table_blocks.append({
                    "title": title,
                    "markdown": md,
                    "cell_count": len(table_cells)
                })
                i = j
                continue

            regular_text.append(b.text)
            i += 1

        return {
            "text": "\n\n".join(regular_text),
            "tables": table_blocks,
            "figures": figures
        }

