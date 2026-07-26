# 위치 기능 계층화 설계 (좌표 작업 전 사전작업)

작성일: 2026-07-26
브랜치: `feature/region_hierarchy`
담당: 한상민 (Product/Category)

## 배경과 범위

기존 `Region`은 flat 구조(`name` 문자열 unique)이고, 상품은 `region` 문자열 컬럼으로
지역을 참조한다. 이번 작업은 실제 좌표 작업 이전의 사전작업으로, 지역을 시–구–동
계층 구조로 재설계하고 상품이 이를 FK로 참조하도록 전환한다.

**스냅샷 고정**: 지역 데이터는 2026년 7월 법정동코드 스냅샷으로 고정한다. 이후
행정구역 개편 반영은 이 작업의 범위 밖이다.

**이번 범위**: 지역 계층 엔티티, 시드 주입, 계단식 조회 API, 상품 연동 및 계층 필터.
**범위 밖**: 좌표/거리 계산, member_locations 전환(문자열 유지), 프론트 계단식 UI 구현.

## 1. 엔티티

```
Region
  id           BIGINT PK auto_increment      // 참조 대상. 의미 없음
  code         CHAR(10) UNIQUE NOT NULL       // 법정동코드. 문자열(prefix 조작용)
  level        INT NOT NULL                   // 1 시도 / 2 시군구 / 3 동
  parent_id    BIGINT NULL FK → regions(id)   // 셀프참조. 최상위는 null
  full_name    VARCHAR(100) NOT NULL          // "서울특별시 종로구 청운동" 표시용 비정규화
  display_name VARCHAR(50) NOT NULL           // "청운동" 단일 표시용 (unique 아님)
```

- 인덱스: `unique(code)`, `index(parent_id)`. code 유니크 인덱스가 prefix 필터도 커버.
- PK를 auto_increment id로 두는 이유: 법정동코드는 불변이 아니다(행정구역 개편 시
  코드 폐지·신규 발급). code를 PK로 잡고 상품이 FK로 물면 개편 시 참조 연쇄 수정이
  되므로, PK에 의미를 싣지 않는다.
- code는 Long이 아니라 문자열(CHAR(10))이다. 연산 대상이 아니라 자릿수 자체가
  의미인 식별자다(앞 2자리 시도, 앞 5자리 시군구). prefix 조작이 필요하므로 문자열.
- **이름 기반 조회·매칭 로직은 어디에도 넣지 않는다.** display_name 중복은 전국 단위
  일반현상이다(전수 집계 597종 중복, level3만 590종). 지역 식별은 항상 id 또는 code로만.
- 기존 flat `Region(name unique)` 구조는 폐기·교체한다.

## 2. 시드 주입

- `region_seed.csv`(헤더 제외 5,338행)를 `src/main/resources`에서 클래스패스 리소스로 로드.
  이 CSV는 **이 브랜치 첫 커밋에 포함**한다.
- CSV 컬럼: `code, level, parent_code, full_name, display_name`.
- **1-pass 주입**: CSV가 code 오름차순이라 부모가 항상 자식보다 먼저 나온다. 위에서부터
  순회하며 `code → 방금 발급된 id` 맵을 메모리에 유지하고, 자식 차례에 `parent_code`로
  맵을 조회해 parent를 연결한다. 2-pass 불필요.
- 멱등: regions 테이블에 행이 있으면 스킵(기존 `DataSeeder` 패턴 유지).
- 기존 하드코딩 `RegionSeeder`의 시군구 List는 전면 교체.

## 3. 계단식 조회 API

부모 기준 단일 엔드포인트. 층 번호로 분기하지 않는다(세종 대응).

```
GET /api/regions                → 최상위(parent 없음) 목록. 16개 시도
GET /api/regions?parentId={id}  → 해당 지역의 자식 목록
```

- 응답 항목마다 `{ id, code, level, displayName }` 포함. **level 필수** — 클라이언트가
  `level==3`으로 선택 종료를 판단한다.
- 세종은 `parentId=세종id` 호출 시 자식이 바로 level 3(동)으로 나오고, 클라이언트는
  동일 규칙(level==3=말단)으로 정상 종료한다. 세종 전용 분기·엔드포인트 없음.
