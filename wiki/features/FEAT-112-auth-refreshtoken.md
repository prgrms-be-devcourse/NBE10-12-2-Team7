---
id: FEAT-112
type: feature
status: done
author: horangnabi97
date: 2026-07-03
related: []
tags: [auth, global, backend, docs]
pr: 112
---

## 무엇을 / 왜

- Refresh Token 기반 Access Token 재발급 기능을 추가했습니다.
- Access Token 만료 15분(900초) / Refresh Token 만료 7일(604800초) 정책을 적용했습니다.
- 담당 도메인(Auth/Member) 범위 내 작업이며, 이번 브랜치의 `global/security`, `global/exception` 변경은 팀장과 사전 협의된 예외 사항입니다.

## 어떻게 (구현 요약)

- 로그인(`POST /api/auth/login`) 성공 시 `accessToken` + `refreshToken`을 함께 발급
- `refresh_tokens` 테이블 추가 (`RefreshToken` 엔티티, `RefreshTokenRepository`)
- Refresh Token은 회원당 1개만 저장 — 재로그인 시 기존 토큰을 교체(단일 세션 정책, Rotation 없음)
- `POST /api/auth/reissue` 추가 — 인증 헤더 없이 Refresh Token만으로 새 Access Token 발급
- 만료 / 위조 / DB 미존재 / 저장값 불일치 Refresh Token에 대한 예외 처리 (`ErrorCode` AUTH_005~007)
- **Refresh Token으로 보호 API에 접근하는 것을 차단** — JWT에 `type=access`/`type=refresh` 클레임을 추가하고, `JwtTokenProvider.getAuthentication()`이 `type=access`가 아니면 인증을 거부하도록 수정
- **Access Token으로 `/api/auth/reissue`를 호출하는 것을 차단** — `RefreshTokenService.validateAndGetMemberId()`에서 `type=refresh`가 아니면 `INVALID_REFRESH_TOKEN` 반환
- **SUSPENDED/DELETED 회원의 재발급 차단** — `login()`에 있던 회원 상태 검증을 `reissue()`에도 동일하게 적용(`AuthService.validateActiveStatus()`로 공통화)

**검증**

| 시나리오 | 결과 | 검증 테스트 |
|---|---|---|
| Access Token으로 보호 API 접근 | 200 성공 | 기존 `SecurityPolicyTest`, `MemberControllerTest` 등 |
| Refresh Token으로 보호 API 접근 | 401 `INVALID_TOKEN` | `SecurityPolicyTest#protectedApi_withRefreshToken_returns401InvalidToken` |
| Refresh Token으로 `/api/auth/reissue` 호출 | 200 성공 (Access Token만 재발급, Refresh Token은 동일 값 유지) | `AuthControllerTest#reissue_success` |
| Access Token으로 `/api/auth/reissue` 호출 | 401 `INVALID_REFRESH_TOKEN` | `AuthControllerTest#reissue_withAccessToken_returnsInvalidRefreshToken`, `AuthServiceTest#reissue_accessTokenPresented_throwsInvalidRefreshToken`, `RefreshTokenServiceTest#validateAndGetMemberId_accessTokenPresented_throwsInvalidRefreshToken` |
| SUSPENDED/DELETED 회원의 재발급 시도 | 각각 403/400 (`SUSPENDED_MEMBER`/`DELETED_MEMBER`) | `AuthServiceTest#reissue_suspendedMember_throwsException`, `#reissue_deletedMember_throwsException` |

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/auth/controller/AuthController.java` (+10/-1)
- `backend/src/main/java/com/dongnemarket/auth/dto/LoginResponse.java` (+9/-3)
- `backend/src/main/java/com/dongnemarket/auth/dto/ReissueRequest.java` (+20/-0)
- `backend/src/main/java/com/dongnemarket/auth/dto/TokenResponse.java` (+24/-0)
- `backend/src/main/java/com/dongnemarket/auth/entity/RefreshToken.java` (+70/-0)
- `backend/src/main/java/com/dongnemarket/auth/repository/RefreshTokenRepository.java` (+11/-0)
- `backend/src/main/java/com/dongnemarket/auth/service/AuthService.java` (+31/-7)
- `backend/src/main/java/com/dongnemarket/auth/service/RefreshTokenService.java` (+66/-0)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+3/-0)
- `backend/src/main/java/com/dongnemarket/global/security/SecurityConfig.java` (+1/-1)
- `backend/src/main/java/com/dongnemarket/global/security/jwt/JwtTokenProvider.java` (+43/-2)
- `backend/src/main/resources/application.yml` (+2/-1)
- `backend/src/test/java/com/dongnemarket/auth/controller/AuthControllerTest.java` (+69/-2)
- `backend/src/test/java/com/dongnemarket/auth/entity/RefreshTokenTest.java` (+57/-0)
- `backend/src/test/java/com/dongnemarket/auth/service/AuthServiceTest.java` (+140/-4)
- … 외 3개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 이번 PR로 Refresh Token을 저장하는 `refresh_tokens` 테이블이 새로 추가되었습니다.
- **운영 DB에 이 테이블이 아직 없으면, 배포 후 애플리케이션이 스키마 검증 단계에서 기동에 실패할 수 있습니다.**
- 따라서 배포 전에 운영 DB에 `refresh_tokens` 테이블을 먼저 생성해야 합니다. 필요한 DDL은 `docs/postman/auth-refresh-token.md`의 "운영 배포 참고사항" 절에 정리되어 있습니다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/112
- 이슈: 없음
