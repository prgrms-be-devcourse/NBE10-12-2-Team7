---
id: FEAT-040
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-06-26
related: []
tags: [report, backend, docs]
pr: 40
---

## 무엇을 / 왜

상품 신고 API의 Repository, Service, Controller를 구현하고 단위 테스트를 작성한다.

## 어떻게 (구현 요약)

- `ReportRepository` 구현: 중복 신고 검증 및 신고 내역 조회 쿼리 메서드 추가
- `ReportService` 구현: 상품 신고, 회원 신고, 내 신고 내역 조회 비즈니스 로직
- `ReportController` 구현: 상품 신고 / 회원 신고 / 내 신고 내역 조회 엔드포인트
- `ReportServiceTest`: 8개 단위 테스트 작성 (전체 통과)
- `docs/postman/report-product.md`: 신고 API Postman 테스트 시나리오 문서

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/report/controller/ReportController.java` (+57/-0)
- `backend/src/main/java/com/dongnemarket/report/repository/ReportRepository.java` (+15/-0)
- `backend/src/main/java/com/dongnemarket/report/service/ReportService.java` (+90/-0)
- `backend/src/test/java/com/dongnemarket/report/ReportServiceTest.java` (+232/-0)
- `docs/postman/report-product.md` (+336/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 신고자 인증은 `@AuthenticationPrincipal Long reporterId` (SecurityContext에서 memberId 추출)

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/40
- 이슈: 없음
