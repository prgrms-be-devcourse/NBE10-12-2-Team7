---
id: FEAT-167
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-06
related: []
tags: [globals, my-profile, signup, frontend]
pr: 167
---

## 무엇을 / 왜

김대연 님이 요청한 인증 관련 프론트 개선 4가지를 구현했다.
로그아웃 후 이동 처리, 회원가입 이메일 인증, 비밀번호 확인 입력란, 비밀번호 변경 기능을 추가했다.

## 어떻게 (구현 요약)

- 로그아웃 성공/실패와 무관하게 `/login`으로 이동(전체 새로고침)하도록 수정했다.
기존에는 로그아웃 후 화면이 그대로라 성공 여부를 확인하기 어려웠다.
- 회원가입 페이지에 이메일 인증 UI/로직을 추가했다. 이메일 입력 → 인증번호 발송 → 코드 입력 → 인증 확인 흐름으로 구현했고,
인증 전에는 가입 버튼을 비활성화해 제출을 막았다. 이메일을 바꾸면 인증 상태가 초기화되도록 했다.
- 회원가입 페이지에 비밀번호 확인 입력란을 추가했다. 비밀번호와 값이 다르면 제출 전에 막고 안내 문구를 보여준다. `passwordConfirm`은 프론트 검증에만 쓰고 API에는 `password`만 그대로 보낸다.
- 내정보 페이지에 비밀번호 변경 카드를 추가했다. 새 비밀번호/확인 값이 다르면 API 호출 전에 차단하고,
성공하면 토큰을 지우고 `/login`으로 이동시킨다.
- 비밀번호 변경 완료 토스트 메시지("비밀번호를 변경했어요." / "다시 로그인해주세요.")에 줄바꿈을 추가했다.
`.toast`에 `white-space: pre-line`을 전역으로 넣었는데, 다른 한 줄짜리 토스트에는 영향이 없다.

**검증**

- 로컬 SMTP 계정이 placeholder라 실제 이메일 발송은 안 됐지만, 에러 메시지가 정상적으로 뜨는 것까지 확인했다.
- DB에 인증 코드를 직접 넣어서 인증 확인 → 회원가입 성공 → 로그인 페이지 이동까지 전체 흐름을 실제로 검증했다.
- 비밀번호 변경도 실제 계정으로 새 비밀번호 로그인 성공, 기존 비밀번호 로그인 거부까지 확인했다.
- 로그아웃 클릭 시 `/login`으로 정상 이동하는 것을 확인했다.
- 토스트 줄바꿈은 DOM에 직접 렌더링해서 두 줄로 나뉘는 것을 스크린샷으로 확인했다.

## 건드린 파일

- `frontend/src/app/globals.css` (+1/-0)
- `frontend/src/app/my-profile/page.tsx` (+119/-0)
- `frontend/src/app/signup/page.module.css` (+5/-0)
- `frontend/src/app/signup/page.tsx` (+137/-13)
- `frontend/src/components/Header.tsx` (+5/-1)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 요청서에 적힌 이메일 인증 API 경로(`/api/auth/email/send`, `/api/auth/email/verify`)는 실제 백엔드 구현과 달라서,
실제 존재하는 엔드포인트(`/api/auth/email-verifications`, `/api/auth/email-verifications/confirm`)로 맞춰서 연동했다.
- 비밀번호 변경 API의 새 비밀번호 규칙(10~64자, 영문·숫자·특수문자 모두 포함)이 회원가입 비밀번호 규칙(8~20자, 영문·숫자만)과
달라서, 프론트 검증 정규식도 각각 다르게 맞췄다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/167
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/166
