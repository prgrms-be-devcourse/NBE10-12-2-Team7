---
id: FEAT-186
type: feature
status: done
author: jomin4
date: 2026-07-07
related: []
tags: [global, backend]
pr: 186
---

## 무엇을 / 왜

ãconfig 수정

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `global` 도메인 — SecurityConfig(기타, 신규)

**검증**

- [ ] 빌드 성공 / 서버 정상 기동
- [ ] 담당 API 정상 동작 (Postman 확인)
- [ ] develop 최신 반영 / 충돌 해결
- [ ] 공통 응답 형식(ApiResponse / ErrorResponse) 준수

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/global/security/SecurityConfig.java` (+1/-0)
- `presentations/marketon-project-interim-2026-07-07.pptx` (+0/-0)
- `presentations/marketon-project-interim-2026-07-07.yaml` (+168/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/186
- 이슈: 없음
