---
id: BUG-164
type: bug
status: done
author: NextWave-Dev-Space
date: 2026-07-06
related: []
tags: [globals, frontend]
pr: 164
---

## 증상

사용자가 시스템에서 설정한 라이트/다크 모드가 MarketON 접속 시 잘 반영되는지 테스트하던 중 발견한 버그를 수정했다.
`header` 등 대부분의 UI는 테마 전환 시 정상적으로 색이 바뀌는데,
`<body>` 요소 자체만 라이트 모드 배경/글자색에 고정되어 갱신되지 않는 문제였다.

## 원인

`globals.css`의 `body` 규칙에 걸려 있던 `transition: color .35s, background-color .35s`가 원인이었다.
- 테마는 `<html data-theme="dark">` 속성을 토글하고,
`[data-theme="dark"] { --bg: #000; ... }`로 CSS 변수를 바꾸는 방식으로 동작한다.
- `body { background-color: var(--bg); color: var(--text); transition: ...; }`처럼
**조상 요소의 속성 변경으로 인해 상속된 CSS 변수 값이 바뀌는 경우**,
해당 값을 참조하는 속성에 `transition`이 걸려 있으면 브라우저가 전환을 제대로 커밋하지 못하고
이전 값에 멈추는 현상이 있었다.
- 실제로 브라우저에서 `body.style.setProperty('transition', 'none', 'important')`를 강제 적용하자
즉시 올바른 값(검정 배경)으로 정상화되는 것을 확인해 원인을 특정했다.
- 같은 `var(--bg)`를 쓰지만 `transition`이 없는 `header`는 애초에 이 문제를 겪지 않고 정상 동작하고 있었다.

## 해결 방법

- `body`의 `transition: color .35s ease, background-color .35s ease;` 선언 제거

## 건드린 파일

- `frontend/src/app/globals.css` (+0/-1)

## 재발 방지

- 시스템 다크 모드 상태로 최초 접속 → `body` 배경 `rgb(0, 0, 0)`, 글자색 `rgb(245, 242, 244)`로 정상 전환 확인
- 시스템 라이트 모드 상태로 최초 접속 → `body` 배경 `rgb(255, 255, 255)`로 정상 유지 확인
- 헤더의 수동 다크모드 토글 버튼 클릭 → `body` 배경도 즉시 정상 전환 확인
- 스크린샷으로 전체 페이지 다크모드 렌더링 최종 확인

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/164
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/163
