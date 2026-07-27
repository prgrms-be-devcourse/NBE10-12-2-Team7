"""노드 단위 테스트 — LLM·검색을 목킹해 결정적 로직만 검증한다.

모델 품질(답변이 좋은가)은 여기서 안 본다 → ④ 평가(eval)에서.
여기서 보는 것: 분기·파싱·임계·조립 같은 '우리 코드'의 행동.
"""

import agent.nodes as nodes
from agent.nodes import finalize_node, retrieve_node, scope_node

_DISCLAIMER_MARK = "법률 자문이 아닙니다"


# ---------- finalize_node: 4갈래 + 면책 항상 부착 ----------

def test_finalize_normal_attaches_answer_and_sources():
    # given: 범위 안 + 근거 있음 + 생성된 답변과 검색 근거
    state = {
        "in_scope": True, "grounded": True, "answer": "환불 가능성이 있습니다.",
        "retrieved": [{"text": "민법 제580조 하자담보책임", "source": "손해배상", "score": 0.8}],
    }
    # when: 마무리하면
    out = finalize_node(state)
    # then: 답변 본문 + 면책이 붙고, 출처가 채워진다
    assert "환불 가능성이 있습니다." in out["answer"]
    assert _DISCLAIMER_MARK in out["answer"]
    assert out["sources"] == [{"title": "손해배상", "snippet": "민법 제580조 하자담보책임"}]


def test_finalize_no_grounding_uses_fallback_and_no_sources():
    # given: 범위 안이지만 근거 없음
    out = finalize_node({"in_scope": True, "grounded": False, "retrieved": []})
    # then: 자료 없음 안내 + 면책, 출처는 비어있다
    assert "찾지 못했습니다" in out["answer"]
    assert _DISCLAIMER_MARK in out["answer"]
    assert out["sources"] == []


def test_finalize_emergency_shows_112():
    # given: 긴급 신호
    out = finalize_node({"emergency": True})
    # then: 112 안내 + 면책
    assert "112" in out["answer"]
    assert _DISCLAIMER_MARK in out["answer"]
    assert out["sources"] == []


def test_finalize_out_of_scope_shows_notice():
    # given: 범위 밖(비긴급)
    out = finalize_node({"in_scope": False})
    # then: 범위밖 안내 + 면책
    assert "범위" in out["answer"]
    assert _DISCLAIMER_MARK in out["answer"]
    assert out["sources"] == []


# ---------- retrieve_node: 변환 + grounded 임계 (store.search 목킹) ----------

def test_retrieve_grounded_when_top_score_passes(monkeypatch):
    # given: 유사도 높은(거리 0.2) 검색 결과 하나
    hits = [{"text": "질문문", "metadata": {"answer": "답변문", "title": "손해배상"}, "distance": 0.2}]
    monkeypatch.setattr(nodes.store, "search", lambda q, k: hits)
    # when: 검색하면
    out = retrieve_node({"question": "환불되나요"})
    # then: score=1-거리=0.8, 임계(0.5) 통과 → grounded, 근거는 answer, 출처는 title
    assert out["grounded"] is True
    assert out["retrieved"][0]["score"] == 0.8
    assert out["retrieved"][0]["text"] == "답변문"
    assert out["retrieved"][0]["source"] == "손해배상"


def test_retrieve_not_grounded_when_below_threshold(monkeypatch):
    # given: 유사도 낮은(거리 0.7) 결과만
    hits = [{"text": "질문문", "metadata": {"answer": "답"}, "distance": 0.7}]
    monkeypatch.setattr(nodes.store, "search", lambda q, k: hits)
    # when: 검색하면
    out = retrieve_node({"question": "점심 뭐먹지"})
    # then: score 0.3 < 0.5 → grounded False (생성이 스킵될 것)
    assert out["grounded"] is False


def test_retrieve_empty_result_not_grounded(monkeypatch):
    # given: 검색 결과가 없으면
    monkeypatch.setattr(nodes.store, "search", lambda q, k: [])
    # when: 검색하면
    out = retrieve_node({"question": "아무거나"})
    # then: retrieved 비고 grounded False
    assert out["retrieved"] == []
    assert out["grounded"] is False


# ---------- scope_node: 긴급 규칙컷 + NIM 파싱 (generate 목킹) ----------

def test_scope_emergency_is_rule_based_and_skips_llm(monkeypatch):
    # given: generate가 불리면 기록(불리면 안 된다)
    calls = []
    monkeypatch.setattr(nodes, "generate", lambda s, u: calls.append(u) or "YES")
    # when: 긴급 신호가 든 질문
    out = scope_node({"question": "너무 힘들어서 자해하고 싶어요"})
    # then: 규칙으로 즉시 컷, NIM은 호출되지 않는다
    assert out == {"in_scope": False, "emergency": True}
    assert calls == []


def test_scope_yes_is_in_scope(monkeypatch):
    # given: NIM이 YES라고 하면
    monkeypatch.setattr(nodes, "generate", lambda s, u: "YES")
    # then: 범위 안, 긴급 아님
    out = scope_node({"question": "중고 환불 문의"})
    assert out["in_scope"] is True
    assert out["emergency"] is False


def test_scope_no_is_out_of_scope(monkeypatch):
    # given: NIM이 NO라고 하면
    monkeypatch.setattr(nodes, "generate", lambda s, u: "NO")
    # then: 범위 밖
    assert scope_node({"question": "양도소득세 얼마?"})["in_scope"] is False


def test_scope_ambiguous_defaults_in_scope(monkeypatch):
    # given: 모델이 애매하게 답하면
    monkeypatch.setattr(nodes, "generate", lambda s, u: "글쎄요 상황에 따라 다릅니다")
    # then: 안전하게 True (뒤의 근거 게이트가 거른다)
    assert scope_node({"question": "이건 뭔가요"})["in_scope"] is True
