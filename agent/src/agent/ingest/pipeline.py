"""ETL 파이프라인 — AI Hub 민사법 원본을 검색 가능한 Chroma 지식으로.

분기 없는 직선 배치라 LangGraph 없이 순수 함수로 조립한다.
load → normalize → keyword_prefilter → chunk → store.
멱등·resumable은 store의 upsert(sha1 id)가 보장. LangGraph는 분기가 있는
런타임 '거래 법률 도우미' 에이전트에만 쓴다.

대용량(수만 건)을 대비해 레코드를 스트리밍으로 흘리며 batch_size마다
store로 flush한다 → 메모리 일정 + 부분 저장(중간에 끊겨도 재실행 안전).
"""

from __future__ import annotations

from agent.config import settings
from agent.ingest.document import Document
from agent.ingest.tools.chunk import chunk
from agent.ingest.tools.filters import keyword_prefilter
from agent.ingest.tools.loader import load_records
from agent.ingest.tools.normalize import normalize
from agent.ingest.tools.store import store


def run_ingest(root: str | None = None, batch_size: int = 128) -> dict:
    """원본 디렉터리를 적재→정규화→키워드필터→청킹→저장. 단계별 건수(dict) 반환.

    root: 원본 JSON 루트(기본 settings.raw_dir). batch_size: 임베딩·저장 배치 크기.
    """
    root = root or settings.raw_dir
    stats = {"loaded": 0, "normalized": 0, "filtered": 0, "chunked": 0, "stored": 0}
    batch: list[Document] = []

    def flush() -> None:
        if batch:
            stats["stored"] += store(batch)
            batch.clear()

    for record in load_records(root):
        stats["loaded"] += 1
        doc = normalize(record)
        if doc is None:
            continue
        stats["normalized"] += 1
        if not keyword_prefilter([doc]):   # 키워드 없으면 제외(값싼 결정적 필터)
            continue
        stats["filtered"] += 1
        pieces = chunk(doc)
        stats["chunked"] += len(pieces)
        batch.extend(pieces)
        if len(batch) >= batch_size:
            flush()
    flush()
    return stats
