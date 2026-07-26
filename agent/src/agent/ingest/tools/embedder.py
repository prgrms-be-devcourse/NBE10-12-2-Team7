"""embed tool — 텍스트를 bge-m3(Ollama)로 벡터(임베딩)로 만든다.

적재용(embed_texts: 여러 개 배치)과 검색용(embed_query: 하나)을 나눈다.
같은 모델을 써야 적재 벡터와 검색 벡터가 같은 공간에 놓여 비교가 된다.
"""

from __future__ import annotations

from langchain_ollama import OllamaEmbeddings

from agent.config import settings

_embeddings = OllamaEmbeddings(model=settings.embed_model, base_url=settings.ollama_base_url)


def embed_texts(texts: list[str]) -> list[list[float]]:
    """여러 텍스트 → 여러 벡터 (문서 적재용, 배치)."""
    return _embeddings.embed_documents(texts)


def embed_query(text: str) -> list[float]:
    """질문 하나 → 벡터 하나 (검색용)."""
    return _embeddings.embed_query(text)
