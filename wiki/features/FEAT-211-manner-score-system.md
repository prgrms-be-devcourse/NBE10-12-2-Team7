---
id: FEAT-211
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-23
related: []
tags: [admin, global, manner, product, backend]
pr: 211
---

## 무엇을 / 왜

매너온도 시스템 백엔드를 구현했다.
Closes #210

## 어떻게 (구현 요약)

- MannerScore/MannerScoreHistory/MannerRating 엔티티 및 리포지토리 추가
- MannerScoreService: 별점 반영, 신고 확정/기각 반영, 거래 완료 보너스, 시간 경과 회복(36.5 상한),
  계정 자동 정지, 관리자 모니터링 조회 로직
- MannerRatingService: 거래 완료 + 실제 거래 참여자(구매자) + 중복 방지 검증 후 별점 등록
- ReportStatusChangedEvent, ProductCompletedEvent 신규 추가 후, 각각 신고 상태 변경 시점(AdminReportService)과
  상품 거래완료 시점(ProductService)에 발행하도록 훅 연결
- MannerScoreEventListener: 두 이벤트를 AFTER_COMMIT + REQUIRES_NEW로 구독해 매너온도에 반영(기존 NotificationEventHandler와 동일한 패턴)
- MannerScoreRecoveryScheduler: 매일 새벽 3시, 최근 30일 정상거래가 있고
  최근 30일 내 페널티가 없는 회원을 대상으로 점수 회복
- 관리자용 AdminMannerController/Service/Response 추가 (저신뢰 회원 목록 조회)
- ErrorCode에 MANNER_001~003 추가, Product에 completedAt 필드 추가

**검증**

- `./gradlew compileJava` 빌드 성공 확인함
- 프론트엔드 `tsc --noEmit`, `eslint` 통과 확인함
- 로컬 서버(백엔드 + MySQL + Redis)를 기동하고 실제 계정으로 로그인해서 4개 API를 각각 호출 검증했다. 전부 정상 동작함.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/DongneMarketApplication.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/admin/controller/AdminMannerController.java` (+33/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminMannerResponse.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminMannerService.java` (+30/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminReportService.java` (+11/-2)
- `backend/src/main/java/com/dongnemarket/global/common/event/ProductCompletedEvent.java` (+13/-0)
- `backend/src/main/java/com/dongnemarket/global/common/event/ReportStatusChangedEvent.java` (+16/-0)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+5/-0)
- `backend/src/main/java/com/dongnemarket/global/security/SecurityConfig.java` (+1/-0)
- `backend/src/main/java/com/dongnemarket/manner/controller/MannerRatingController.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/manner/controller/MannerScoreController.java` (+40/-0)
- `backend/src/main/java/com/dongnemarket/manner/dto/MannerRatingCreateRequest.java` (+26/-0)
- `backend/src/main/java/com/dongnemarket/manner/dto/MannerRatingResponse.java` (+32/-0)
- `backend/src/main/java/com/dongnemarket/manner/dto/MannerScoreHistoryResponse.java` (+42/-0)
- `backend/src/main/java/com/dongnemarket/manner/dto/MannerScoreResponse.java` (+24/-0)
- … 외 14개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- report/product 도메인 담당자에게 이벤트 발행 훅 추가 부분(AdminReportService, Product, ProductService) 리뷰 요청
- 신뢰도 가중 신고 처리(우선순위 조정), 상품 노출 우선순위 하락 로직은 별도 후속 PR에서 진행 예정

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/211
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/210
