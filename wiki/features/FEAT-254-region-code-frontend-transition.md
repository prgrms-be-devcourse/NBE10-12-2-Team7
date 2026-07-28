---
id: FEAT-254
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-27
related: [FEAT-248, FEAT-250]
tags: [admin, my-profile, products, frontend]
pr: 254
---

## 무엇을 / 왜

한상민님 지역 계층 전환 백엔드 계약(PR #248, #250)에 맞춰, 프론트엔드의 지역 관련 API 연동을 전부 regionCode 기반으로 전환했다.

Closes #253

## 어떻게 (구현 요약)

**계단식 지역 선택 컴포넌트**
- `components/RegionCascadeSelect.tsx`(+`.module.css`) 신규 작성
   — 시/도→시/군/구→동 계단식 select, 하위 지역 존재 여부로 단계를 늘려가는 방식이라
  세종처럼 중간 단계가 없는 지역도 level 하드코딩 없이 자동으로 처리됨

**상품 등록/수정**
- `ProductForm.tsx`: `region` 전송 제거하고 `regionCode`로 전송.
   수정 화면 진입 시 `regionCode`+`regionFullName`으로 계단식 select를 역으로 복원
   (regionCode 단건 조회 API가 없어 fullName을 공백 분리해 각 단계 displayName과 매칭하는 방식으로 해결)

**상품 목록/검색 필터**
- `products/page.tsx`: `regions` 파라미터 → `regionCodes`로 전환(기존 `regions`는 백엔드에서 이미 무시되고 있던 상태였음),
   내 동네 설정 모달의 동네 추가를 계단식 선택으로 교체

**내 동네 설정**
- `my-profile.tsx`: PUT/GET `regionCodes`/`{regionCode,regionFullName,sortOrder,active}` 계약 반영,
   동네 추가 모달도 계단식 선택으로 교체

**표시 전용 전환**
- `(my-marketon)/favorites`, `(my-marketon)/my-products`, `admin/products`(목록/상세), `products/[id]`,
  `TradeHistorySection`: `region` → `regionFullName`

**검증**

- `npm run lint` + `npm run build` 통과
- 실 서버로 실제 시나리오 재현: 상품 등록(계단식 선택으로 서울특별시→강남구→역삼동 선택 후 정상 등록),
  수정 화면 재진입 시 계단식 select가 regionCode 기준으로 정확히 복원되는 것 확인,
  목록 페이지 지역 필터가 `regionCodes` 쿼리로 정상 전송되는 것 확인,
  내 동네 설정에서 동네 추가(계단식 선택 → 자동 추가) 확인

## 건드린 파일

- `frontend/src/app/(my-marketon)/favorites/page.tsx` (+2/-2)
- `frontend/src/app/(my-marketon)/my-products/page.tsx` (+2/-2)
- `frontend/src/app/admin/products/[id]/page.tsx` (+2/-2)
- `frontend/src/app/admin/products/page.tsx` (+1/-1)
- `frontend/src/app/my-profile/page.tsx` (+33/-63)
- `frontend/src/app/products/[id]/page.tsx` (+2/-2)
- `frontend/src/app/products/page.tsx` (+41/-72)
- `frontend/src/components/ProductForm.tsx` (+15/-22)
- `frontend/src/components/RegionCascadeSelect.module.css` (+21/-0)
- `frontend/src/components/RegionCascadeSelect.tsx` (+136/-0)
- `frontend/src/components/TradeHistorySection.tsx` (+2/-2)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

한상민님 최근 커밋을 전수 조사했는데(PR #248, #250까지), 이번 PR로 관련 API 전환이 전부 커버된다.
추가로 필요한 화면은 없었다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/254
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/253
