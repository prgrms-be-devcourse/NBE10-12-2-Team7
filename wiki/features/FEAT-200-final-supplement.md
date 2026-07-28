---
id: FEAT-200
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-08
related: []
tags: [favorite, admin, chat, favorites, my-products, backend]
pr: 200
---

## 무엇을 / 왜

최종 보완 — 관리자/채팅/내 상품/관심상품 UI 개선 및 관심상품 카테고리 필터 추가

## 어떻게 (구현 요약)

- 관리자 페이지 로그아웃 버튼 배경색을 사이드바 배경색과 통일
- 채팅방/채팅 목록 페이지 디자인 개선 (아바타, 날짜 구분선, 메시지 그룹핑, 상태 배지, 안읽음 강조 등)
- 내 상품 목록 요약 통계 타일에 상태별 구분 색상 및 강조선 적용
- 관심상품 페이지에 통계 카드, 카테고리 필터 탭 추가
- 관심상품 카드 전체를 상세 페이지 링크로 연결 (기존엔 텍스트 영역만 클릭 가능했음)
- 관심상품 API에 categoryId 노출 (FavoriteProductSummary, FavoriteRepository)

**검증**

- [x] 빌드 성공 / 서버 정상 기동
- [x] 담당 API 정상 동작 (Postman 확인)
- [x] develop 최신 반영 / 충돌 해결
- [x] 공통 응답 형식(ApiResponse / ErrorResponse) 준수

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/favorite/dto/FavoriteProductSummary.java` (+5/-1)
- `backend/src/main/java/com/dongnemarket/favorite/repository/FavoriteRepository.java` (+4/-3)
- `frontend/src/app/admin/admin.module.css` (+6/-0)
- `frontend/src/app/admin/layout.tsx` (+1/-1)
- `frontend/src/app/chat/[roomId]/page.module.css` (+82/-35)
- `frontend/src/app/chat/[roomId]/page.tsx` (+62/-19)
- `frontend/src/app/chat/page.module.css` (+45/-14)
- `frontend/src/app/chat/page.tsx` (+18/-4)
- `frontend/src/app/favorites/page.module.css` (+65/-0)
- `frontend/src/app/favorites/page.tsx` (+96/-7)
- `frontend/src/app/my-products/page.module.css` (+12/-1)
- `frontend/src/app/my-products/page.tsx` (+4/-4)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/200
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/199
