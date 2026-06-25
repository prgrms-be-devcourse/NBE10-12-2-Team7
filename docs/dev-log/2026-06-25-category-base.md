# 2026-06-25 Category Base

## 작업 단위

- 브랜치: `feature/category_base`
- 담당 도메인: `category`
- 목표: 카테고리 기준 데이터 저장 기반과 목록 조회 API 생성

## 예외 분석

- `GET /api/categories`는 전체 카테고리 목록 조회 API다.
- 목록 데이터가 없으면 예외가 아니라 빈 배열을 반환한다.
- 이번 작업은 특정 카테고리 ID를 조회하지 않으므로 신규 `ErrorCode`를 추가하지 않는다.
- 기존 `CATEGORY_NOT_FOUND`는 이후 특정 카테고리 조회 또는 상품 목록 조회에서 사용한다.

## 구현 범위

- `Category` Entity
  - `id`: `BIGINT`, PK, `IDENTITY`
  - `name`: `VARCHAR(50)`, `UNIQUE`, `NOT NULL`
- `CategoryRepository`
  - `JpaRepository<Category, Long>`
  - `existsByName(String name)`
- `CategoryInitializer`
  - 애플리케이션 시작 시 기본 카테고리 8개 보장
  - 이미 존재하는 이름은 건너뛰고 없는 카테고리만 저장
- `CategoryResponse`
  - `id`, `name`
- `CategoryService`
  - 카테고리 목록을 `id` 오름차순으로 조회
- `CategoryController`
  - `GET /api/categories`
  - `ApiResponse<List<CategoryResponse>>` 반환

## 기본 카테고리

- 디지털기기
- 생활가전
- 가구/인테리어
- 의류
- 도서
- 스포츠/레저
- 반려동물용품
- 기타

## API 응답

```http
GET /api/categories
```

```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "id": 1,
      "name": "디지털기기"
    }
  ]
}
```

## Postman 시나리오

- 요청: `GET /api/categories`
- 인증: 불필요
- 기대 상태: `200 OK`
- 기대 응답:
  - `status`: `200`
  - `message`: `요청이 성공적으로 처리되었습니다.`
  - `data`: 배열
  - 첫 번째 카테고리 이름: `디지털기기`
  - 마지막 카테고리 이름: `기타`

## 테스트

```bash
cd backend
sh ./gradlew test --tests com.dongnemarket.category.init.CategoryInitializerTest
```

- 기본 카테고리 8개 저장 검증
- 초기화기 재실행 시 중복 저장 방지 검증
- 인증 없이 카테고리 목록 조회 검증
