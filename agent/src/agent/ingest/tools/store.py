"""store tool — Document을 bge-m3로 임베딩해 Chroma에 upsert(멱등)하고 검색한다.

같은 id면 덮어써 재실행해도 중복이 없다(멱등). 검색은 질문을 벡터로 만들어
코사인 거리로 가장 가까운 top-k를 돌려준다.
"""

from __future__ import annotations

import hashlib

import chromadb

from agent.config import settings
from agent.ingest.document import Document
from agent.ingest.tools.embedder import embed_query, embed_texts

_client = chromadb.PersistentClient(path=settings.chroma_path)


def _collection():
    # hnsw:space=cosine → 텍스트 임베딩 표준(거리 작을수록 유사)
    return _client.get_or_create_collection(
        settings.chroma_collection, metadata={"hnsw:space": "cosine"}
    )


def store(docs: list[Document]) -> int:
    """Document들을 임베딩해 Chroma에 upsert(멱등). 저장 개수 반환."""
    if not docs:
        return 0
    ids = [_doc_id(d) for d in docs]
    embeddings = embed_texts([d.text for d in docs])
    metadatas = [_clean_meta(d.metadata) for d in docs]
    documents = [d.text for d in docs]
    _collection().upsert(ids=ids, embeddings=embeddings, documents=documents, metadatas=metadatas)
    return len(docs)


def search(query: str, k: int = 5) -> list[dict]:
    """질문으로 유사도 top-k 검색. 각 결과 = {text, metadata, distance}."""
    res = _collection().query(query_embeddings=[embed_query(query)], n_results=k)
    return [
        {"text": t, "metadata": m, "distance": d}
        for t, m, d in zip(res["documents"][0], res["metadatas"][0], res["distances"][0])
    ]


def _doc_id(doc: Document) -> str:
    key = f"{doc.metadata.get('source_file', '')}|{doc.metadata.get('chunk', 0)}|{doc.text}"
    return hashlib.sha1(key.encode("utf-8")).hexdigest()


def _clean_meta(meta: dict) -> dict:
    out = {}
    for key, value in meta.items():
        out[key] = value if isinstance(value, (str, int, float, bool)) else ("" if value is None else str(value))
    return out
