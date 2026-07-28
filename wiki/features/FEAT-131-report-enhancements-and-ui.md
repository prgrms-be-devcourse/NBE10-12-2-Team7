---
id: FEAT-131
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-05
related: []
tags: [global, report, admin, globals, my-reports, page]
pr: 131
---

## 무엇을 / 왜

신고 도메인 기능 5가지를 추가/개선하고, MarketON 소개 페이지 신규 제작, 로그인/로그아웃 연동, 알림 점 표시 수정,
사용자·관리자 페이지 UI를 보완했다.

## 어떻게 (구현 요약)

- `POST /api/reports/evidence-image` — 증빙 이미지 업로드 (multipart)
- `GET /api/reports/evidence-image/{filename}` — 증빙 이미지 조회
- `DELETE /api/members/me/reports/{reportId}` — 신고 취소
- 기존 신고 생성/조회 API는 시그니처 유지, 내부적으로 중복 방지·N+1 개선만 반영
- 관리자 AI 챗봇(`POST /api/admin/ai/chat`)·최근 신고 목록(`GET /api/admin/reports`)은 기존 API를 그대로 사용

**검증**

- `./gradlew test` 전체 통과 (단위/통합 테스트, H2 기반)
- 동시성 통합 테스트(`ReportConcurrencyIntegrationTest`)는 Testcontainers 기반으로 작성했으나
  로컬에 Docker가 없어 실행 미확인 — CI/Docker 환경에서 확인 필요
- 프론트엔드는 로컬 프리뷰로 홈페이지 애니메이션, 로그인/로그아웃, 신고 취소·증빙 이미지 업로드 플로우 직접 확인

## 건드린 파일

- `.gitignore` (+4/-0)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+5/-0)
- `backend/src/main/java/com/dongnemarket/report/controller/ReportController.java` (+45/-0)
- `backend/src/main/java/com/dongnemarket/report/dto/EvidenceImageUploadResponse.java` (+18/-0)
- `backend/src/main/java/com/dongnemarket/report/dto/MemberReportCreateRequest.java` (+4/-0)
- `backend/src/main/java/com/dongnemarket/report/dto/MyReportResponse.java` (+3/-0)
- `backend/src/main/java/com/dongnemarket/report/dto/ProductReportCreateRequest.java` (+4/-0)
- `backend/src/main/java/com/dongnemarket/report/dto/ReportResponse.java` (+3/-0)
- `backend/src/main/java/com/dongnemarket/report/entity/Report.java` (+25/-3)
- `backend/src/main/java/com/dongnemarket/report/repository/ReportRepository.java` (+7/-0)
- `backend/src/main/java/com/dongnemarket/report/service/EvidenceImageStorageService.java` (+87/-0)
- `backend/src/main/java/com/dongnemarket/report/service/ReportService.java` (+54/-5)
- `backend/src/main/resources/application-local.yml` (+5/-0)
- `backend/src/main/resources/application.yml` (+10/-0)
- `backend/src/test/java/com/dongnemarket/report/ReportServiceTest.java` (+129/-0)
- … 외 23개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 담당 패키지(report) 외에 admin, products, signup, 공용 컴포넌트(Header, auth.ts) 파일도 함께 수정했다.
- 관리자 AI 챗봇/대시보드 위젯 추가, 로그인/로그아웃 연동은 팀 논의 후 명시적으로 요청받아 진행한 작업이다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/131
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/130
