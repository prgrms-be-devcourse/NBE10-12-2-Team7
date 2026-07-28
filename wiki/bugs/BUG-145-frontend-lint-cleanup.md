---
id: BUG-145
type: bug
status: done
author: NextWave-Dev-Space
date: 2026-07-06
related: []
tags: [admin, products, frontend]
pr: 145
---

## 증상

CI 파이프라인의 `frontend-check` 잡이 `react-hooks/set-state-in-effect` 에러(15건)로 실패하던 문제를 해결했다.
이 규칙은 `useEffect` 본문에서 동기적으로 `setState`를 호출하는 패턴
(예: 토큰 가드 `if (!token) { setStatus('error'); return }`)을 에러로 막는데,
React 19 기준 불필요한 연쇄 렌더를 유발하는 안티패턴이기 때문이다.

## 원인

- PR 본문에 원인 기록 없음.

## 해결 방법

- **관리자 페이지 8곳** (`admin/comments`, `admin/dashboard`, `admin/members`, `admin/members/[id]`, `admin/products`, `admin/products/[id]`, `admin/reports`, `admin/reports/[id]`): 토큰 부재 시 `setStatus('error')`를 effect
안에서 호출하던 부분을, `useState` 초기값을 lazy하게 계산(`useState(() => getAccessToken() ? 'loading' : 'error')`)
하도록 변경하고 effect 내부는 `if (!token) return`으로 단순화. 렌더 시점에 이미 동기적으로 알 수 있는 값을
effect에서 다시 설정하지 않도록 한 것으로, 동작은 기존과 동일하다.
- **Header.tsx**: 로그인 상태(`loggedIn`)도 동일한 방식으로 lazy 초기화.
다크 테마 초기화는 `localStorage`/`matchMedia`가 SSR에서 접근 불가능해 반드시 마운트 후(effect)에 읽어야 하므로,
해당 한 줄만 `eslint-disable-next-line`으로 처리하고 사유를 주석으로 남겼다.
- **products/page.tsx**: `activeRegion` 변경 시 재조회 로딩 표시(`setStatus('loading')`)는
이 파일이 담당자(한상민 님)의 커서 페이지네이션 전환 작업 대상이라, 구조를 바꾸는 대신 해당 줄만 동일하게
`eslint-disable-next-line` 처리했다.

## 건드린 파일

- `frontend/src/app/admin/comments/page.tsx` (+2/-2)
- `frontend/src/app/admin/dashboard/page.tsx` (+2/-2)
- `frontend/src/app/admin/members/[id]/page.tsx` (+2/-2)
- `frontend/src/app/admin/members/page.tsx` (+2/-2)
- `frontend/src/app/admin/products/[id]/page.tsx` (+2/-2)
- `frontend/src/app/admin/products/page.tsx` (+2/-2)
- `frontend/src/app/admin/reports/[id]/page.tsx` (+2/-2)
- `frontend/src/app/admin/reports/page.tsx` (+2/-2)
- `frontend/src/app/products/page.tsx` (+1/-0)
- `frontend/src/components/Header.tsx` (+2/-2)

## 재발 방지

```
cd frontend
npm run lint # ✖ 0 errors, 9 warnings
npm run build # 성공
```
- 로컬 프리뷰로 홈페이지, 관리자 대시보드 미인증 가드 화면(`관리자 로그인이 필요해요`) 정상 렌더 확인,
콘솔에 하이드레이션 경고 없음.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/145
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/144
