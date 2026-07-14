"""FastAPI 얇은 입구. 그래프를 HTTP로 노출한다.

응답은 백엔드 관례(ApiResponse)와 결을 맞춰 {success, data} 봉투로 감싼다.
"""

from fastapi import FastAPI
from pydantic import BaseModel

from agent.graph import graph

app = FastAPI(title="MarketON AI Agent", version="0.1.0")


class RunRequest(BaseModel):
    input: str


class RunData(BaseModel):
    output: str


class ApiResponse(BaseModel):
    success: bool = True
    data: RunData


@app.get("/health")
def health() -> dict:
    """헬스체크 — 서비스 기동 확인용."""
    return {"success": True, "data": {"status": "ok"}}


@app.post("/agent/run", response_model=ApiResponse)
def run(request: RunRequest) -> ApiResponse:
    """에이전트 그래프를 한 번 실행한다(스캐폴드: echo)."""
    result = graph.invoke({"input": request.input})
    return ApiResponse(data=RunData(output=result.get("output", "")))
