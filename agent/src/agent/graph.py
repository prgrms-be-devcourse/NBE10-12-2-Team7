"""그래프 조립 = 정본. 노드·조건부 엣지를 엮어 컴파일된 RAG 그래프를 만든다.

거래 법률 도우미 런타임: scope → (범위밖·긴급) finalize / (범위안) retrieve
→ (근거없음) finalize / (근거있음) generate → finalize → END.
분기의 근거는 State의 in_scope·grounded (조건부 엣지가 읽는다).
"""

from langgraph.graph import END, START, StateGraph

from agent.nodes import finalize_node, generate_node, retrieve_node, scope_node
from agent.state import AgentState


def _after_scope(state: AgentState) -> str:
    """범위 안이면 검색으로, 범위 밖·긴급이면 바로 마무리로."""
    return "retrieve" if state.get("in_scope") else "finalize"


def _after_retrieve(state: AgentState) -> str:
    """근거가 임계를 넘으면 생성으로, 아니면 마무리로(생성 스킵)."""
    return "generate" if state.get("grounded") else "finalize"


def build_graph():
    """거래 법률 도우미 런타임 그래프를 컴파일해 반환한다."""
    builder = StateGraph(AgentState)
    builder.add_node("scope", scope_node)
    builder.add_node("retrieve", retrieve_node)
    builder.add_node("generate", generate_node)
    builder.add_node("finalize", finalize_node)

    builder.add_edge(START, "scope")
    builder.add_conditional_edges(
        "scope", _after_scope, {"retrieve": "retrieve", "finalize": "finalize"}
    )
    builder.add_conditional_edges(
        "retrieve", _after_retrieve, {"generate": "generate", "finalize": "finalize"}
    )
    builder.add_edge("generate", "finalize")
    builder.add_edge("finalize", END)
    return builder.compile()


graph = build_graph()
