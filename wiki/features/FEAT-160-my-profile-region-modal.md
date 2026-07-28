---
id: FEAT-160
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-06
related: []
tags: [my-profile, frontend]
pr: 160
---

## 무엇을 / 왜

한상민님이 요청한 지역 데이터 노출 테스트(내정보/상품목록 페이지에서 전국 시/군/구가 잘 보이는지) 과정에서,
두 페이지의 동네 선택 UI 방식이 서로 다름을 확인했다. 데이터 자체는 두 곳 모두 정상 노출됐지만,
사용자 경험을 우선시하기 위해 내정보 페이지도 상품목록 페이지와 동일한 UI로 통일했다.

## 어떻게 (구현 요약)

- 기존 `<select>` 드롭다운 방식을 상품목록 페이지와 동일한 검색 가능 모달 UI(핀 아이콘 + 검색창 + 스크롤 목록)로 교체
- 대표/삭제 칩 UI, 명시적 "동네 저장" 버튼 등 내정보 페이지 고유의 기존 동작은 변경 없이 유지
(상품목록 페이지처럼 선택 즉시 자동 저장되는 방식이 아니라, 여전히 "동네 저장" 버튼을 눌러야 반영됨)
- 모달 관련 CSS(`.modalOverlay`, `.modal`, `.modalHead`, `.searchWrap`, `.resultsList` 등)를
상품목록 페이지의 기존 스타일과 동일하게 추가

**검증**

- 실제 로그인 계정(user01)으로 확인
  - "동네 추가" 버튼 → 검색 모달 정상 오픈
  - "강남" 검색 → "서울 강남구"만 정확히 필터링
  - 클릭 시 "대표" 칩으로 정상 추가되고 모달 자동 닫힘
  - 저장 전 상태에서는 DB에 반영되지 않는 기존 동작 유지 확인
- `npx tsc --noEmit` 통과

## 건드린 파일

- `frontend/src/app/my-profile/page.module.css` (+55/-11)
- `frontend/src/app/my-profile/page.tsx` (+60/-23)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 담당 패키지(report) 외 파일(member/my-profile)을 수정했다 — 내가 주도한 UI 통일 작업이다.
- 별도로, 이 작업 전 상품목록 페이지에서 일시적으로 크래시가 발견됐었는데
확인 결과 코드 버그가 아니라 로컬 백엔드 프로세스가 최신 코드 반영 전 상태였던 것이 원인이었고,
재기동 후 정상 동작을 확인했다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/160
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/159