- 시 목록 → 구 목록 → 동 목록이 전부 이 두 형태로 처리된다.
- 코드 어디에서도 "시도의 자식은 level 2다"를 가정하지 않는다. 자식을 따라가거나
  code prefix를 사용한다.

## 4. 상품 연동 및 계층 필터

**부착**: product는 `region_id BIGINT NOT NULL FK → regions(id)`. 등록/수정 검증 =
존재 AND `level==3`. 동이 아니면 거부한다. 기존 `region` varchar 컬럼은 제거한다.

**계층 필터 — code prefix 채택**: 상품은 항상 level3에 붙으므로, 선택한 지역 R의
유효 prefix로 매칭한다.

```sql
SELECT p.* FROM products p
JOIN regions r ON p.region_id = r.id
WHERE r.code LIKE CONCAT(:prefix, '%')
```

`prefix` = R.code의 유효 자릿수(level1 → 앞 2자리, level2 → 앞 5자리, level3 → 전체 10자리).

- **채택 근거**: 하강 깊이에 무관하다(시도→구→동이든 세종의 시도→동이든 동일 동작).
  재귀 CTE·다단 IN 불필요. `regions.code` 유니크 인덱스로 `LIKE 'prefix%'` sargable.
  "시도의 자식이 level2"라는 가정을 하지 않으므로 세종도 특수처리 없이 커버된다.
- **트레이드오프**: 필터가 법정동코드 prefix 규칙(앞 2=시도, 앞 5=시군구)에 결합된다.
  스냅샷 고정이라 이 규칙을 불변으로 취급하고 수용한다.
- `ProductSpecification.regionIn`(문자열 IN)을 이 prefix 조인으로 교체한다.

**전체 주소 표시**: 부모 역추적(N+1) 없이 `full_name` 반환으로 해결한다. 표시용
비정규화이며 개명 시 다중 행 수정이 대가라는 트레이드오프를 인지·수용한 결정이다.

## 5. member / product 지역 표현 불일치 (의도된 결정)

이번 범위에서 member_locations는 문자열 유지, product만 FK로 전환한다. 두 도메인의
지역 표현이 일시적으로 갈린다. 이 불일치는 의도된 결정으로 감수한다.

**접점 조사 결과**:

- **백엔드**: product↔location 접점은 없다(product 패키지에 location 참조 0). 그러나
  member는 region 마스터에 직접 의존한다 — `MemberLocationService.validateRegions()`가
  `RegionRepository.existsByName(region)`으로 회원 동네 문자열을 마스터와 대조 검증한다.
  하드컷으로 마스터를 계층 구조로 교체하면 기존 member 문자열("서울 강남구", 옛 시더
  포맷)이 새 마스터 이름과 매칭되지 않아 회원 동네 저장이 전부 거부된다.
  - **결정**: MemberLocationService에서 마스터 대조 검증(existsByName)만 제거한다. 중복·
    개수 검증은 유지. region은 member가 FK로 전환될 때까지 자유 문자열로 둔다. member는
    원 담당 김대연 영역이므로 **침범 기록 대상**. `RegionRepository.existsByName`은
    이 제거로 사용처가 사라지면 함께 삭제한다.
- **프론트엔드**: 접점 있음. `frontend/src/app/products/page.tsx`에서 회원 활성 동네가
  상품 목록 필터로 직접 흘러든다.
  - L141 `/api/members/me/locations` 조회 → L154 `activeRegion`(문자열, 예 "서울 강남구")
  - L165 `if (activeRegion) params.append('regions', activeRegion)` →
    `GET /api/products?regions=<문자열>`로 전송
  - L337 클라이언트 필터가 상품 응답의 `.region` 문자열을 사용

**공존 기간 동작**: product 목록 필터가 문자열 `regions` 파라미터를 제거하고 `regionId`
기반으로 바뀐다. 파라미터 자체가 사라지므로, 프론트가 보내던 옛 `?regions=<문자열>`은
Spring에서 미인식 쿼리파라미터로 바인딩되지 않고 무시된다 → **전체 목록 반환**. 즉 위
프론트 흐름(활성 동네 → 상품 필터)은 필터가 적용되지 않은 채 동작하며, 에러는 나지 않는다.
프론트가 §7대로 `regionId` 전송으로 수정되면 필터가 복구된다. member 전환과 프론트
계단식 UI는 후속 작업으로 분리한다. member는 원 담당 김대연 영역이므로 침범 기록 대상이다.

