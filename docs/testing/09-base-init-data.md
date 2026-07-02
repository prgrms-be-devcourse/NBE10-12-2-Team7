# 09. Base Init Data (개발/검증용 초기 시드) — 제안서

> **상태: 제안(PROPOSAL) · 미도입.** 이 문서는 "무엇을·어떻게 시드할지"를 먼저 합의하기 위한 설계 명세다.
> 승인 후 초기화 코드를 도입한다. (코드 선(先)작성 금지 — 문서 → 승인 → 도입)

---

## 1. 배경 / 결함

현재 애플리케이션 부팅 시 시드되는 데이터는 두 가지뿐이다.

| 시더 | 내용 | 프로파일 |
|---|---|---|
| `AdminAccountInitializer` | 관리자 1명 (`admin@dongnemarket.com` / `admin1234!`, ROLE_ADMIN·ACTIVE) | `@Profile("!test")` |
| `CategoryInitializer` | 카테고리 8종 (디지털기기·생활가전·가구/인테리어·의류·도서·스포츠/레저·반려동물용품·기타) | (제한 없음) |

**결함:** 일반 회원·상품·댓글·신고 등 **업무 데이터가 전무**하다. 그래서
- 관리자 콘솔(회원/상품/신고/댓글 탭)을 열어도 전부 "없습니다"로 표시되어 **실제 동작을 검증할 수 없다.**
- 각 상태(정지 회원, 숨김 상품, 삭제 댓글, 신고 처리 단계 등)를 수기로 매번 만들어야 한다.

**목표:** 부팅 시 관리자 콘솔의 **모든 화면 상태를 한 번에 검증**할 수 있는 최소 시드 세트를, 운영에 안전한 가드와 함께 도입한다.

---

## 2. 설계 개요

### 2.1 형태
기존 시더와 동일하게 `ApplicationRunner` 구현체 1개(`BaseInitDataInitializer`)로 도입한다.

### 2.2 안전 가드 (도입 결정 필요 — §5)
| 항목 | 권고안 | 이유 |
|---|---|---|
| 프로파일 | `@Profile("!test")` **필수** | 슬라이스/통합 테스트 오염 방지 (기존 컨벤션과 동일) |
| 실행 스위치 | 프로퍼티 플래그 `app.seed.base-data=true`(기본 **false**) 또는 `local` 프로파일 게이팅 | **운영 DB에 절대 시드되지 않도록.** 이게 "결함 수정"의 핵심 안전장치 |
| 멱등성 | 센티넬 존재 검사 후 skip (예: `existsByEmail("user01@dongnemarket.com")`면 전체 skip) | MySQL 볼륨이 재시작 간 유지되므로 중복 삽입 방지 |
| 실행 순서 | 카테고리·관리자 시더 **이후** 실행 (`@Order` 로 뒤 번호 부여) | 상품이 기존 카테고리를 이름으로 조회해 재사용 |

### 2.3 시드 순서 (FK/의존성 강제)
```
1) 회원(Member)      ── 저장 후 id 확보
2) 카테고리 조회      ── 기존 8종을 이름으로 findByName
3) 상품(Product)     ── Member·Category 엔티티 참조 + 상태 변형(hide/softDelete/tradeStatus/viewCount)
4) 댓글/즐겨찾기/신고  ── 3)까지의 저장된 id(Long)를 사용
```

### 2.4 위치 / 패키지 (결정 필요 — §5)
도메인을 가로지르므로 도메인 패키지에 두기 애매하다. 권고: `com.dongnemarket.global.init.BaseInitDataInitializer`
(대안: `com.dongnemarket.support.init`). — 최종 위치는 팀장 결정.

---

## 3. 시드 데이터 상세

> 비밀번호는 전부 BCrypt로 인코딩해 저장(로그인 가능해야 함). 아래 "원문 비번"으로 로그인.

