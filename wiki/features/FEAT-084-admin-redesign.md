---
id: FEAT-084
type: feature
status: done
author: jomin4
date: 2026-07-01
related: []
tags: [admin, backend, test]
pr: 84
---

## 무엇을 / 왜

Admin 도메인의 기존 테스트를 전면 제거하고, 서비스 단위(S) 계층을 실무 방식으로 재설계했습니다.

## 어떻게 (구현 요약)

**재작성 (서비스 단위 · Mockito)**
| 파일 | 케이스 |
| --- | --- |
| AdminMemberServiceTest | trim 성공 + 파싱 실패(파라미터 6종) + nullRequest + NOT_FOUND (9) |
| AdminReportServiceTest | 위와 동일 구조 (9) |
| AdminProductServiceTest | hide/delete 성공 + NOT_FOUND (3) |
| AdminCommentServiceTest | delete 성공 + NOT_FOUND (2) |

- 파싱 실패는 `@ParameterizedTest`로 통합(null·공백·오타·대소문자 불일치), NOT_FOUND는 ErrorCode당 1회.
- 상태 전이(`changeStatus`의 deletedAt 등)는 엔티티 로직이라 기존 엔티티 테스트가 커버 → 중복 제거.

**제거**
- 컨트롤러 슬라이스 5종, AdminAccountInitializerTest, AdminMemberIntegrationTest, AdminDashboardServiceTest
- Dashboard 집계·목록 조회·보안(401/403)은 **후속 통합(I) 계층**에서 유즈케이스 기반으로 검증 예정

**검증**

서비스 단위 테스트 **23케이스 전부 green** (`./gradlew test --tests "com.dongnemarket.admin.service.*"`).

## 건드린 파일

- `backend/src/test/java/com/dongnemarket/admin/controller/AdminCommentControllerTest.java` (+0/-111)
- `backend/src/test/java/com/dongnemarket/admin/controller/AdminDashboardControllerTest.java` (+0/-81)
- `backend/src/test/java/com/dongnemarket/admin/controller/AdminMemberControllerTest.java` (+0/-182)
- `backend/src/test/java/com/dongnemarket/admin/controller/AdminProductControllerTest.java` (+0/-187)
- `backend/src/test/java/com/dongnemarket/admin/controller/AdminReportControllerTest.java` (+0/-140)
- `backend/src/test/java/com/dongnemarket/admin/init/AdminAccountInitializerTest.java` (+0/-51)
- `backend/src/test/java/com/dongnemarket/admin/integration/AdminMemberIntegrationTest.java` (+0/-52)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminCommentServiceTest.java` (+30/-38)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminDashboardServiceTest.java` (+0/-54)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminMemberServiceTest.java` (+68/-85)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminProductServiceTest.java` (+41/-93)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminReportServiceTest.java` (+67/-71)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 이 PR은 **서비스 단위(S) 계층까지**입니다. 통합(I, Testcontainers MySQL) 계층은 후속 PR.
- 알려진 갭(N1 관리자 잠금·N3 대시보드 집계 등)은 "현재 동작 고정" 방침으로 통합 계층에서 케이스화 예정.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/84
- 이슈: 없음
