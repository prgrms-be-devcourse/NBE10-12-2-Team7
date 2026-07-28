---
id: FEAT-002
type: feature
status: done
author: jomin4
date: 2026-06-25
related: []
tags: [global, backend, docs]
pr: 2
---

> ⚠️ **이 문서는 PR 본문이 비어 있어 제목·변경 파일에서 역구성했다.** 의도·트레이드오프는 추정하지 않고 공란으로 두었다.

## 무엇을 / 왜

Feature/global monorepo

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `기타` 도메인 — DongneMarketApplication(기타, 신규)
- `global` 도메인 — BaseTimeEntity(기타, 수정), JpaAuditingConfig(설정, 신규), SwaggerConfig(설정, 수정), BusinessException(예외, 수정), ErrorCode(예외, 신규), GlobalExceptionHandler(예외, 수정), ApiResponse(기타, 수정), ErrorResponse(기타, 수정), SecurityConfig(기타, 수정), JwtAccessDeniedHandler(기타, 수정), JwtAuthenticationEntryPoint(기타, 수정), JwtAuthenticationFilter(기타, 신규), JwtTokenProvider(기타, 신규)
- 리소스 — `application.yml` (+0)
- 문서 — `docs/` 1개, `docs/ai/` 1개, `docs/dev-log/` 1개
- 테스트 — 3개 파일 (+0줄)

## 건드린 파일

- `backend/.gitattributes` (+0/-0)
- `backend/build.gradle` (+0/-6)
- `backend/docker-compose.yml` (+0/-0)
- `backend/gradle/wrapper/gradle-wrapper.jar` (+0/-0)
- `backend/gradle/wrapper/gradle-wrapper.properties` (+0/-0)
- `backend/gradlew` (+0/-0)
- `backend/gradlew.bat` (+0/-0)
- `backend/settings.gradle` (+0/-0)
- `backend/src/main/java/com/dongnemarket/DongneMarketApplication.java` (+0/-0)
- `backend/src/main/java/com/dongnemarket/global/common/BaseTimeEntity.java` (+8/-2)
- `backend/src/main/java/com/dongnemarket/global/config/JpaAuditingConfig.java` (+0/-0)
- `backend/src/main/java/com/dongnemarket/global/config/SwaggerConfig.java` (+2/-2)
- `backend/src/main/java/com/dongnemarket/global/exception/BusinessException.java` (+4/-3)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+0/-0)
- `backend/src/main/java/com/dongnemarket/global/exception/GlobalExceptionHandler.java` (+4/-2)
- … 외 14개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/2
- 이슈: 없음
