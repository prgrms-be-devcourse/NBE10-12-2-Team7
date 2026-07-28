---
id: FEAT-077
type: feature
status: done
author: horangnabi97
date: 2026-07-01
related: []
tags: [auth, member, backend, test]
pr: 77
---

## 무엇을 / 왜

auth/member 서비스 테스트 mock 정리

## 어떻게 (구현 요약)

**AuthServiceTest**
- PasswordEncoder, JwtTokenProvider Mock 제거
- 실제 BCryptPasswordEncoder, JwtTokenProvider 사용
- 로그인 성공 테스트에서 실제 JWT 발급 후 memberId 검증
- ReflectionTestUtils를 이용한 status 변경 제거
- Member.changeStatus() 사용
- JPA가 생성하는 id 설정을 위한 ReflectionTestUtils.setField(member, "id", ...)는 유지

**MemberServiceTest**
- mock(Member.class) 제거
- 실제 Member 엔티티 사용
- changeStatus(SUSPENDED)를 통해 상태 변경
- Repository만 Mock 유지

**검증**

```bash
./gradlew test \
  --tests "com.dongnemarket.auth.service.AuthServiceTest" \
  --tests "com.dongnemarket.member.service.MemberServiceTest"
```

✅ AuthServiceTest: 11/11
✅ MemberServiceTest: 13/13
✅ BUILD SUCCESSFUL

## 건드린 파일

- `backend/src/test/java/com/dongnemarket/auth/service/AuthServiceTest.java` (+26/-21)
- `backend/src/test/java/com/dongnemarket/member/service/MemberServiceTest.java` (+6/-7)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/77
- 이슈: 없음
