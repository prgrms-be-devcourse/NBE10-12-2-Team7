---
id: FEAT-169
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-06
related: []
tags: [find-password, login, password-reset, frontend]
pr: 169
---

## 무엇을 / 왜

김대연 님이 요청한 비밀번호 찾기/재설정 화면을 구현했다. 로그인 페이지에 링크를 추가하고,
이메일 입력 화면과 토큰 기반 재설정 화면을 새로 만들었다.

## 어떻게 (구현 요약)

- 로그인 페이지에 "비밀번호 찾기" 링크를 추가했다.
- `/find-password` 페이지를 추가했다. 이메일을 입력하면 재설정 메일 발송을 요청하고,
계정 존재 여부와 무관하게 항상 같은 안내 메시지를 보여준다.
- `/password-reset` 페이지를 추가했다. `?token=` 쿼리 값을 읽어서 새 비밀번호/확인 입력을 받고 confirm API로 보낸다.
새 비밀번호 정책은 회원가입·비밀번호 변경과 동일하게 10~64자, 영문+숫자+특수문자 포함, 공백 불가로 검증한다.
토큰이 없으면 유효하지 않은 링크 안내와 재요청 링크를 보여준다.
- `useSearchParams` 사용 때문에 생기는 빌드 경고를 피하려고 `/password-reset` 페이지를 Suspense로 감쌌다.
- 로그인 페이지에서 "자동 로그인" 체크박스와 "비밀번호 찾기" 링크를 한 줄에 배치하다가 발견한 레이아웃 버그도 같이 고쳤다.
`flex-shrink` 처리가 빠져서 링크 텍스트가 세로로 한 글자씩 줄바꿈되고 있었는데,
`flex-shrink: 0`과 `white-space: nowrap`을 넣어서 해결했다.

**검증**

- 로컬 SMTP 계정이 없어서 실제 메일 발송은 안 됐지만, 에러 메시지가 제대로 뜨는 것까지 확인했다.
- DB에 재설정 토큰(SHA-256 해시)을 직접 넣어서 confirm 단계까지 실제로 검증했다.
새 비밀번호로 로그인 성공, 기존 비밀번호는 거부, 토큰이 1회용으로 소진되는 것,
토큰 없이 접근했을 때 안내 화면이 뜨는 것까지 전부 확인했다.
- 로그인 페이지 링크 레이아웃 버그도 고친 뒤 정상 크기로 렌더링되는 것을 확인했다.

## 건드린 파일

- `frontend/src/app/find-password/page.module.css` (+85/-0)
- `frontend/src/app/find-password/page.tsx` (+101/-0)
- `frontend/src/app/login/page.module.css` (+3/-1)
- `frontend/src/app/login/page.tsx` (+10/-7)
- `frontend/src/app/password-reset/page.module.css` (+85/-0)
- `frontend/src/app/password-reset/page.tsx` (+150/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 담당 패키지(report) 외 파일(login, find-password, password-reset)을 수정했다 — 팀원이 직접 요청한 작업이다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/169
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/168
