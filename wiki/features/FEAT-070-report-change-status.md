---
id: FEAT-070
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-06-29
related: []
tags: [report, backend]
pr: 70
---

## 무엇을 / 왜

관리자 신고 상태 변경 API(`PATCH /api/admin/reports/{reportId}/status`) 구현에 필요한 `Report` 엔티티 메서드를 추가한다.

## 어떻게 (구현 요약)

- [x] `Report.changeStatus(ReportStatus status)` 메서드 추가

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/report/entity/Report.java` (+5/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/70
- 이슈: 없음
