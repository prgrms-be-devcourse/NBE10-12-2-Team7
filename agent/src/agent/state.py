"""LangGraph State — 그래프의 노드 사이를 흐르는 계약(스키마).

기능이 붙기 전 스캐폴드 단계라 최소 필드만 둔다. 기능별로 필드를 확장한다.
"""

from typing import TypedDict


class AgentState(TypedDict, total=False):
    # 사용자 입력 (에이전트로 들어온 원문)
    input: str
    # 에이전트 최종 출력
    output: str
