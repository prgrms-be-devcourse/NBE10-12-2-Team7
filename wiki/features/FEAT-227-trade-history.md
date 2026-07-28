---
id: FEAT-227
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-24
related: []
tags: [trade, my-profile, backend, frontend]
pr: 227
---

## 무엇을 / 왜

거래내역(판매내역/구매내역/월별 통계) 기능을 백엔드·프론트엔드 모두 구현했다.
기존 역할 분담 어디에도 속하지 않는 크로스도메인 성격이라 신규 `trade` 패키지로 만들었다.

Closes #226

## 어떻게 (구현 요약)

**백엔드**
- `trade` 패키지 신설(dto/service/controller). 별도 거래 엔티티 없이
   기존 `ProductRepository`/`ChatRoomRepository`의 공개 조회 메서드만 재사용해서, 상품·채팅 도메인 코드는 전혀 수정하지 않았다.
- 판매내역: 내 상품 중 거래완료(COMPLETED)분 기준
- 구매내역: 스키마상 구매자를 식별할 수 있는 유일한 경로인 채팅방(내가 buyer인 방) 중 상품이 거래완료된 것 기준
   — 채팅 없이 완료 처리된 거래는 구매내역에 잡히지 않는 구조적 한계가 있음
- 월별 통계: 위 두 데이터를 월(yyyy-MM) 단위로 집계, 최신 달이 먼저 오도록 정렬

**프론트엔드**
- `TradeHistorySection` 컴포넌트 신규 작성, 내정보(my-profile) 페이지 프로필 요약 카드 바로 아래에 배치
- 판매내역/구매내역/월별 통계 3개 탭으로 구성

**검증**

- 단위 테스트(TradeServiceTest) 6건 작성 — 완료 여부 필터링, 정렬, 월별 집계 검증. 전체 테스트 스위트(`./gradlew test`) 통과
- 실 서버 기동 후 curl로 라이브 검증: 판매자·구매자 계정으로 상품 등록 → 채팅방 생성 → 거래완료까지 실제로 진행시켜
  세 API 응답 모두 확인
- 브라우저로 세 탭 화면 모두 직접 확인 (판매내역/구매내역/월별 통계) — 검증 중 월별 통계에서 "구매 0건"인데
   "나눔"으로 잘못 표시되던 버그(0원 처리를 상품가 표시 로직과 혼용)를 발견해 즉시 수정함

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/trade/controller/TradeController.java` (+47/-0)
- `backend/src/main/java/com/dongnemarket/trade/dto/MonthlyTradeStatsResponse.java` (+33/-0)
- `backend/src/main/java/com/dongnemarket/trade/dto/TradePurchaseResponse.java` (+49/-0)
- `backend/src/main/java/com/dongnemarket/trade/dto/TradeSaleResponse.java` (+45/-0)
- `backend/src/main/java/com/dongnemarket/trade/service/TradeService.java` (+101/-0)
- `backend/src/test/java/com/dongnemarket/trade/service/TradeServiceTest.java` (+195/-0)
- `frontend/src/app/my-profile/page.tsx` (+4/-0)
- `frontend/src/components/TradeHistorySection.module.css` (+138/-0)
- `frontend/src/components/TradeHistorySection.tsx` (+171/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 채팅 없이 오프라인으로 거래 완료 처리된 상품은 구매내역에 반영되지 않는 스키마상 한계가 있음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/227
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/226
