---
id: FEAT-020
type: feature
status: done
author: horangnabi97
date: 2026-06-26
related: []
tags: [auth, member, backend]
pr: 20
---

## 무엇을 / 왜

로그인 API 구현

## 어떻게 (구현 요약)

- `POST /api/auth/login` 로그인 API 구현
- 이메일/비밀번호 검증 후 JWT Access Token 발급
- 탈퇴(DELETED)/정지(SUSPENDED) 회원 로그인 차단
- `LoginRequest` / `LoginResponse` DTO 추가
- `MemberRepository.findByEmail()` 추가

**검증**

**단위 테스트 (AuthServiceTest)** — BUILD SUCCESSFUL
- login_success: 올바른 자격증명 → accessToken 반환
- login_emailNotFound: 없는 이메일 → MEMBER_NOT_FOUND
- login_wrongPassword: 틀린 비밀번호 → INVALID_PASSWORD
- login_deletedMember: 탈퇴 회원 → DELETED_MEMBER
- login_suspendedMember: 정지 회원 → SUSPENDED_MEMBER

**통합 테스트 (AuthControllerTest)** — BUILD SUCCESSFUL
- login_success, login_emailNotFound, login_wrongPassword, login_invalidEmailFormat, login_blankPassword

**로컬 서버 직접 검증 (docker-compose MySQL + bootRun)**
| # | 케이스 | HTTP | 결과 |
|---|---|---|---|
| 1 | 회원가입 성공 | 201 | memberId, email, nickname 반환 확인 |
| 2 | 로그인 성공 | 200 | accessToken 발급 확인 |
| 3 | 존재하지 않는 이메일 | 404 | MEMBER_NOT_FOUND |
| 4 | 비밀번호 불일치 | 401 | INVALID_PASSWORD |
| 5 | 이메일 형식 오류 | 400 | INVALID_INPUT_VALUE |
| 6 | 비밀번호 빈 값 | 400 | INVALID_INPUT_VALUE |

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/auth/controller/AuthController.java` (+9/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/LoginRequest.java` (+30/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/LoginResponse.java` (+18/-0)
- `backend/src/main/java/com/dongnemarket/auth/service/AuthService.java` (+25/-1)
- `backend/src/main/java/com/dongnemarket/member/repository/MemberRepository.java` (+4/-0)
- `backend/src/test/java/com/dongnemarket/auth/controller/AuthControllerTest.java` (+59/-0)
- `backend/src/test/java/com/dongnemarket/auth/service/AuthServiceTest.java` (+76/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 없음 (이번 PR은 login 기능만 포함)
- 다음 PR: `GET /api/members/me` (내 정보 조회)

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/20
- 이슈: 없음
