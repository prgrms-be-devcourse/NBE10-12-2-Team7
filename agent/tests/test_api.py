"""FastAPI 스모크 테스트 — 라이브 모델 없이 통과해야 한다."""

from fastapi.testclient import TestClient

from agent.api import app

client = TestClient(app)


def test_health_returns_ok():
    # when: 헬스체크를 호출하면
    response = client.get("/health")

    # then: 200과 ok 상태를 준다
    assert response.status_code == 200
    assert response.json()["data"]["status"] == "ok"


def test_run_echoes_input():
    # when: 에이전트 실행 엔드포인트에 입력을 보내면
    response = client.post("/agent/run", json={"input": "테스트"})

    # then: 봉투 형식으로 echo 출력을 돌려준다
    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["data"]["output"] == "테스트"
