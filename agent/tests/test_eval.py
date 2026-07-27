"""구조 평가(eval) — 그래프를 API로 통째로 돌려 '응답 구조·가드레일'을 검증한다.

품질(답변 내용)은 여기서 안 본다 → 라이브(ⓑ)에서. 여기선 결정성을 위해
외부 의존(NIM generate·Chroma search)만 가짜로 바꾸고, 그래프·노드·엣지·
finalize·API 직렬화는 전부 진짜로 태운다(=E2E). 라우팅은 가짜가 강제한다.
"""

from fastapi.testclient import TestClient

import agent.nodes as nodes
from agent.api import app

client = TestClient(app)
_DISCLAIMER = "법률 자문이 아닙니다"


def _fake_generate(scope_verdict):
    """scope 콜엔 YES/NO, 생성 콜엔 고정 답변을 주는 가짜 generate."""
    def _gen(system, user):
        if "YES" in system and "NO" in system:        # scope 분류 프롬프트
            return scope_verdict
        return "관련 법리와 실무 단계는 다음과 같습니다."  # 답변 생성
    return _gen


def _fake_search(distance):
    """고정 거리의 검색 결과 하나를 주는 가짜 search."""
    def _search(query, k):
        return [{"text": "질문문", "metadata": {"answer": "민법상 근거", "title": "손해배상"}, "distance": distance}]
    return _search


def _ask(question):
    return client.post("/agent/legal/ask", json={"question": question}).json()


def test_grounded_question_returns_answer_sources_and_disclaimer(monkeypatch):
    # given: 범위 안(YES) + 유사도 높은 근거(거리 0.2)
    monkeypatch.setattr(nodes, "generate", _fake_generate("YES"))
    monkeypatch.setattr(nodes.store, "search", _fake_search(0.2))
    # when: 하자 환불 질문을 API로
    body = _ask("중고 노트북 하자, 환불되나요")
    # then: 봉투 구조 완비 + 답변·출처 채워짐 + 면책 항상
    assert body["success"] is True
    data = body["data"]
    assert set(data) >= {"answer", "sources", "inScope", "grounded"}
    assert data["inScope"] is True and data["grounded"] is True
    assert data["answer"] != "" and _DISCLAIMER in data["answer"]
    assert len(data["sources"]) >= 1
    assert set(data["sources"][0]) == {"title", "snippet"}


def test_out_of_scope_question_has_no_sources(monkeypatch):
    # given: 범위 밖(NO)
    monkeypatch.setattr(nodes, "generate", _fake_generate("NO"))
    monkeypatch.setattr(nodes.store, "search", _fake_search(0.2))
    # when: 세금 질문
    data = _ask("아파트 양도소득세 얼마인가요")["data"]
    # then: inScope False, 출처 없음, 면책은 있음
    assert data["inScope"] is False
    assert data["sources"] == []
    assert _DISCLAIMER in data["answer"]


def test_no_grounding_question_declines_with_disclaimer(monkeypatch):
    # given: 범위 안(YES)이나 유사도 미달(거리 0.7)
    monkeypatch.setattr(nodes, "generate", _fake_generate("YES"))
    monkeypatch.setattr(nodes.store, "search", _fake_search(0.7))
    # when: 근거 없는 질문
    data = _ask("우주여행 계약은 어떻게 하나요")["data"]
    # then: grounded False, 출처 없음, "모른다" 취지 + 면책
    assert data["grounded"] is False
    assert data["sources"] == []
    assert "찾지 못했습니다" in data["answer"] and _DISCLAIMER in data["answer"]


def test_emergency_question_routes_to_112_without_external_calls(monkeypatch):
    # given: 긴급은 규칙컷 → NIM·검색이 불리면 실패시킨다
    def _boom(*a, **k):
        raise AssertionError("긴급 경로에서 외부 호출이 발생하면 안 된다")
    monkeypatch.setattr(nodes, "generate", _boom)
    monkeypatch.setattr(nodes.store, "search", _boom)
    # when: 위협 질문
    data = _ask("칼로 위협받고 있어요")["data"]
    # then: 112 안내 + 면책, 출처 없음, inScope False (외부 호출 없이)
    assert "112" in data["answer"] and _DISCLAIMER in data["answer"]
    assert data["inScope"] is False
    assert data["sources"] == []
