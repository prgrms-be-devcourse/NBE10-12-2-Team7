"""그래프 스모크 테스트 — 라이브 모델 없이 통과해야 한다."""

from agent.graph import build_graph


def test_graph_echoes_input_to_output():
    # given: 컴파일된 스캐폴드 그래프
    graph = build_graph()

    # when: 입력을 넣어 한 번 실행하면
    result = graph.invoke({"input": "안녕하세요"})

    # then: echo 노드가 입력을 출력으로 옮긴다
    assert result["output"] == "안녕하세요"
