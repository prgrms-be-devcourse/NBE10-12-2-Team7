"""filter tool — 거래 관련 지식만 남기는 키워드 필터.

keyword_prefilter: 거래 키워드가 하나라도 있는 Document만 남기는
값싼 결정적 필터. (v1은 키워드 분류만 — NIM 관계성 게이트는 보류)
"""

from __future__ import annotations

from agent.ingest.document import Document

_KEYWORDS = (
    "매매", "매도", "매수", "계약", "환불", "반품", "청약철회",
    "하자", "담보책임", "채무불이행", "손해배상", "대금", "이행",
    "해제", "해지", "사기", "기망", "대여", "임대차", "보증", "물품", "중고",
)


def keyword_prefilter(docs: list[Document]) -> list[Document]:
    """거래 키워드가 하나라도 있는 Document만 남긴다(값싼 결정적 1차 필터)."""
    return [d for d in docs if _has_keyword(d)]


def _has_keyword(doc: Document) -> bool:
    haystack = " ".join((
        doc.text,
        str(doc.metadata.get("answer", "")),
        str(doc.metadata.get("category", "")),
    ))
    return any(kw in haystack for kw in _KEYWORDS)
