---
id: FEAT-085
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-01
related: [BUG-088, FEAT-089]
tags: [report, backend]
pr: 85
---

## 무엇을 / 왜

신고(Report) 도메인의 코드 품질 개선을 위한 리팩토링 및 테스트 코드 정리 및 작성을 완료했다.

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `report` 도메인 — MyReportResponse(DTO, 수정), Report(엔티티, 수정), ReportRepository(리포지토리, 수정), ReportService(서비스, 수정)
- 테스트 — 3개 파일 (+216줄)

**검증**

- verify(never().save(...)) 제거 — 예외 발생 시 save가 호출되지 않는 것은 자명
- 단순 존재 확인 테스트 3개 제거 (reporterNotFound, productNotFound, targetNotFound)
- Mock을 JPA 엔티티 기반 메서드에 맞게 수정

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/report/dto/MyReportResponse.java` (+2/-2)
- `backend/src/main/java/com/dongnemarket/report/entity/Report.java` (+28/-19)
- `backend/src/main/java/com/dongnemarket/report/repository/ReportRepository.java` (+5/-3)
- `backend/src/main/java/com/dongnemarket/report/service/ReportService.java` (+12/-17)
- `backend/src/test/java/com/dongnemarket/report/ReportServiceTest.java` (+28/-71)
- `backend/src/test/java/com/dongnemarket/report/integration/ReportControllerIntegrationTest.java` (+171/-0)
- `backend/src/test/resources/sql/report-scenario.sql` (+17/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

통합 테스트 전체 실행(./gradlew integrationTest) 시 다른 팀원 통합 테스트와 Testcontainers 컨테이너 충돌이 발생한다.
단, 신고 통합 테스트 단독 실행 시에는 정상 통과한다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/85
- 이슈: 없음
