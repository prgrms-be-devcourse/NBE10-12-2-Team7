"""load tool — data/civil_law의 원본 JSON을 레코드로 순회한다.

AI Hub 민사법 라벨링 데이터: 파일 1개 = 레코드 1개(dict), info/taskinfo 구조.
각 레코드에 원본 경로·폴더명을 붙여 normalize가 카테고리를 판별하게 한다.
깨진 파일은 건너뛴다(대량이라 하나 실패로 전체가 멈추지 않게).
"""

from __future__ import annotations

import json
from collections.abc import Iterator
from pathlib import Path


def load_records(root: str | Path) -> Iterator[dict]:
    """root 아래 모든 *.json을 읽어 dict 레코드를 순서대로 yield(경로·폴더 태깅)."""
    base = Path(root)
    if not base.exists():
        return
    for path in sorted(base.rglob("*.json")):
        try:
            record = json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError, UnicodeDecodeError):
            continue
        if isinstance(record, dict):
            record["_path"] = str(path)
            record["_folder"] = path.parent.name
            yield record