### 3.1 회원 (Member) — 관리자 시더의 admin 외 신규 6명
| 로그인 이메일 | 원문 비번 | 닉네임 | 권한 | 상태 | 비고 |
|---|---|---|---|---|---|
| admin@dongnemarket.com | admin1234! | 관리자 | ROLE_ADMIN | ACTIVE | (기존 시더) |
| user01@dongnemarket.com | user1234! | 상민 | ROLE_USER | ACTIVE | 판매자·댓글작성 |
| user02@dongnemarket.com | user1234! | 지훈 | ROLE_USER | ACTIVE | 구매자·신고자 |
| user03@dongnemarket.com | user1234! | 민서 | ROLE_USER | ACTIVE | 판매자 |
| user04@dongnemarket.com | user1234! | 철수 | ROLE_USER | **SUSPENDED** | 정지 배지·로그인 차단 검증 |
| user05@dongnemarket.com | user1234! | 영희 | ROLE_USER | **DELETED** | 소프트삭제·로그인 차단·목록엔 노출 |
| admin2@dongnemarket.com | admin1234! | 부관리자 | ROLE_ADMIN | ACTIVE | (선택) N1 갭 시연용 — 관리자↔관리자 |

> SUSPENDED/DELETED는 `createUser()` 후 `changeStatus(...)` 로 변형. DELETED는 `deletedAt` 자동 세팅.

### 3.2 상품 (Product) — 6건, 거래상태·숨김·삭제 전 조합
| # | 제목 | 카테고리 | 판매자 | tradeStatus | hidden | deletedAt | viewCount |
|---|---|---|---|---|---|---|---|
| P1 | 아이폰 13 128GB | 디지털기기 | 민서 | ON_SALE | - | - | 152 |
| P2 | 원목 책상 의자 | 가구/인테리어 | 상민 | RESERVED | - | - | 43 |
| P3 | 에어팟 프로 2세대 | 디지털기기 | 민서 | COMPLETED | - | - | 88 |
| P4 | 겨울 패딩 (L) | 의류 | 상민 | ON_SALE | **true** | - | 12 |
| P5 | 캠핑 텐트 4인용 | 스포츠/레저 | 철수(정지) | ON_SALE | - | **set** | 5 |
| P6 | 강아지 사료 5kg | 반려동물용품 | 지훈 | ON_SALE | - | - | 27 |

> 변형: P3 `complete()`, P2 `changeTradeStatus(RESERVED)`, P4 `hide()`, P5 `softDelete()`, viewCount는 `increaseViewCount()` 반복 또는 리플렉션.

### 3.3 댓글 (Comment) — 5건, 정상 + 삭제
| # | 상품 | 작성자 | 내용 | 상태 |
|---|---|---|---|---|
| C1 | P1 | 지훈 | 관심있어요! 네고 가능한가요? | 정상 |
| C2 | P1 | 상민 | 직거래 가능합니다 | 정상 |
| C3 | P3 | 지훈 | 상태 좋네요, 잘 쓸게요 | 정상 |
| C4 | P4 | 민서 | (부적절 내용) | **삭제됨** (softDelete) |
| C5 | P6 | 상민 | 우리 강아지가 잘 먹어요 | 정상 |

### 3.4 즐겨찾기 (Favorite) — 3건 (관리자 콘솔엔 탭 없음 · 사용자 API 검증용, 선택)
| # | 회원 | 상품 |
|---|---|---|
| F1 | 지훈 | P1 |
| F2 | 상민 | P3 |
| F3 | 지훈 | P6 |

> `uk_favorites_member_product` 유니크 제약 → (회원,상품) 중복 금지.

### 3.5 신고 (Report) — 5건, 4개 상태 + 2개 유형 + 여러 사유
| # | 유형 | 신고자 | 대상 | 사유 | 상태 |
|---|---|---|---|---|---|
| R1 | PRODUCT | 지훈 | P1 | FRAUD_SUSPECTED | **RECEIVED** |
| R2 | PRODUCT | 상민 | P4 | PROHIBITED_ITEM | REVIEWING |
| R3 | MEMBER | 민서 | 철수(회원) | INAPPROPRIATE_CONTENT | COMPLETED |
| R4 | PRODUCT | 지훈 | P3 | FAKE_ITEM | REJECTED |
| R5 | MEMBER | 상민 | 지훈(회원) | ETC | **RECEIVED** |

