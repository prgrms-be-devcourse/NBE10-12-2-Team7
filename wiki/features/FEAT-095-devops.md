---
id: FEAT-095
type: feature
status: done
author: jomin4
date: 2026-07-02
related: []
tags: [admin, global, backend]
pr: 95
---

## 무엇을 / 왜

ai기능구현 ë

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `admin` 도메인 — AdminAiConfig(설정, 신규), AdminAiController(컨트롤러, 신규), AdminAiChatRequest(DTO, 신규), AdminAiChatResponse(DTO, 신규), AdminCommentTools(기타, 신규), AdminDashboardTools(기타, 신규), AdminMemberTools(기타, 신규), AdminProductTools(기타, 신규), AdminReportTools(기타, 신규)
- `global` 도메인 — SecurityConfig(기타, 신규)
- 리소스 — `application-prod.yml` (+33), `application.yml` (+11)
- 추가된 API 표면 — `@RequestMapping("/api/admin/ai")`, `@PostMapping("/chat")`

## 건드린 파일

- `backend/Dockerfile` (+7/-0)
- `backend/build.gradle` (+12/-0)
- `backend/docker-compose.prod.yml` (+44/-0)
- `backend/src/main/java/com/dongnemarket/admin/ai/config/AdminAiConfig.java` (+51/-0)
- `backend/src/main/java/com/dongnemarket/admin/ai/controller/AdminAiController.java` (+45/-0)
- `backend/src/main/java/com/dongnemarket/admin/ai/dto/AdminAiChatRequest.java` (+21/-0)
- `backend/src/main/java/com/dongnemarket/admin/ai/dto/AdminAiChatResponse.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/admin/ai/tool/AdminCommentTools.java` (+27/-0)
- `backend/src/main/java/com/dongnemarket/admin/ai/tool/AdminDashboardTools.java` (+26/-0)
- `backend/src/main/java/com/dongnemarket/admin/ai/tool/AdminMemberTools.java` (+36/-0)
- `backend/src/main/java/com/dongnemarket/admin/ai/tool/AdminProductTools.java` (+34/-0)
- `backend/src/main/java/com/dongnemarket/admin/ai/tool/AdminReportTools.java` (+34/-0)
- `backend/src/main/java/com/dongnemarket/global/security/SecurityConfig.java` (+2/-0)
- `backend/src/main/resources/application-prod.yml` (+33/-0)
- `backend/src/main/resources/application.yml` (+11/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/95
- 이슈: 없음
