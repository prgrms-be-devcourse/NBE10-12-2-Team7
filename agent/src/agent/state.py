"""LangGraph State — 그래프의 노드 사이를 흐르는 계약(스키마).

거래 법률 도우미 런타임 그래프의 상태. 각 노드가 자기 필드만 채우고,
조건부 엣지는 in_scope·grounded를 읽어 경로를 정한다.
"""

from typing import TypedDict


class AgentState(TypedDict, total=False):
    # [입력] 사용자 질문
    question: str

    # [scope] 중고거래 민사 범위 안인가 (범위 밖·긴급이면 False → 생성 스킵)
    in_scope: bool
    emergency: bool   # 신변 위협·자해 등 긴급 신호(코드 규칙으로 감지) → 112 안내

    # [retrieve] 검색 쿼리 / top-k 결과 / 근거 충분 여부(임계 통과)
    query: str
    retrieved: list[dict]   # [{"text", "source", "score"}]
    grounded: bool

    # [generate] 근거로 생성한 답변 본문
    answer: str

    # [finalize] 응답에 첨부할 출처 목록
    sources: list[dict]     # [{"title", "snippet"}]
