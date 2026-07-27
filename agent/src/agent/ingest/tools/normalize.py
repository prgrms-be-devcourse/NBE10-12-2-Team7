"""normalize tool — AI Hub 민사법 라벨링 레코드를 공통 Document로 변환한다.

실제 스키마(2026-07-14 확인):
  info     : { doc_id | statute_name, casenames | statute_abbrv, normalized_court,
               announce_date | effective_date, casetype | statute_type, ... }
  taskinfo : { instruction(일반 지침·제외), input(질문), output(답변), sentences(원문 근거) }

전략(설계 확정): 질의응답은 **질문(input)을 text로 임베딩**(질문↔질문 검색),
답변(output)·원문 근거(sentences)는 metadata에 담아 검색 후 근거로 쓴다.
요약(input 없음)은 요약문(output)을 text로 임베딩한다.
매핑 불가(텍스트 없음) 레코드는 None → 파이프라인에서 건너뜀.
"""

from __future__ import annotations

from pathlib import Path

from agent.ingest.document import Document

_CATEGORIES = ("판결문", "법령", "심결례", "유권해석")


def normalize(record: dict) -> Document | None:
    """원본 레코드 → Document. 텍스트가 없으면 None."""
    task = record.get("taskinfo") or {}
    info = record.get("info") or {}

    question = _text(task.get("input"))
    answer = _text(task.get("output"))
    # 질의응답: 질문을 임베딩 대상으로 / 요약: 요약문(output)을 대상으로
    text = question or answer
    if not text:
        return None

    folder = _text(record.get("_folder"))
    category = next((c for c in _CATEGORIES if c in folder), "")

    metadata = {
        "law_area": "민사",
        "category": category,                       # 판결문 | 법령 | 심결례 | 유권해석
        "task": "qa" if question else "summary",    # qa | summary
        "answer": answer,                           # 정제된 답변 (생성 근거)
        "source_excerpt": _sentences(task.get("sentences")),  # 원문 근거(판결문/법령 발췌)
        "doc_id": _text(info.get("doc_id")) or _text(info.get("statute_name")),
        "title": _text(info.get("casenames")) or _text(info.get("statute_abbrv")),
        "court": _text(info.get("normalized_court")),
        "date": _text(info.get("announce_date")) or _text(info.get("effective_date")),
        "source_file": Path(_text(record.get("_path"))).name,
    }
    return Document(text=text, metadata=metadata)


def _text(value) -> str:
    return value.strip() if isinstance(value, str) else ""


def _sentences(value, limit: int = 2000) -> str:
    """원문 근거 문장(배열/문자열)을 합쳐 근거로 쓸 수 있게 truncate."""
    if isinstance(value, list):
        joined = " ".join(str(v).strip() for v in value if str(v).strip())
    else:
        joined = _text(value)
    return joined[:limit]
