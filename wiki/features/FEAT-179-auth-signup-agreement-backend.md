---
id: FEAT-179
type: feature
status: done
author: horangnabi97
date: 2026-07-07
related: [FEAT-180]
tags: [auth, global, member, backend]
pr: 179
---

## 무엇을 / 왜

회원가입 약관 동의 저장 및 탈퇴 회원 표시명 추가

## 어떻게 (구현 요약)

- 회원가입 시 이용약관/개인정보 수집 및 이용 동의를 `MemberAgreement` 엔티티로 저장(버전/동의시각/IP/User-Agent 포함)
- 둘 중 하나라도 미동의면 회원가입 차단
- 탈퇴 회원의 닉네임을 다른 도메인에서 노출할 때 "탈퇴한 회원입니다"로 표시하는 `Member.getDisplayNickname()` 추가 (호출 지점은 이번 PR에 포함하지 않음 — chat 도메인 담당자가 서비스 단에서 직접 반영 예정)

**검증**

- [x] 빌드 성공 / 서버 정상 기동 / 공통 응답 형식 준수 — `./gradlew test` 전체 통과(489/489), 실서버 curl로 성공/실패 3케이스 재검증

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/auth/controller/AuthController.java` (+16/-3)
- `backend/src/main/java/com/dongnemarket/auth/dto/SignupRequest.java` (+18/-1)
- `backend/src/main/java/com/dongnemarket/auth/service/AuthService.java` (+33/-2)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/member/entity/AgreementType.java` (+13/-0)
- `backend/src/main/java/com/dongnemarket/member/entity/MemberAgreement.java` (+95/-0)
- `backend/src/main/java/com/dongnemarket/member/repository/MemberAgreementRepository.java` (+7/-0)
- `backend/src/test/java/com/dongnemarket/auth/controller/AuthControllerTest.java` (+35/-15)
- `backend/src/test/java/com/dongnemarket/auth/controller/PasswordResetControllerTest.java` (+8/-1)
- `backend/src/test/java/com/dongnemarket/auth/service/AuthServiceTest.java` (+80/-18)
- `backend/src/test/java/com/dongnemarket/member/controller/MemberControllerTest.java` (+11/-3)
- `backend/src/test/java/com/dongnemarket/member/controller/MemberLocationControllerTest.java` (+7/-1)
- `backend/src/test/java/com/dongnemarket/member/entity/MemberTest.java` (+23/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/179
- 이슈: 없음
