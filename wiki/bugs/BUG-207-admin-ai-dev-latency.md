---
id: BUG-207
type: bug
status: done
author: jomin4
date: 2026-07-09
related: [BUG-206, FEAT-208]
tags: [관리자AI, ollama, 타임아웃, 성능, dev]
pr: 207
---

## 증상
로컬(dev)에서 관리자 AI에 "대시보드 현황 알려줘" 등을 물으면 **"답변을 가져오지 못했어요"** 만 반환. ([BUG-206](BUG-206-dev-ollama-localhost.md)으로 로컬 Ollama에 붙게 만든 뒤에도 여전히 실패.)

## 원인 (실측)
- 이 머신은 GPU 가속 없음(AMD 780M iGPU, `size_vram=0`) → **CPU-only 추론 ~5 토큰/초**.
- 여유 RAM 부족(0.8GB free)으로 `qwen3:4b`(3.2GB)가 스와핑 → tool 호출 단일 패스가 180초에도 미완.
- 느린 생성이 Spring AI Ollama 클라이언트 기본 read-timeout을 초과 → 백엔드 예외(비-2xx) → 프론트가 폴백 메시지 표시.

## 해결 방법
dev 한정 + 무해한 공통 기본값으로 조정:
- **`application-dev.yml`**: 모델 기본값 `qwen3:4b` → **`qwen3:1.7b`**(경량, tool 지원 동일). `spring.http.client.read-timeout: 600s`(non-stream `ChatClient.call()`이 쓰는 RestClient가 느린 생성을 끝까지 대기 — Spring Boot 3.4+).
- **`AdminAiConfig.defaultOptions`**: `keepAlive("30m")`(모델 상주 → 콜드로드 제거), `numPredict(512)`(답변 길이 상한으로 최악 응답 시간 bound), `temperature(0.1)`(defaultOptions로 이동), `disableThinking()` 유지.

base/prod/demo는 사내 IP·`qwen3:14b` 그대로 → 배포 환경 무영향. `keepAlive`/`numPredict`는 코드 공통이나 무해한 기본값(사내 서버는 더 빠름).

## 건드린 파일
- `backend/src/main/resources/application-dev.yml`
- `backend/src/main/java/com/dongnemarket/admin/ai/config/AdminAiConfig.java`

## 재발 방지
- 로컬 CPU 환경 전제를 dev 프로파일에 못박아, GPU 없는 팀원도 AI를 돌릴 수 있게 함.
- ⚠️ 런타임 검증은 수동 의존: `ollama pull qwen3:1.7b` 후 백엔드 재기동 → 실제 반환 확인. AI 테스트 중 RAM 확보(불필요한 앱 종료) 권장 — 스와핑 방지. (자동 회귀 테스트 없음)

## 링크
- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/207
- 이슈: 해당 없음
