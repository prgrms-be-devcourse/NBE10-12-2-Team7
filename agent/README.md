# 마켓온 AI 에이전트 (agent/)

마켓온 **사용자 도메인**에 AI를 도입하는 독립 서비스. 기존 Spring Boot 백엔드와 분리된
**Python + LangGraph + Ollama(qwen3:4b)** FastAPI 서비스다. 백엔드는 이 서비스를 HTTP로 호출한다.

> 이 폴더는 모노레포(`ai-agent/`) 안의 독립 모듈이다(`backend/`·`frontend/`·`infra/`와 형제).
> 개발 워크플로는 **`/ai-workflow`** 스킬을 따른다(설계→구현→단위→평가→ship).

## 스택

|                |                                                           |
| -------------- | --------------------------------------------------------- |
| 오케스트레이션 | LangGraph (State·Node·Tool·Graph)                      |
| LLM            | Ollama 로컬 모델**qwen3:4b** (`langchain-ollama`) |
| API            | FastAPI + uvicorn                                         |
| 테스트         | pytest (LLM은 목킹 · 라이브 호출은`-m live`로 분리)    |

## 사전 준비

```bash
# 1) Ollama 설치 후 모델 받기 (한 번만)
ollama pull qwen3:4b

# 2) 파이썬 환경 (리포 루트가 아니라 이 폴더에서)
cd agent
python -m venv .venv
# Windows PowerShell
.venv\Scripts\Activate.ps1
pip install -e ".[dev]"

# 3) 환경변수
cp .env.example .env   # 값 확인/수정
```

## 실행

```bash
# 개발 서버 (:8000)
uvicorn agent.api:app --reload --port 8000
# 헬스체크
curl http://localhost:8000/health
# 에이전트 실행(스캐폴드 — 아직 echo)
curl -X POST http://localhost:8000/agent/run -H "Content-Type: application/json" -d '{"input":"안녕"}'
```

## 테스트

```bash
pytest            # 기본: 라이브 모델 없이 통과 (LLM 목킹)
pytest -m live    # 실제 Ollama qwen3:4b 호출 테스트만 (모델 기동 필요)
```

## 폴더 구조

```
agent/
├── pyproject.toml        의존성·pytest·ruff 설정
├── .env.example          환경변수 키 목록
├── src/agent/
│   ├── config.py         설정 로드 (Ollama·모델옵션·서버)
│   ├── llm.py            qwen3:4b 클라이언트 팩토리 (thinking off·temp·num_predict·keep_alive 중앙화)
│   ├── state.py          LangGraph State 스키마 (그래프가 흐르는 계약)
│   ├── graph.py          노드·엣지 조립 = 그래프 정본
│   └── api.py            FastAPI 얇은 입구 (/health · /agent/run)
└── tests/                pytest (그래프·API 스모크)
```

## 모델 옵션 메모 (Java admin.ai에서 계승)

기존 백엔드(`admin/ai`)에서 실측 검증된 qwen3 운용값을 그대로 계승한다:

- **thinking 비활성**: qwen3 계열은 thinking이 기본 켜져 최종 답변이 새어 content가 빈다 → 반드시 끈다.
- **temperature 0.1** (결정성), **num_predict 512** (느린 CPU 추론 최악 응답시간 bound), **keep_alive 30m** (콜드로드 제거).
