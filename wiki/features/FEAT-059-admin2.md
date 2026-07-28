---
id: FEAT-059
type: feature
status: done
author: jomin4
date: 2026-06-28
related: []
tags: [admin, backend, docs]
pr: 59
---

## 무엇을 / 왜

관리자 회원 조회 API 구현

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `admin` 도메인 — AdminMemberController(컨트롤러, 신규), AdminMemberResponse(DTO, 신규), AdminMemberRepository(리포지토리, 신규), AdminMemberService(서비스, 신규)
- 문서 — `docs/postman/` 1개
- 추가된 API 표면 — `@RequestMapping("/api/admin/members")`, `@GetMapping`, `@GetMapping("/{memberId}")`
- 테스트 — 2개 파일 (+225줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/admin/controller/AdminMemberController.java` (+40/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminMemberResponse.java` (+52/-0)
- `backend/src/main/java/com/dongnemarket/admin/repository/AdminMemberRepository.java` (+12/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminMemberService.java` (+36/-0)
- `backend/src/test/java/com/dongnemarket/admin/controller/AdminMemberControllerTest.java` (+148/-0)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminMemberServiceTest.java` (+77/-0)
- `docs/postman/admin-members.md` (+192/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/59
- 이슈: 없음
