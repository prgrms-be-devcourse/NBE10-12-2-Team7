---
id: FEAT-217
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-23
related: []
tags: [admin, global, manner, product, report, backend]
pr: 217
---

## 무엇을 / 왜

신뢰도(매너온도) 연동 기능 중 신고 처리 우선순위, 신고 상세 조회, API 보안 강화(rate limiting)를 구현했다.
Closes #216

매너온도 백엔드 PR(#(매너온도 백엔드 PR 번호 입력))이 먼저 머지되어야 하는 브랜치다(그 위에서 분기했다).

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `기타` 도메인 — DongneMarketApplication(기타, 신규)
- `admin` 도메인 — AdminMannerController(컨트롤러, 신규), AdminMannerResponse(DTO, 신규), AdminMannerService(서비스, 신규), AdminReportService(서비스, 수정)
- `global` 도메인 — ProductCompletedEvent(기타, 신규), ReportStatusChangedEvent(기타, 신규), ErrorCode(예외, 수정), RateLimitFilter(기타, 신규), RateLimitFilterConfig(기타, 신규), SecurityConfig(기타, 신규)
- `manner` 도메인 — MannerRatingController(컨트롤러, 신규), MannerScoreController(컨트롤러, 신규), MannerRatingCreateRequest(DTO, 신규), MannerRatingResponse(DTO, 신규), MannerScoreHistoryResponse(DTO, 신규), MannerScoreResponse(DTO, 신규), MannerRating(엔티티, 신규), MannerScore(엔티티, 신규), MannerScoreChangeReason(엔티티, 신규), MannerScoreHistory(엔티티, 신규), MannerScoreEventListener(기타, 신규), MannerRatingRepository(리포지토리, 신규), MannerScoreHistoryRepository(리포지토리, 신규), MannerScoreRepository(리포지토리, 신규), MannerScoreRecoveryScheduler(기타, 신규), MannerRatingService(서비스, 신규), MannerScoreService(서비스, 신규)
- `product` 도메인 — Product(엔티티, 수정), ProductService(서비스, 신규)
- `report` 도메인 — ReportController(컨트롤러, 신규), MyReportResponse(DTO, 신규), Report(엔티티, 신규), ReportService(서비스, 신규)
- 리소스 — `application.yml` (+6)
- 추가된 API 표면 — `@RequestMapping("/api/admin/manner-scores")`, `@GetMapping`, `@PostMapping("/api/manner/ratings")`, `@GetMapping("/api/members/{memberId}/manner-score")`, `@GetMapping("/api/members/me/manner-score/history")`, `@GetMapping("/api/members/me/reports/{reportId}")`
- 테스트 — 1개 파일 (+8줄)

**검증**

- 로컬 서버(백엔드 + MySQL + Redis) 기동 후 각 기능 curl 개별 호출로 검증
- **신뢰도 우선순위 정렬**: 테스트용 신고를 확정 처리해 특정 회원 온도를 36.5→36.0으로 낮춘 뒤,
  그 회원 대상의 RECEIVED 신고가 날짜순 최하위에서 목록 최상위로 올라오는 것 확인
- **신고 상세 조회**: 본인 조회(200, content 포함) / 타인 조회 시도(403) / 존재하지 않는 신고(404) / 미인증(401) 전부 확인
- **rate limit 필터**: 한도를 10회/10초로 낮춰서 11번째 요청부터 429 발생, 시간 경과에 따라 슬라이딩 윈도우답게
  점진적으로 재개방되는 것까지 확인
- `./gradlew test` 전체 538개+α 테스트 통과 확인함

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/DongneMarketApplication.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/admin/controller/AdminMannerController.java` (+33/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminMannerResponse.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminMannerService.java` (+30/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminReportService.java` (+48/-4)
- `backend/src/main/java/com/dongnemarket/global/common/event/ProductCompletedEvent.java` (+13/-0)
- `backend/src/main/java/com/dongnemarket/global/common/event/ReportStatusChangedEvent.java` (+16/-0)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+7/-1)
- `backend/src/main/java/com/dongnemarket/global/filter/RateLimitFilter.java` (+82/-0)
- `backend/src/main/java/com/dongnemarket/global/filter/RateLimitFilterConfig.java` (+40/-0)
- `backend/src/main/java/com/dongnemarket/global/security/SecurityConfig.java` (+1/-0)
- `backend/src/main/java/com/dongnemarket/manner/controller/MannerRatingController.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/manner/controller/MannerScoreController.java` (+40/-0)
- `backend/src/main/java/com/dongnemarket/manner/dto/MannerRatingCreateRequest.java` (+26/-0)
- `backend/src/main/java/com/dongnemarket/manner/dto/MannerRatingResponse.java` (+32/-0)
- … 외 21개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 상품 노출 우선순위 하락(신뢰도 연동 기능 나머지 절반)은 상품 목록/검색 핵심 정렬 로직을 건드리는 작업이라 보류함
   — 한상민님과 논의 후 별도 PR로 진행 예정
- rate limit 기본값(10초당 60회)이 실제 서비스 트래픽 패턴에 적절한지 팀 논의 필요

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/217
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/216
