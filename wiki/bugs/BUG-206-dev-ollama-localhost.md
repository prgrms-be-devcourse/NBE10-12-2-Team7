---
id: BUG-206
type: bug
status: done
author: jomin4
date: 2026-07-09
related: [BUG-207, FEAT-208]
tags: [관리자AI, ollama, dev, 설정]
pr: 206
---

## 증상
로컬 개발(호스트 `bootRun`, dev 프로파일)에서 관리자 AI 어시스턴트가 항상 **사내망 Ollama IP**(`10.111.111.90:11434`)로만 붙었다. base `application.yml`의 값을 dev·prod·demo가 모두 상속하는 구조라, 사내망 밖에서는 로컬 개발 중에도 AI가 동작하지 않았다.

## 원인
`spring.ai.ollama.base-url`이 base `application.yml`에만 정의돼 있고 dev 프로파일 오버라이드가 없었다. → dev도 사내 IP를 그대로 상속.

## 해결 방법
`application-dev.yml`에 dev 프로파일 한정으로 `spring.ai.ollama` 오버라이드를 추가:
- `base-url: ${OLLAMA_BASE_URL:http://localhost:11434}`
- `chat.options.model: ${OLLAMA_MODEL:qwen3:4b}` (로컬 pull 모델 기준)

env(`OLLAMA_BASE_URL`/`OLLAMA_MODEL`) 주입 시 그 값이 우선 → escape hatch 유지. base/prod/demo/온프레미스는 사내 IP·`qwen3:14b` 그대로라 **배포 환경 무영향**.

## 건드린 파일
- `backend/src/main/resources/application-dev.yml`

## 재발 방지
- 프로파일별로 갈려야 하는 외부 엔드포인트는 base에 두지 말고 프로파일 YAML에서 명시 오버라이드 + env escape hatch를 기본 패턴으로.
- 이 변경만으로는 로컬에서 AI가 실제 응답하진 않았다 — CPU 추론 지연 문제가 남아 후속 [BUG-207](BUG-207-admin-ai-dev-latency.md)로 이어졌다.

## 링크
- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/206
- 이슈: 해당 없음
