---
id: FEAT-072
type: feature
status: done
author: jomin4
date: 2026-06-29
related: []
tags: [admin, global, backend, docs]
pr: 72
---

## 무엇을 / 왜

ê관리자 api 구현 및 문서 수정

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `admin` 도메인 — AdminMemberController(컨트롤러, 신규), AdminReportController(컨트롤러, 신규), AdminMemberStatusUpdateRequest(DTO, 신규), AdminReportStatusUpdateRequest(DTO, 신규), AdminAccountInitializer(시더, 신규), AdminMemberService(서비스, 신규), AdminReportService(서비스, 신규)
- `global` 도메인 — ErrorCode(예외, 수정)
- 문서 — `docs/postman/` 5개, `docs/proposal/` 4개
- 추가된 API 표면 — `@PatchMapping("/{memberId}/status")`, `@PatchMapping("/{reportId}/status")`
- 테스트 — 5개 파일 (+200줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/admin/controller/AdminMemberController.java` (+12/-0)
- `backend/src/main/java/com/dongnemarket/admin/controller/AdminReportController.java` (+12/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminMemberStatusUpdateRequest.java` (+17/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminReportStatusUpdateRequest.java` (+17/-0)
- `backend/src/main/java/com/dongnemarket/admin/init/AdminAccountInitializer.java` (+43/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminMemberService.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminReportService.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+3/-1)
- `backend/src/test/java/com/dongnemarket/admin/controller/AdminMemberControllerTest.java` (+34/-0)
- `backend/src/test/java/com/dongnemarket/admin/controller/AdminReportControllerTest.java` (+31/-0)
- `backend/src/test/java/com/dongnemarket/admin/init/AdminAccountInitializerTest.java` (+51/-0)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminMemberServiceTest.java` (+48/-0)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminReportServiceTest.java` (+36/-0)
- `docs/postman/admin-comments.md` (+123/-0)
- `docs/postman/admin-dashboard.md` (+69/-0)
- … 외 10개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/72
- 이슈: 없음
