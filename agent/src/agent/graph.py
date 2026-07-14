"""그래프 조립 = 정본. 노드·엣지를 여기서 엮어 컴파일된 그래프를 만든다.

스캐폴드 단계에서는 LLM을 호출하지 않는 echo 노드 하나만 둔다(오프라인 테스트 통과용).
Phase 1부터 실제 노드(설명 생성·검색 등)를 추가한다.
"""

from langgraph.graph import END, START, StateGraph

from agent.state import AgentState


def echo_node(state: AgentState) -> AgentState:
    """입력을 그대로 출력으로 옮기는 최소 노드(플레이스홀더)."""
    return {"output": state.get("input", "")}


def build_graph():
    """컴파일된 LangGraph를 반환한다."""
    builder = StateGraph(AgentState)
    builder.add_node("echo", echo_node)
    builder.add_edge(START, "echo")
    builder.add_edge("echo", END)
    return builder.compile()


# 앱 전역에서 재사용할 컴파일된 그래프
graph = build_graph()
