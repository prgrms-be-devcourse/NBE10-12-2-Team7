---
id: FEAT-117
type: feature
status: done
author: han95white
date: 2026-07-03
related: []
tags: [member, backend]
pr: 117
---

## 무엇을 / 왜

회원 내 동네 설정 및 조회 기능 추가

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `member` 도메인 — MemberLocationController(컨트롤러, 신규), MemberLocationResponse(DTO, 신규), MemberLocationUpdateRequest(DTO, 신규), MemberLocation(엔티티, 신규), MemberLocationRepository(리포지토리, 신규), MemberLocationService(서비스, 신규)
- 추가된 API 표면 — `@RequestMapping("/api/members/me/locations")`, `@PutMapping`, `@GetMapping`
- 테스트 — 3개 파일 (+509줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/member/controller/MemberLocationController.java` (+47/-0)
- `backend/src/main/java/com/dongnemarket/member/dto/MemberLocationResponse.java` (+36/-0)
- `backend/src/main/java/com/dongnemarket/member/dto/MemberLocationUpdateRequest.java` (+25/-0)
- `backend/src/main/java/com/dongnemarket/member/entity/MemberLocation.java` (+75/-0)
- `backend/src/main/java/com/dongnemarket/member/repository/MemberLocationRepository.java` (+18/-0)
- `backend/src/main/java/com/dongnemarket/member/service/MemberLocationService.java` (+89/-0)
- `backend/src/test/java/com/dongnemarket/member/controller/MemberLocationControllerTest.java` (+225/-0)
- `backend/src/test/java/com/dongnemarket/member/repository/MemberLocationRepositoryTest.java` (+85/-0)
- `backend/src/test/java/com/dongnemarket/member/service/MemberLocationServiceTest.java` (+199/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/117
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/111
