---
id: FEAT-011
type: feature
status: done
author: horangnabi97
date: 2026-06-25
related: []
tags: [auth, member, backend, docs]
pr: 11
---

## 무엇을 / 왜

[Auth] 회원가입(Signup) API 구현

## 어떻게 (구현 요약)

**작업 내용**: 회원가입(`POST /api/auth/signup`) API 구현 + 저장 시점 unique 제약 위반(race condition) 보완
**담당 도메인**: auth, member — Member 엔티티/Repository는 회원가입 구현을 위한 기반 코드로 이 PR에 함께 포함하며, 별도 PR로 분리하지 않음

**변경 파일**
- [x] `backend/src/main/java/com/dongnemarket/auth/controller/AuthController.java`
- [x] `backend/src/main/java/com/dongnemarket/auth/dto/SignupRequest.java`
- [x] `backend/src/main/java/com/dongnemarket/auth/dto/SignupResponse.java`
- [x] `backend/src/main/java/com/dongnemarket/auth/service/AuthService.java`
- [x] `backend/src/main/java/com/dongnemarket/member/entity/Member.java`
- [x] `backend/src/main/java/com/dongnemarket/member/entity/MemberStatus.java`
- [x] `backend/src/main/java/com/dongnemarket/member/entity/Role.java`
- [x] `backend/src/main/java/com/dongnemarket/member/repository/MemberRepository.java`
- [x] `backend/src/test/java/com/dongnemarket/auth/controller/AuthControllerTest.java`
- [x] `backend/src/test/java/com/dongnemarket/auth/service/AuthServiceTest.java`
- [x] `docs/postman/auth-signup.md`

**검증**

- [x] `./gradlew build` 성공
- [x] `AuthServiceTest` 6개 통과 (성공1 + 실패2 + race condition 3)
- [x] `AuthControllerTest` 4개 통과 (성공1 + 실패3: 이메일중복/닉네임중복/validation)
- [x] 기존 `SecurityPolicyTest` 2개, `DongneMarketApplicationTests` 1개 — 영향 없음 확인

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/auth/controller/AuthController.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/SignupRequest.java` (+41/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/SignupResponse.java` (+32/-0)
- `backend/src/main/java/com/dongnemarket/auth/service/AuthService.java` (+56/-0)
- `backend/src/main/java/com/dongnemarket/member/entity/Member.java` (+86/-0)
- `backend/src/main/java/com/dongnemarket/member/entity/MemberStatus.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/member/entity/Role.java` (+9/-0)
- `backend/src/main/java/com/dongnemarket/member/repository/MemberRepository.java` (+11/-0)
- `backend/src/test/java/com/dongnemarket/auth/controller/AuthControllerTest.java` (+86/-0)
- `backend/src/test/java/com/dongnemarket/auth/service/AuthServiceTest.java` (+120/-0)
- `docs/postman/auth-signup.md` (+147/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- [ ] ERD 기준과 대부분 동일하게 설계했으며, 닉네임 길이는 ERD의 50자 대신 서비스 사용성을 고려해 20자로 제한했습니다.
- [ ] `login`(`POST /api/auth/login`)은 이번 PR 범위 밖, 다음 작업 단위로 별도 진행 예정

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/11
- 이슈: 없음
