---
id: FEAT-012
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-06-25
related: []
tags: [global, report, backend]
pr: 12
---

## 무엇을 / 왜

[Report] 상품 신고 API Entity·Enum·DTO 골격 구현

## 어떻게 (구현 요약)

- Report 도메인 Entity·Enum·DTO 골격 구현
- ReportType / ReportReason / ReportStatus Enum 구현
- Report Entity 구현 (ERD 기준 컬럼명 적용)
- ProductReportCreateRequest / MemberReportCreateRequest Request DTO 구현
- ReportResponse / MyReportResponse Response DTO 구현
- DUPLICATE_REPORT ErrorCode 추가 (REPORT 영역)

**검증**

- 빌드 성공 (./gradlew compileJava 통과)
- 본 PR은 골격 구현 단계로 서버 기동 테스트는 단위 2 완료 후 진행 예정

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+1/-0)
- `backend/src/main/java/com/dongnemarket/report/dto/MemberReportCreateRequest.java` (+19/-0)
- `backend/src/main/java/com/dongnemarket/report/dto/MyReportResponse.java` (+40/-0)
- `backend/src/main/java/com/dongnemarket/report/dto/ProductReportCreateRequest.java` (+19/-0)
- `backend/src/main/java/com/dongnemarket/report/dto/ReportResponse.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/report/entity/Report.java` (+69/-0)
- `backend/src/main/java/com/dongnemarket/report/entity/ReportReason.java` (+9/-0)
- `backend/src/main/java/com/dongnemarket/report/entity/ReportStatus.java` (+8/-0)
- `backend/src/main/java/com/dongnemarket/report/entity/ReportType.java` (+6/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- Member / Product Entity가 아직 없어 @ManyToOne 연관관계 대신 Long ID + @Column(name=...) 으로 처리함
  → 해당 Entity가 develop에 머지되면 단위 2 전후로 연관관계 전환 논의 필요

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/12
- 이슈: 없음
