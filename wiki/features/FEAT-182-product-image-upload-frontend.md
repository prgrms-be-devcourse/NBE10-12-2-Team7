---
id: FEAT-182
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-07
related: [FEAT-176]
tags: [frontend]
pr: 182
---

## 무엇을 / 왜

상품 등록/수정 이미지를 파일 업로드 방식으로 전환

## 어떻게 (구현 요약)

- 상품 등록/수정 화면의 이미지 입력을 URL 직접 입력에서 파일 업로드 방식으로 바꿨다.
- 파일을 선택하면 바로 POST /api/products/images로 업로드하고,
응답으로 받은 imageUrls를 상품 등록/수정 요청에 그대로 실어 보낸다.
- 대표 이미지(thumbnailIndex)는 최종 imageUrls 배열 기준으로 계산한다.

**검증**

- [x] 빌드 성공 / 서버 정상 기동 (npm run lint / npx tsc --noEmit / npm run build 모두 통과)
- [x] 담당 API 정상 동작 (브라우저에서 실제 업로드→등록→조회까지 확인)
- [x] develop 최신 반영 / 충돌 해결
- [ ] 공통 응답 형식(ApiResponse / ErrorResponse) 준수 — 프론트 전용이라 해당 없음

## 건드린 파일

- `frontend/src/components/ProductForm.module.css` (+3/-0)
- `frontend/src/components/ProductForm.tsx` (+106/-46)

## 결정과 트레이드오프

- **[한상민 팀원 후속 작업 필요]** `GET /api/products/images/{filename}`가 아직 `permitAll`이 아니라서, 상품 목록/상세 페이지(plain `<img>` 태그 사용)에서는 새로 업로드한 이미지가 로그인 여부와 무관하게 깨져 보인다. `SecurityConfig`에 `GET /api/products/images/** permitAll()` 추가가 필요하다. 이 설정이 들어오면 프론트 쪽 추가 수정 없이 바로 정상 표시된다.

## 남은 이슈 / 후속 작업

- **[한상민 팀원 후속 작업 필요]** `GET /api/products/images/{filename}`가 아직 `permitAll`이 아니라서, 상품 목록/상세 페이지(plain `<img>` 태그 사용)에서는 새로 업로드한 이미지가 로그인 여부와 무관하게 깨져 보인다. `SecurityConfig`에 `GET /api/products/images/** permitAll()` 추가가 필요하다. 이 설정이 들어오면 프론트 쪽 추가 수정 없이 바로 정상 표시된다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/182
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/181