## 6. 마이그레이션 전략 (하드컷 리셋)

로컬 테스트 환경 + 더미 데이터라 백필/병행 없이 절단한다.

- `regions` 테이블 재구성: code/level/parent_id/full_name/display_name 추가, 기존
  `name unique` 제거.
- `products`: `region` varchar 제거, `region_id` + FK + 인덱스 추가.
- dev/test는 ddl-auto가 스키마를 담당(자동 반영). **prod는 Flyway `V3__...sql` 스크립트
  필요**(기존 ADR 0004 P4 정책). regions 재구성 + products.region → region_id 전환을 포함.
- 기존 더미 상품은 `region_id NOT NULL` 신설이라 재시드 또는 임의 level3 id 재배정.

## 7. Breaking Change 필드 목록 (프론트 전달용)

프론트엔드가 대응해야 할 계약 변경:

| 구분 | 변경 전 | 변경 후 |
|---|---|---|
| 상품 등록 요청 지역 | `region` (문자열, 예 "서울 강남구") | `regionId` (number, level3 동의 id) |
| 상품 수정 요청 지역 | `region` (문자열) | `regionId` (number) |
| 상품 응답 지역 표현 | `region` (문자열) | `regionId` + `regionName`(full_name 표시용). `region` 문자열 제거 |
| 상품 목록/검색 지역 필터 | `?regions=<문자열>` (최대 2) | `?regionId=<id>` (선택한 지역 하위 전체 매칭) |
| `GET /api/regions` | flat 전체 지역 문자열 목록 | 최상위 목록 `[{id,code,level,displayName}]`. 계단식은 `?parentId=` |
| 제거 필드 | `products.region` (문자열) | — |

프론트 영향 지점: `products/page.tsx`(동네 필터·목록 조회), `ProductForm.tsx`(지역 선택),
`products/[id]/page.tsx`(상세 지역 표시), `my-profile/page.tsx`(동네 설정 모달).

## 8. 테스트 전략

**시드 검증**
- 총 5,338행, level 분포 1:16 / 2:255 / 3:5,067
- 고아 행 0(모든 parent_code가 유효 id로 연결)
- 세종 직속 자식 33개(전부 level 3)
- 재기동 시 중복 미적재(멱등)

**세종 경로**
- `parentId=세종id` 자식 조회가 level 3을 반환
- 세종 선택 시 계층 필터(prefix 2자리)가 세종 소속 상품만 정확히 반환

**이름 중복**
- 같은 display_name(예: 교동)이 다른 부모 아래 공존해도 조회·저장이 id/code로 정확

**상품 연동**
- level3 아닌 region_id로 등록 시 거부
- 구(level2) 필터가 하위 전체 동 상품 반환, 시도(level1) 필터가 그 아래 전체 반환
- 문자열 `regions` 파라미터가 들어와도 서버가 조용히 무시(공존 기간 동작)

## 9. 영향 범위

| 영역 | 변경 |
|---|---|
| region 패키지 | entity/repo/service/controller/dto 전면 개편 |
| product | entity(region_id), ProductSpecification, 등록·수정 검증, 응답 DTO |
| 시드 | RegionSeeder 재작성, `region_seed.csv` 리소스 추가(첫 커밋) |
| prod 스키마 | Flyway V3 신규 |
| member_locations | 이번 미변경(문자열 유지). 백엔드 접점 없음, 프론트 접점은 §5·§7에 명시 |
| frontend | §7 breaking 목록대로 후속 대응 필요(백엔드 범위 밖) |

## 10. 작업 순서

1. `region_seed.csv` 커밋 + Region 엔티티/repo 재작성
2. RegionSeeder 재작성(1-pass) + 시드 검증 테스트
3. 계단식 조회 API(service/controller/dto) + 테스트
4. product region_id 전환(entity/DTO/검증) + 계층 필터(prefix) + 테스트
5. Flyway V3 작성
6. 전체 테스트 통과 확인
7. 작업 단위별 로컬 커밋(push 금지)
