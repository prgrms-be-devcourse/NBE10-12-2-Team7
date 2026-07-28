---
id: FEAT-101
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-03
related: []
tags: [favicon, favorites, globals, layout, login, my-products]
pr: 101
---

## 무엇을 / 왜

프론트엔드 전 페이지(회원가입/로그인/신고/내 정보/상품/카테고리/찜/댓글)를 실제 백엔드 API와 연동한다.

## 어떻게 (구현 요약)

* 회원가입/로그인 폼을 SignupRequest/LoginRequest 실제 필드에 맞춰 정리 (전화번호, 이메일 중복확인 UI 제거), 로그인 성공 시 JWT 저장
* 상품 신고 / 사용자 신고(진입점 신규 추가) / 내 신고 내역을 실제 API에 연동, ReportReason enum에 맞춰 신고 사유 값 수정
* 내 정보 조회/수정/탈퇴(GET·PATCH·DELETE /api/members/me) 연동
* 상품 목록/상세/등록/수정/내 상품 목록을 실제 API에 연동, 백엔드에 없는 필드(사진 업로드, 등록 폼 거래상태 선택, 상대시간 표시) 제거
* 카테고리 목록 API 연동 (하드코딩된 카테고리 목록 제거)
* 관심 상품 등록/조회/해제 연동
* 댓글 작성/조회/수정/삭제(CRUD 전체) 연동, 작성자 본인만 수정·삭제 가능하도록 클라이언트에서도 권한 체크
* next.config.ts에 /api/* → 백엔드(localhost:8080) rewrites 프록시 추가 (CORS 불필요)
* lib/auth.ts(토큰 저장·조회, JWT sub 클레임 디코드), lib/tradeStatus.ts, lib/reportReasons.ts 공용 유틸 추가
* 홈페이지(/)를 /products로 리다이렉트 처리

**검증**

※ 로컬 Docker 이슈로 실제 MySQL·백엔드를 띄운 Postman 테스트는 진행하지 못했다. 대신 브라우저에서 아래 항목을 확인했다.
 * 각 요청의 URL·HTTP 메서드·요청 바디 필드명이 컨트롤러/DTO와 일치하는지 백엔드 소스와 전수 대조
 * next.config.ts 프록시를 통해 요청이 실제로 localhost:8080으로 전달되는 경로 (네트워크 탭 확인)
 * mock 응답으로 각 페이지의 로딩/에러/비로그인/성공 상태 분기 렌더링 확인
 * 백엔드 미기동(500) 시 데모 fallback 없이 정직하게 에러 메시지가 표시되는지 확인
 * 회원가입 → 로그인 → 신고 작성 → 내 신고 내역, 상품 등록 → 수정 → 삭제, 댓글 작성 → 수정 → 삭제 등 주요 플로우를 화면 단위로 클릭 재현
 * 백엔드 미실행 상태이므로 실제 DB 저장까지 이어지는 성공 케이스는 머지 전 팀원 로컬 확인 필요

## 건드린 파일

- `frontend/.gitignore` (+41/-0)
- `frontend/README.md` (+36/-0)
- `frontend/eslint.config.mjs` (+18/-0)
- `frontend/next.config.ts` (+16/-0)
- `frontend/package-lock.json` (+6704/-0)
- `frontend/package.json` (+26/-0)
- `frontend/postcss.config.mjs` (+7/-0)
- `frontend/src/app/favicon.ico` (+0/-0)
- `frontend/src/app/favorites/page.module.css` (+130/-0)
- `frontend/src/app/favorites/page.tsx` (+174/-0)
- `frontend/src/app/globals.css` (+148/-0)
- `frontend/src/app/layout.tsx` (+27/-0)
- `frontend/src/app/login/page.module.css` (+82/-0)
- `frontend/src/app/login/page.tsx` (+124/-0)
- `frontend/src/app/my-products/page.module.css` (+151/-0)
- … 외 23개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

* 인증 헤더는 로그인 시 저장한 accessToken을 Authorization: Bearer로 프론트 fetch에 직접 첨부
* GET /api/products/search, GET /api/categories/{categoryId}/products는 미사용 — 상품 목록을 한 번에 조회 후 클라이언트에서 검색어·카테고리로 필터링
* 판매자·댓글 작성자 이름은 회원 #id로 표시 — GET /api/products/{id} 호출 시 조회수가 증가하는 부작용이 있어 목록/댓글 표시용으로는 쓸 수 없고, 닉네임 조회용 공개 API도 없음 (부작용 없는 요약 조회 API 추가는 별도 이슈로 분리 제안)
* "채팅으로 거래하기" 버튼은 UI만 존재 — 백엔드에 채팅 도메인이 없어 클릭 시 안내 메시지만 표시
* /api/admin/** 전체는 프론트엔드에 관리자 화면이 없어 연동 범위에서 제외

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/101
- 이슈: 없음
