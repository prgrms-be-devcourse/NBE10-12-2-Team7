# 데이터 모델 (ERD)

JPA 엔티티 기준 테이블 구조와 관계. 스키마는 `ddl-auto=update`로 엔티티에서 자동 생성된다.

```mermaid
erDiagram
    MEMBERS ||--o{ PRODUCTS : "등록"
    CATEGORIES ||--o{ PRODUCTS : "분류"
    MEMBERS ||--o{ FAVORITES : "찜 (논리 FK)"
    PRODUCTS ||--o{ FAVORITES : "대상 (논리 FK)"
    MEMBERS ||--o{ COMMENTS : "작성 (논리 FK)"
    PRODUCTS ||--o{ COMMENTS : "대상 (논리 FK)"
    MEMBERS ||--o{ REPORTS : "신고자"
    MEMBERS ||--o{ REPORTS : "피신고 회원"
    PRODUCTS ||--o{ REPORTS : "피신고 상품"

    MEMBERS {
        bigint id PK
        varchar email UK
        varchar password
        varchar nickname UK
        enum role "ROLE_USER / ROLE_ADMIN"
        enum status "ACTIVE / DELETED / SUSPENDED"
        datetime deleted_at "nullable, soft delete"
        datetime created_at
        datetime updated_at
    }
    PRODUCTS {
        bigint id PK
        bigint member_id FK
        bigint category_id FK
        varchar title
        varchar description
        int price
        enum trade_status "ON_SALE / COMPLETED"
        varchar region
        bigint view_count
        boolean hidden
        datetime deleted_at "nullable, soft delete"
        datetime created_at
        datetime updated_at
    }
    CATEGORIES {
        bigint id PK
        varchar name UK
    }
    FAVORITES {
        bigint id PK
        bigint member_id "논리 FK, uk(member_id+product_id)"
        bigint product_id "논리 FK"
        datetime created_at
        datetime updated_at
    }
    COMMENTS {
        bigint id PK
        bigint member_id "논리 FK"
        bigint product_id "논리 FK"
        varchar content
        datetime deleted_at "nullable, soft delete"
        datetime created_at
        datetime updated_at
    }
    REPORTS {
        bigint id PK
        bigint reporter_id "신고자"
        bigint target_member_id "nullable, 회원 신고 시"
        bigint target_product_id "nullable, 상품 신고 시"
        enum report_type "PRODUCT / MEMBER"
        enum reason
        varchar content "nullable"
        enum status "RECEIVED 등"
        datetime created_at
        datetime updated_at
    }
```

## 관계 표현 방식 — 중요한 설계 결정

- **`products`만 JPA 연관관계**(`@ManyToOne`)로 `members`·`categories`를 참조한다 → 물리적 외래키(FK) 생성.
- **`favorites`·`comments`·`reports`는 `Long` ID 참조**(논리 FK)다. 엔티티 객체 대신 `member_id`/`product_id` 같은 ID 값만 저장하고 JPA 연관관계를 두지 않는다 → 도메인 간 결합도를 낮추는 의도적 선택.
- 따라서 위 ERD의 일부 관계선은 **DB 제약이 아니라 애플리케이션 레벨의 논리적 관계**다.

## 공통 규칙

- **시각 필드**: `categories`를 제외한 모든 테이블이 `BaseTimeEntity`를 상속해 `created_at`/`updated_at`을 자동 관리한다.
- **소프트 삭제**(`deleted_at`): `members`(+`status=DELETED`), `products`, `comments`. 물리 삭제 대신 표시만 한다.
- **유니크 제약**: `members.email`, `members.nickname`, `categories.name`, `favorites(member_id, product_id)`.
