---
id: FEAT-230
type: feature
status: done
author: jomin4
date: 2026-07-27
related: []
tags: [에스크로, 안심결제, 거래, 상태머신, BE]
pr: 230
---

## 무엇을 / 왜
C2C 중고거래 MarketON에 당근 "안심결제"식 **에스크로 거래**를 도입했다. 플랫폼이 대금을 잠시 **보관(예치)** 했다가 구매자가 **구매확정**하면 판매자에게 풀고, 취소하면 구매자에게 되돌리는 흐름. 처음엔 토스페이먼츠 단순결제로 설계했으나, C2C에선 PG 결제가 "플랫폼 가맹점 매입"일 뿐 판매자에게 정산되지 않음을 확인하고 방향을 에스크로로 틀었다. 실제 송금·정산은 전자금융업 라이선스가 필요해(당근도 금융위 등록) **상태 전이로 모사(가정)** 한다.

## 어떻게 (구현 요약)
"돈을 붙잡은 상태 1개 + 두 출구"만 남긴 **v0 최소 뼈대**. 단일 `Escrow` 애그리거트 + 상태 3개 `IN_ESCROW → DONE / CANCELED`.
- **Unit 1** — 도메인 뼈대: `Escrow`(`BaseTimeEntity` 상속, product/buyer/seller = `@ManyToOne(LAZY)`, amount 스냅샷), `EscrowStatus` enum, `EscrowRepository`(`existsByProductIdAndStatus`), `ErrorCode`에 `ESCROW_001~007`. 상태 전이는 `Escrow.confirm()/cancel()` 엔티티 메서드에 가드(예치 상태만) 캡슐화.
- **Unit 2** — 거래 생성·조회: `EscrowService.create()`(검증 존재·삭제·숨김·본인상품·ON_SALE·중복 → 상품가 스냅샷 예치 → 상품 `RESERVED`), `get()`. `EscrowController` `POST /api/escrows`(201) · `GET /api/escrows/{id}`. DTO `from()` 변환.
- **Unit 3** — 구매확정·취소: `confirm()`(→DONE + `Product.complete()`), `cancel()`(→CANCELED + 상품 `ON_SALE` 복귀), `findOwnedEscrow`로 본인 검증 공통화. `POST /{id}/confirm`·`/{id}/cancel`. 상태 변경 + 상품 상태 연동을 한 `@Transactional`로.
- **테스트** — 단위: `EscrowTest` 상태 전이 6개(허용 2 + 금지 4, POJO). 통합: `EscrowControllerTest` E2E 8개(@SpringBootTest+MockMvc+JWT, 여정 3 + 보안·검증 5).

## 건드린 파일
- `backend/src/main/java/com/dongnemarket/escrow/entity/{Escrow,EscrowStatus}.java`
- `backend/src/main/java/com/dongnemarket/escrow/repository/EscrowRepository.java`
- `backend/src/main/java/com/dongnemarket/escrow/dto/{EscrowCreateRequest,EscrowResponse}.java`
- `backend/src/main/java/com/dongnemarket/escrow/service/EscrowService.java`
- `backend/src/main/java/com/dongnemarket/escrow/controller/EscrowController.java`
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (ESCROW_001~007 블록)
- `backend/src/test/java/com/dongnemarket/escrow/entity/EscrowTest.java`
- `backend/src/test/java/com/dongnemarket/escrow/controller/EscrowControllerTest.java`

## 결정과 트레이드오프
- **토스 단순결제 폐기 → 에스크로** — C2C에서 판매자에게 실제 돈이 가려면 정산/라이선스가 필요. v0는 "상태 흐름을 정확히 모사"에 집중하고 실송금은 가정.
- **단일 `Escrow` + 상태 3개(뼈대 우선)** — `Order`/`Payment` 분리 안 함. 정산 분리(CONFIRMED/SETTLED)는 성장 경로로.
- **상태 규칙은 엔티티에 캡슐화** — 서비스가 `setStatus` 하지 않고 `confirm()/cancel()`만 호출, 종료 거래 재처리를 엔티티가 예외로 방어.
- **네이밍 `escrow`** — product의 `TradeStatus`·한상민 `TRADE_*`와 충돌 회피(`ESCROW_*`, `/api/escrows`).
- **결제(예치)는 v0 가정** — `POST /api/escrows` 호출 = 예치로 간주. 실 토스 연동은 증분.
- **Product `RESERVED` 재활용** — 예치 시 상품이 RESERVED가 되어, 중복 거래는 `ESCROW_ALREADY_EXISTS`(409) 전에 `PRODUCT_NOT_ON_SALE`(400)에서 먼저 차단됨. 즉 `ESCROW_ALREADY_EXISTS`는 비정상 상태 전용 방어 코드.

## 남은 이슈 / 후속 작업
- 성장 경로: ①DONE→CONFIRMED+SETTLED 분리 ②자동확정 스케줄러(72h `@Scheduled`) ③REQUESTED→ACCEPTED 판매자 수락 ④DELIVERED 배송추적 ⑤토스 실연동 ⑥수수료(2%)·분쟁조정·안심보상
- docs/api·docs/architecture 반영 여부 팀장 확인 대기
- FE 상품상세 '결제하기' 버튼·'판매완료' 상태 연동 (frontend 워크트리)

## 링크
- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/230
- Notion 설계·구현·테스트: https://app.notion.com/p/39dcd6a1d02381ef8ab4e72996d4fb05
- 브랜치: `feat/escrow`
