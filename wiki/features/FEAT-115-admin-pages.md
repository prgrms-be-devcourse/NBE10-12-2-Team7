---
id: FEAT-115
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-03
related: []
tags: [admin, globals, frontend]
pr: 115
---

## 무엇을 / 왜

와이어프레임 디자인을 기반으로 관리자 페이지 8종(운영 대시보드, 회원 목록/상세, 상품 목록/상세, 신고 목록/상세, 댓글 목록)을
새로 구현하고 실제 백엔드 admin API에 연동한다.

## 어떻게 (구현 요약)

- [x] admin/layout.tsx: 다크 사이드바 레이아웃, JWT role 클레임 기반 관리자 권한 가드(비로그인/권한없음 상태 분리 + 재시도 버튼), 로그아웃
- [x] 운영 대시보드: 통계 5종 조회, 최근 가입 회원·최근 등록 상품을 회원/상품 목록에서 직접 정렬해 표시
- [x] 회원 목록/상세: 검색·상태 필터(클라이언트 처리), 상태 변경(정상/정지/탈퇴)
- [x] 상품 목록/상세: 검색·거래상태·숨김여부 필터(클라이언트 처리), 숨김 처리, 삭제 처리, 판매자 정보(닉네임·상태) 표시
- [x] 신고 목록/상세: 처리상태·신고유형 필터(클라이언트 처리), 처리 상태 변경, 연계 조치(상품 신고 시 대상 상품 숨김 + 판매자 정지, 사용자 신고 시 대상 회원 정지)
- [x] 댓글 목록: 내용·상품ID 필터(클라이언트 처리), 삭제 처리
- [x] 회원/상품별 신고 수는 백엔드 응답에 필드가 없어 신고 목록(GET /api/admin/reports)을 별도로 불러와 targetMemberId/targetProductId 기준으로 클라이언트에서 집계
- [x] 판매자·신고자·댓글 작성자의 닉네임은 관리자 전용 조회 API(GET /api/admin/members/{id})로 실시간 조회해 표시 — 고객용 화면에서는 불가능했던 실명 표시가 관리자 화면에서는 가능
- [x] lib/memberStatus.ts, lib/reportStatus.ts 추가, lib/auth.ts에 getCurrentRole/isAdmin 추가
- [x] Header: /admin/** 경로에서는 고객용 헤더를 숨김 (관리자 레이아웃이 자체 상단바를 가짐)
- [x] globals.css: 기존에 없던 .btn.ghost, .btn:disabled 전역 스타일 추가 (여러 곳에서 재사용)

**검증**

※ 로컬 Docker 이슈로 실제 백엔드를 띄운 테스트는 진행하지 못해, 브라우저에서 mock 응답으로 아래 항목을 확인했다. 백엔드에는 관리자 계정이 시드되어 있다(admin@dongnemarket.com / admin1234!).
* 비로그인 / 일반회원 로그인 시 각각 다른 접근 차단 문구와 재시도 버튼이 정상 표시되는지
* 대시보드 통계 5종과 최근 가입 회원·등록 상품 정렬이 올바른지
* 회원 목록 검색·상태 필터, 회원 상세의 신고 누적 수 집계, 상태 변경(정지→정상)이 실시간 반영되는지
* 상품 목록/상세의 숨김 버튼이 이미 숨김 처리된 상품에서 정확히 비활성화되는지, 판매자 정보·카테고리명이 정확히 조회되는지
* 신고 상세의 신고자·대상 닉네임 조회, 연계 조치 버튼(대상 상품 숨김/대상 회원 정지)이 올바른 대상 ID로 요청을 보내는지
* 댓글 목록 삭제 처리 후 "삭제됨" 태그와 버튼 비활성화가 정확히 반영되는지
* npx tsc --noEmit, npm run build 모두 정상 통과
* 실제 DB 저장까지 이어지는 성공 케이스는 머지 전 팀원 로컬 확인 필요

## 건드린 파일

- `frontend/src/app/admin/admin.module.css` (+102/-0)
- `frontend/src/app/admin/comments/page.tsx` (+121/-0)
- `frontend/src/app/admin/dashboard/page.tsx` (+103/-0)
- `frontend/src/app/admin/layout.tsx` (+104/-0)
- `frontend/src/app/admin/members/[id]/page.tsx` (+137/-0)
- `frontend/src/app/admin/members/page.tsx` (+124/-0)
- `frontend/src/app/admin/page.tsx` (+5/-0)
- `frontend/src/app/admin/products/[id]/page.tsx` (+182/-0)
- `frontend/src/app/admin/products/page.tsx` (+172/-0)
- `frontend/src/app/admin/reports/[id]/page.tsx` (+223/-0)
- `frontend/src/app/admin/reports/page.tsx` (+111/-0)
- `frontend/src/app/globals.css` (+6/-0)
- `frontend/src/components/Header.tsx` (+4/-0)
- `frontend/src/lib/auth.ts` (+21/-5)
- `frontend/src/lib/memberStatus.ts` (+7/-0)
- … 외 1개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

* 목록 조회 API(회원/상품/신고/댓글) 전부 검색·필터 쿼리 파라미터를 지원하지 않아 클라이언트에서 필터링한다.
* 상품 숨김(PATCH .../hidden)은 되돌릴 수 없는 단방향 처리이다(숨김 해제 API 없음) — 와이어프레임의 숨김/노출 토글 UI는 실제로 만들 수 없어 숨김 버튼만 두고 이미 숨김인 경우 비활성화했다.
* 회원 상세의 "전화번호", "관리자 메모" 필드는 백엔드에 대응 데이터가 없어 제외했다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/115
- 이슈: 없음
