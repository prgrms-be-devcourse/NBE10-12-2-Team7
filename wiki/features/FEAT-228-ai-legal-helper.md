---
id: FEAT-228
type: feature
status: done
author: jomin4
date: 2026-07-26
related: [FEAT-208]
tags: [AI, agent, RAG, LangGraph, 거래법률, 민사]
pr: 228
---

## 무엇을 / 왜
중고거래 중 생긴 **민사** 분쟁(계약·환불·하자·미배송·사기 등)에 일반적인 법률 '정보'를 안내하는 **사용자 도메인 첫 AI** = '거래 법률 도우미'. 관리자 AI([FEAT-208])만 있고 사용자 쪽엔 AI가 없어, AI Hub 민사법 데이터를 **검색 근거(RAG)**로 1차 법률 정보 창구를 제공한다. 법률 '자문'이 아니며 면책을 상시 부착한다.

## 어떻게 (구현 요약)
기존 Spring 백엔드와 분리된 **독립 Python 모듈 `agent/`** (LangGraph · FastAPI). 두 에이전트가 **Vector DB(Chroma)**를 다리로 이어진다.

- **인제스트(오프라인 ETL)** — 순수 함수 **스트리밍** 파이프라인: `load → normalize → 키워드필터 → chunk → embed(bge-m3) → store(Chroma, sha1 upsert 멱등)`. batch flush로 메모리 일정. CLI `python -m agent.ingest.cli ingest|search|stats`.
- **런타임 RAG** — LangGraph **조건부 그래프**: `scope`(범위판단) → `retrieve`(top-k + 유사도 임계) → `generate`(NIM) → `finalize`. **그라운딩 2겹**: ① 검색 임계 미달 시 생성 스킵 ② 면책·출처를 **코드로 항상** 부착. `scope`는 하이브리드(긴급 규칙컷 + 애매하면 NIM), 범위밖·긴급(112) 라우팅.
- **모델 배치** — 생성=NVIDIA NIM `llama-3.1-8b`, 임베딩·검색=로컬 bge-m3.
- **API** — `POST /agent/legal/ask` → `{success, data:{answer, sources, inScope, grounded}}`.
- **테스트** — `pytest` 20 passed (노드 단위 목킹 11 + 라우터 2 + 구조 eval E2E 4 + API/그래프 스모크 3).

## 건드린 파일
- `agent/src/agent/ingest/document.py`, `tools/{loader,normalize,filters,chunk,embedder,store}.py`, `pipeline.py`, `cli.py`
- `agent/src/agent/{state,nodes,graph,api,llm,config}.py`
- `agent/tests/{test_nodes,test_eval,test_graph,test_api}.py`
- `agent/pyproject.toml`, `agent/.env.example`

## 결정과 트레이드오프
- **RAG 강제(파라메트릭 지식 금지)** — 소형 모델은 한국 법률 사실을 환각하므로 검색 그라운딩이 척추.
- **ETL은 LangGraph 대신 순수 함수** — 분기 없는 직선 파이프라인이라 LangGraph는 장식 → 제거. LangGraph는 분기가 있는 런타임 그래프에만 사용.
- **인제스트 필터: NIM 관계성 게이트 폐기 → 키워드-only** — 클라우드 의존·비용 제거. 정밀도는 약간 포기(v1), 필요 시 캐스케이드 재도입.
- **생성 모델: 로컬 qwen3:4b → NIM `llama-3.1-8b`** — 로컬 CPU에서 응답 100s+ → ~3s(20~100배). 병목은 주입량(입력 ~1,900토큰=작음)이 아니라 대형 모델 처리속도 + 무료티어 콜드스타트였음. 트레이드오프 = 질문이 클라우드로 나감(포폴/학습 목적이라 수용), **임베딩은 로컬 유지**.
- **그라운딩 2겹을 코드로 강제** — 프롬프트만으론 면책·근거 100% 보장 안 됨.

## 남은 이슈 / 후속 작업
- **ⓑ 품질 평가(라이브)** — RAGAS 스타일 `faithfulness`·`context recall` + 지연 측정. 8B 답변이 가끔 빗나간 인용(예: 민사소송법 조항)을 함 → faithfulness로 정량화 필요.
- **전량 80k 인제스트(~6h)** — 현재 실 컬렉션(`minsa_legal`)은 비어 있고 스모크 60건만 검증됨.
- **FE '거래 법률 도우미' 페이지 연동** — frontend 워크트리(별도 과제). agent는 API 계약만 제공.
- 새 스택(LangGraph·Chroma·NIM)·새 엔드포인트의 **repo 차원 ADR/architecture 반영 여부**는 팀장 확인 대기.
- Notion `역할` 옵션에 'AI' 부재 → 임시 BE.

## 링크
- PR: [#228](https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/228)
- 이슈: 해당 없음
- Notion 설계: https://app.notion.com/p/39dcd6a1d0238175acc0f3cdfc3e933a
