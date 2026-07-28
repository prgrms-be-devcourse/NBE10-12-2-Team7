---
id: FEAT-215
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-23
related: []
tags: [globals, frontend]
pr: 215
---

## 무엇을 / 왜

내상품/관심상품 화면을 "나의 마켓온" 허브로 통합하고,
MarketON 로고에 그라데이션 애니메이션을 추가했다.
Closes #214

## 어떻게 (구현 요약)

- Next.js 라우트 그룹 `(my-marketon)`으로 `my-products`, `favorites`를 감쌌다.
  `git mv`로 폴더만 이동했고 두 페이지의 내용은 전혀 수정하지 않았다(커밋에 `rename ... (100%)`로 확인 가능).
   라우트 그룹은 URL에 영향을 주지 않아 `/my-products`, `/favorites` 주소도 그대로다.
- `MyMarketOnTabs` 공유 탭바 컴포넌트 추가 — 알약형 세그먼트 컨트롤 스타일, 탭 전환 시 핑크색 배경이 부드럽게 슬라이드,
   페이지 컨텐츠 폭에 맞춰 전체 너비로 확장
- `(my-marketon)/layout.tsx` — 탭바 + 기존 페이지를 그대로 감싸기만 함
- `Header.tsx` — "내상품"/"관심상품" 두 내비게이션 항목을 "나의 마켓온" 하나로 병합, 두 경로 모두에서 active 표시되도록 처리
- `globals.css` — `.logo span`("ON")에 그라데이션 애니메이션 추가.
   상품목록 배너와 같은 색 계열(핑크 + 골드)을 사용하되 텍스트에서 선명하게 보이도록 톤 조정,
   9초 주기(약 4초 노출·5초 원래색)로 서서히 나타났다 사라짐, 물결 효과(`mo-wave`) 동반, 라이트/다크 모드 동일 색상으로 고정

**검증**

- `tsc --noEmit`, `eslint` 통과 확인함(신규 에러 없음)
- 실제 로그인해서 두 탭 전환, 상품 수정/삭제, 거래상태 변경, 관심상품 담기/조회까지 정상 동작 확인
- 로고 그라데이션 애니메이션의 키프레임·색상 정지점을 브라우저에서 직접 계산값으로 확인,
  상품등록/내정보 등 여러 페이지에서 동일하게 적용되는 것 확인
- API 변경 사항 없음(순수 프론트엔드 구조·스타일 변경이라 Postman 테스트 대상 없음)

## 건드린 파일

- `frontend/src/app/(my-marketon)/favorites/page.module.css` (+0/-0)
- `frontend/src/app/(my-marketon)/favorites/page.tsx` (+0/-0)
- `frontend/src/app/(my-marketon)/layout.tsx` (+10/-0)
- `frontend/src/app/(my-marketon)/my-products/page.module.css` (+0/-0)
- `frontend/src/app/(my-marketon)/my-products/page.tsx` (+0/-0)
- `frontend/src/app/globals.css` (+18/-1)
- `frontend/src/components/Header.tsx` (+3/-4)
- `frontend/src/components/MyMarketOnTabs.module.css` (+54/-0)
- `frontend/src/components/MyMarketOnTabs.tsx` (+34/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 탭바 아래에 각 페이지 자체의 원래 제목(예: "내 상품 목록")이 한 번 더 보이는 이중 타이틀 형태가 남아있다.
  팀원 코드를 안 건드리기 위한 트레이드오프이다.
- `my-products`/`favorites` 개편이라 한상민님/권건우님 리뷰 요청

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/215
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/214
