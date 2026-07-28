---
id: FEAT-109
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-03
related: []
tags: [my-products, products, frontend]
pr: 109
---

## 무엇을 / 왜

백엔드에 새로 추가된 상품 이미지(ProductImage) 필드를 프론트엔드 상품 등록/수정/목록/상세 화면에 연동한다.

## 어떻게 (구현 요약)

- [x] ProductForm: 파일 업로드 대신 이미지 URL 입력 UI(최대 5장, 실시간 미리보기, 대표 이미지 지정 버튼)로 이미지 필드 구현
- [x] POST·PATCH /api/products 요청 바디에 imageUrls/thumbnailIndex 포함, 빈 URL은 제출 전 필터링
- [x] 수정 폼 진입 시 기존 imageUrls/thumbnailUrl로 이미지 목록·대표 이미지 프리필
- [x] 상품 목록(products/page.tsx), 내 상품(my-products/page.tsx) 카드: thumbnailUrl 있으면 실제 이미지, 없으면 기존 placeholder
- [x] 상품 상세(products/[id]/page.tsx): 갤러리 메인 이미지·썸네일 스트립을 실제 imageUrls로 렌더링

**검증**

※ 로컬 Docker 이슈로 실제 백엔드를 띄운 테스트는 진행하지 못해, 브라우저에서 mock 응답으로 아래 항목을 확인했다.
 * 등록 폼에서 이미지 URL 입력 시 실시간 미리보기가 뜨는지, 대표 이미지 지정·필드 추가/삭제가 정상 동작하는지
 * 수정 폼 진입 시 기존 이미지 목록과 대표 이미지가 정확히 프리필되는지
 * 상품 목록/내 상품 카드에서 썸네일 유무에 따라 실제 이미지/placeholder가 올바르게 갈리는지
 * 상품 상세에서 갤러리 메인 이미지·썸네일 스트립이 실제 이미지로 렌더링되고, 썸네일 클릭 시 메인 이미지가 전환되는지
 * 실제 백엔드 연동 시의 성공 케이스(등록 → 목록/상세 노출)는 머지 전 팀원 로컬 확인 필요

## 건드린 파일

- `frontend/src/app/my-products/page.module.css` (+6/-1)
- `frontend/src/app/my-products/page.tsx` (+4/-0)
- `frontend/src/app/products/[id]/page.module.css` (+12/-3)
- `frontend/src/app/products/[id]/page.tsx` (+20/-10)
- `frontend/src/app/products/page.module.css` (+5/-1)
- `frontend/src/app/products/page.tsx` (+6/-1)
- `frontend/src/components/ProductForm.module.css` (+28/-30)
- `frontend/src/components/ProductForm.tsx` (+88/-1)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

* imageUrls는 이미 호스팅된 이미지 URL 문자열을 받는 방식이다 — 실제 파일 업로드(멀티파트) 엔드포인트는 이번 백엔드 변경에 포함되지 않아, "내 컴퓨터에서 사진 선택" UI는 아직 만들 수 없다.
* 관심 상품(favorites) 목록 카드에는 썸네일을 반영하지 못했다 — FavoriteProductSummary DTO에 thumbnailUrl 필드가 없다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/109
- 이슈: 없음
