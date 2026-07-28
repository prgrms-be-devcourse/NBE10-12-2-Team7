---
id: FEAT-036
type: feature
status: done
author: horangnabi97
date: 2026-06-26
related: []
tags: [member, backend]
pr: 36
---

## 무엇을 / 왜

내 정보 조회/수정/탈퇴 API 구현

## 어떻게 (구현 요약)

- `GET /api/members/me` — 현재 로그인한 사용자 정보 조회 (id, email, nickname, role, status, createdAt)
- `PATCH /api/members/me` — 닉네임 수정 (중복 닉네임 409 DUPLICATE_NICKNAME)
- `DELETE /api/members/me` — 소프트 삭제 (status=DELETED, deletedAt 설정, 이후 로그인 불가)

세 API 모두 Member 도메인의 "내 정보 관리" 기능으로, MemberController · MemberService · MemberRepository · Member Entity 의존성이 공유되어 하나의 PR로 묶었습니다.

**검증**

- 전체 테스트 BUILD SUCCESSFUL, failures=0, errors=0
- MemberServiceTest (단위, Mockito): 5케이스 PASS
- MemberControllerTest (통합, H2 + MockMvc): 10케이스 PASS
- docs/ai/00-ai-common-rules.md 기준 검증 완료 (ErrorCode, 응답 포맷, 계층 분리, Validation, Swagger 모두 일치)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/member/controller/MemberController.java` (+52/-0)
- `backend/src/main/java/com/dongnemarket/member/dto/MemberResponse.java` (+62/-0)
- `backend/src/main/java/com/dongnemarket/member/dto/MemberUpdateRequest.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/member/entity/Member.java` (+9/-0)
- `backend/src/main/java/com/dongnemarket/member/repository/MemberRepository.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/member/service/MemberService.java` (+47/-0)
- `backend/src/test/java/com/dongnemarket/member/controller/MemberControllerTest.java` (+186/-0)
- `backend/src/test/java/com/dongnemarket/member/service/MemberServiceTest.java` (+119/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- `INVALID_TOKEN` (AUTH_004) ErrorCode가 정의되어 있으나 현재 잘못된 JWT 전송 시 `UNAUTHORIZED`(COMMON_003)가 반환됩니다. 수정하려면 `global/security/jwt/` 파일 수정이 필요합니다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/36
- 이슈: 없음
