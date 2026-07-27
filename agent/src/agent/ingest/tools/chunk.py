"""chunk tool — Document의 text를 임베딩 크기에 맞게 조각으로 나눈다.

우리 데이터는 text가 질문(짧음)이라 대개 1개 그대로지만, 긴 요약을 대비해
chunk_size 이하로 자르되 인접 조각을 chunk_overlap만큼 겹치게 한다(경계 문맥 보존).
"""

from __future__ import annotations

from langchain_text_splitters import RecursiveCharacterTextSplitter

from agent.ingest.document import Document

_splitter = RecursiveCharacterTextSplitter(chunk_size=800, chunk_overlap=100)


def chunk(doc: Document) -> list[Document]:
    """Document의 text가 길면 여러 조각 Document로 나눈다. 짧으면 그대로 1개."""
    pieces = _splitter.split_text(doc.text)
    if len(pieces) <= 1:
        return [doc]
    return [
        Document(text=piece, metadata={**doc.metadata, "chunk": i})
        for i, piece in enumerate(pieces)
    ]
