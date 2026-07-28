---
id: FEAT-066
type: feature
status: done
author: jomin4
date: 2026-06-29
related: []
tags: [admin, backend]
pr: 66
---

## 무엇을 / 왜

 관리자 api 기능구현

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `admin` 도메인 — AdminCommentController(컨트롤러, 신규), AdminDashboardController(컨트롤러, 신규), AdminProductController(컨트롤러, 신규), AdminReportController(컨트롤러, 신규), AdminCommentResponse(DTO, 신규), AdminDashboardResponse(DTO, 신규), AdminProductResponse(DTO, 신규), AdminReportResponse(DTO, 신규), AdminCommentRepository(리포지토리, 신규), AdminProductRepository(리포지토리, 신규), AdminReportRepository(리포지토리, 신규), AdminCommentService(서비스, 신규), AdminDashboardService(서비스, 신규), AdminProductService(서비스, 신규), AdminReportService(서비스, 신규)
- 추가된 API 표면 — `@RequestMapping("/api/admin/comments")`, `@GetMapping`, `@DeleteMapping("/{commentId}")`, `@RequestMapping("/api/admin/dashboard")`, `@RequestMapping("/api/admin/products")`, `@GetMapping("/{productId}")`, `@PatchMapping("/{productId}/hidden")`, `@DeleteMapping("/{productId}")`, `@RequestMapping("/api/admin/reports")`, `@GetMapping("/{reportId}")`
- 테스트 — 8개 파일 (+821줄)

## 건드린 파일

- `backend/bin/main/application.yml` (+40/-0)
- `backend/bin/test/application-test.yml` (+18/-0)
- `backend/src/main/java/com/dongnemarket/admin/controller/AdminCommentController.java` (+41/-0)
- `backend/src/main/java/com/dongnemarket/admin/controller/AdminDashboardController.java` (+30/-0)
- `backend/src/main/java/com/dongnemarket/admin/controller/AdminProductController.java` (+56/-0)
- `backend/src/main/java/com/dongnemarket/admin/controller/AdminReportController.java` (+40/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminCommentResponse.java` (+46/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminDashboardResponse.java` (+33/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminProductResponse.java` (+72/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminReportResponse.java` (+50/-0)
- `backend/src/main/java/com/dongnemarket/admin/repository/AdminCommentRepository.java` (+16/-0)
- `backend/src/main/java/com/dongnemarket/admin/repository/AdminProductRepository.java` (+12/-0)
- `backend/src/main/java/com/dongnemarket/admin/repository/AdminReportRepository.java` (+15/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminCommentService.java` (+37/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminDashboardService.java` (+41/-0)
- … 외 10개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/66
- 이슈: 없음
