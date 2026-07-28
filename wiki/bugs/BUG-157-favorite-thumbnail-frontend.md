---
id: BUG-157
type: bug
status: done
author: NextWave-Dev-Space
date: 2026-07-06
related: []
tags: [favorites, frontend]
pr: 157
---

## 증상

권건우님이 관심 상품 이미지 표시 여부를 테스트해달라고 요청하셔서 확인한 결과,
백엔드는 `thumbnailUrl`을 정상 반환하는데 프론트에서 이를 전혀 사용하지 않고
항상 고정 플레이스홀더("상품 이미지")만 보여주고 있었다. 이를 실제 이미지로 표시되도록 연동했다.

## 원인

- PR 본문에 원인 기록 없음.

## 해결 방법

- `FavoriteProduct.product` 타입에 `thumbnailUrl: string | null` 필드 추가
- 썸네일 영역을 products 목록 페이지와 동일한 패턴으로 변경: `thumbnailUrl`이 있으면 `<img>`로 실제 이미지를,
없으면 기존 플레이스홀더("상품 이미지")를 표시
- `.thumbImg` CSS 규칙 추가 (products 페이지의 기존 스타일과 동일하게 `object-fit: cover`로 정사각형 썸네일에 꽉 채워 표시)

## 건드린 파일

- `frontend/src/app/favorites/page.module.css` (+4/-0)
- `frontend/src/app/favorites/page.tsx` (+6/-1)

## 재발 방지

- 로컬 백엔드 연동 후 실제 계정으로 두 케이스 모두 확인
  - 실제 썸네일이 있는 상품(예: 앨범 상품) → 이미지 정상 렌더링
  - 썸네일이 없는 상품(예: 시드 데이터 상품) → 기존 플레이스홀더 그대로 유지, 깨지지 않음
- `npx tsc --noEmit` 통과
- 테스트를 위해 추가했던 관심 상품 2건은 종료 후 원상복구함

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/157
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/156