> 상태는 `ofProduct/ofMember`(초기 RECEIVED) 저장 후 `changeStatus(...)` 로 변형.

---

## 4. 관리자 콘솔 검증 매핑 (이 시드로 확인되는 것)

| 탭 | 확인되는 상태 | 기대 표시 |
|---|---|---|
| **대시보드** | 집계 | 총회원 **7** · 총상품 **6** · 총신고 **5** · **처리대기 2**(RECEIVED만) · 총댓글 **5** |
| **회원** | ACTIVE/SUSPENDED/DELETED, USER/ADMIN | 상태 배지 3종 + 권한 배지 2종 |
| **상품** | ON_SALE/RESERVED/COMPLETED, 숨김, 삭제됨, 조회수 | 거래 3종 + 숨김(P4) + 삭제됨(P5) |
| **신고** | RECEIVED/REVIEWING/COMPLETED/REJECTED, PRODUCT/MEMBER | 상태 4종 + 유형 2종 + 사유 5종 |
| **댓글** | 정상 + 삭제됨 | 삭제 취소선(C4) |

> **N3 갭이 눈에 보이는 지점:** 실제 미처리 신고는 R1·R2·R5(3건 성격)지만 대시보드 "처리대기"는 **RECEIVED만 세어 2**로 표시된다. REVIEWING(R2)은 빠진다 — 시드가 이 갭을 드러낸다.

---

## 5. 도입 시 결정 필요 사항 (승인 게이트)

1. **실행 게이팅 방식** — (A) 프로퍼티 플래그 `app.seed.base-data`(기본 false, 권고) / (B) `local` 프로파일 전용 / (C) `@Profile("!test")`만(개발 편의 우선, 운영 위험). → **택1**
2. **두 번째 관리자(admin2) 포함 여부** — N1(관리자↔관리자) 시연용. 불필요하면 제외.
3. **즐겨찾기 시드 포함 여부** — 관리자 콘솔엔 안 보임(사용자 API 검증용). 관리자 검증만 목적이면 생략 가능.
4. **시더 위치** — `global.init` vs `support.init`.
5. **데이터 규모** — 위 최소 세트로 충분한지, 페이지네이션/검색 확인을 위해 상품을 수십 건으로 늘릴지.

---

## 6. 도입 절차 (승인 후)

1. 결정사항(§5) 확정.
2. `BaseInitDataInitializer` 구현 코드 채팅 제공 → 팀장이 직접 반영(코드 제공 워크스타일).
3. 시더 테스트(`BaseInitDataInitializerTest`) — 멱등성/프로파일 가드/건수 검증. 명세는 본 문서 §3·§4 기준.
4. `./gradlew test` green 확인(기존 277 테스트 영향 없음 — `@Profile("!test")` 로 test 프로파일에서 미실행).
5. `docker compose up -d` 후 `bootRun` → `http://localhost:8080/admin.html` 에서 §4 매핑대로 육안 검증.

---

## 7. 리스크 / 주의

- **운영 유입 금지:** §5-1 게이팅이 없으면 운영 DB에 더미가 들어갈 수 있음 → 플래그/프로파일 가드 필수.
- **멱등성:** `ddl-auto: update` + 볼륨 유지 환경에서 재부팅마다 중복 삽입되지 않도록 센티넬 검사 필수.
- **유니크 제약:** 이메일/닉네임(회원), 카테고리명, (회원,상품)(즐겨찾기) 충돌 주의.
- **테스트 격리:** test 프로파일에서 절대 실행되지 않아야 함(픽스처와 충돌·건수 검증 깨짐).
- 본 시더는 **개발/검증 전용**이며 제품 기능이 아니다.
