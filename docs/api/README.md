# API 명세 (api)

> 최종 수정일: 2026-07-28 · 상태: draft

## 정본은 Swagger (Springdoc)

API의 **source of truth는 코드에서 자동 생성되는 OpenAPI/Swagger**다. 컨트롤러가 바뀌면 문서도 자동으로 바뀌므로, 요청/응답 스키마의 최신 상세는 항상 Swagger에서 본다. 이 문서는 그 위의 **공통 규약 + 리소스 지도**만 담는다.

| 항목 | 위치 (dev 기준) |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

> Postman은 위 OpenAPI를 import해 쓰는 **파생물**이다(개인/워크스페이스 관리). 배경은 [ADR 0001](../adr/0001-adopt-docs-as-code-structure.md).

## 공통 규약

**베이스**: 모든 API는 `/api` 프리픽스. 브라우저는 단일 origin만 호출하고 `/api`는 nginx(운영)·Next dev 서버(개발)가 프록시한다(CORS 없음).

**인증**: JWT Access Token. 헤더 `Authorization: Bearer {accessToken}`. 만료 시 `/api/auth/reissue`로 재발급.

**성공 응답** — `ApiResponse<T>`:

```json
{ "status": 200, "message": "요청이 성공적으로 처리되었습니다.", "data": { } }
```

**에러 응답** — `ErrorResponse` (전역 `GlobalExceptionHandler`가 `BusinessException` + `ErrorCode`를 변환):

```json
{ "status": 404, "error": "PRODUCT_NOT_FOUND", "message": "상품을 찾을 수 없습니다.", "timestamp": "2026-07-07T12:00:00" }
```

## 리소스 지도 (도메인별)

상세 파라미터·스키마는 Swagger 참고. 아래는 **어떤 리소스가 있는지**의 지도다.

| 도메인 | 베이스 경로 | 주요 리소스 |
| --- | --- | --- |
| auth | `/api/auth` | `signup` · `login` · `logout` · `reissue`, `/email-verifications`(confirm) · `/password-resets` |
| member | `/api/members` | `me` · `me/password` · `me/locations` · `me/favorites` · `me/reports` |
| product | `/api/products` | 목록·등록·`{id}`(상세·수정·삭제) · `{id}/status` · `me` · `search` |
| category | `/api/categories` | 목록 · `{categoryId}/products` |
| comment | `/api/products/{productId}/comments`, `/api/comments/{commentId}` | 작성·목록 / 수정·삭제 |
| favorite | `/api/products/{productId}/favorites`, `/api/members/me/favorites` | 등록·취소 / 내 관심 |
| report | `/api/products/{id}/reports`, `/api/members/{id}/reports`, `/api/reports/evidence-image` | 상품·회원 신고 / 증빙 이미지 |
| notification | `/api/notifications` | 목록 · `read` · `unread-count` |
| chat | `/api/chat-rooms` | 방 목록·생성 · `{roomId}/messages` · `{roomId}/read` |
| region | `/api/regions` | 지역 사전 조회 |
| admin | `/api/admin` | `members`(+`{id}/status`) · `products`(+`{id}/hidden`,`{id}/status`) · `comments` · `reports`(+`{id}/status`) · `dashboard` · `ai` |

> 인증 불필요(공개): 회원가입·로그인, 상품 목록/상세, 카테고리, 댓글 목록. 그 외 쓰기·내 정보·`/api/admin/**`(ROLE_ADMIN)은 인증 필요.

## 지역 필드 규약

지역은 문자열 이름이 아니라 `regionCode`를 식별자로 사용한다. 화면 표시에는 `regionFullName`을 사용한다.

| 필드 | 의미 | 사용처 |
| --- | --- | --- |
| `regionCode` | 법정동 코드. 지역 선택·상품 등록/수정·동네 설정·상품 지역 필터의 식별자 | 요청/응답 |
| `regionName` | 현재 선택된 지역의 표시명. 예: `역삼동` | 응답 |
| `regionFullName` | 전체 표시명. 예: `서울특별시 강남구 역삼동` | 응답 화면 표시 |

상품·회원 동네·상품 관련 요약 응답에서 과거 호환 필드였던 `region`은 제거됐다. 기존 표시값이 필요하면 `regionFullName`을 사용한다.

상품 등록/수정 요청은 `regionCode`가 필수이며 신규 상품은 level 3 지역만 허용한다. 회원 내 동네 설정도 `regionCodes` 리스트를 사용한다.

`GET /api/regions`는 계층형 지역 사전을 반환한다. 파라미터 없이 호출하면 최상위 지역 목록을 반환하고, `parentCode`를 넘기면 해당 지역의 하위 지역을 반환한다.
