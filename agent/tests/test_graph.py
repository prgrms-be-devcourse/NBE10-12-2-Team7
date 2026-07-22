"""그래프 스모크 테스트 — 라이브 모델 없이 통과해야 한다.

긴급 경로는 규칙으로 즉시 컷되어 NIM·검색 없이 돈다 → 오프라인 검증에 적합.
(scope NIM·retrieve·generate를 목킹한 본격 단위테스트는 ③ 단위 테스트 단계에서.)
"""

from agent.graph import build_graph


def test_emergency_routes_to_safety_notice():
    # given: 컴파일된 런타임 그래프
    graph = build_graph()

    # when: 급박한 위험 신호가 담긴 질문을 넣으면 (규칙컷 → NIM 없이)
    result = graph.invoke({"question": "너무 힘들어서 자해하고 싶어요"})

    # then: 긴급으로 분기해 112 안내 + 면책이 붙고, 출처는 없다
    assert result["emergency"] is True
    assert result["in_scope"] is False
    assert "112" in result["answer"]
    assert "법률 자문이 아닙니다" in result["answer"]
    assert result["sources"] == []
