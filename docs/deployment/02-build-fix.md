# 02. 빌드 가능화 — 컴파일 오류 복구

> 목적: 배포용 아티팩트(`bootJar`)를 만들기 전, **컴파일 자체가 깨져 있던 문제**를 복구한 기록.
> 작업일: 2026-07-01 · 브랜치: `feat/devops`

---

## 1. 증상

`./gradlew bootJar -x test` 실행 시 `compileJava` 단계에서 **6개 컴파일 오류**로 빌드 실패:

```
error: cannot find symbol  → report.getReporterId() / getTargetMemberId() / getTargetProductId()
error: incompatible types: Long cannot be converted to Member  → Report.ofProduct/ofMember(...)
```

> ⚠️ 콘솔의 한글 깨짐(`?ш린…`)은 git bash 출력 인코딩 문제일 뿐, **소스 파일 인코딩은 정상**이었다.

## 2. 근본 원인 — 머지 통합 갭

`Report` 도메인이 리팩터링되면서 **신고 대상을 id(Long)가 아니라 연관 엔티티(@ManyToOne) 참조로** 바꿨다.

| 구분 | 리팩터링 전 | 리팩터링 후 (현재) |
|------|------------|-------------------|
| 필드 | `Long reporterId` 등 | `Member reporter`, `Member targetMember`, `Product targetProduct` |
| 게터 | `getReporterId(): Long` | `getReporter(): Member` (id 게터는 삭제) |
| 팩토리 | `ofProduct(Long, Long, …)` | `ofProduct(Member, Product, …)` / `ofMember(Member, Member, …)` |

그런데 이 API를 쓰는 **두 호출부가 옛 방식 그대로** 남아 있었다.
`report_refactor`와 `base-init-data`가 각각 develop에 머지됐지만 서로 맞춰지지 않은 **통합 갭**이며, 이로 인해 **develop 전체가 컴파일 불가** 상태였다.

## 3. 수정 내용

### 파일 1 — `admin/dto/AdminReportResponse.java` (생성자)

없어진 id 게터 호출 → **연관 엔티티를 거쳐 `.getId()`로 추출**. 대상은 신고 유형에 따라 한쪽이 null이므로 **null 체크** 추가.

```java
// before
this.reporterId      = report.getReporterId();
this.targetMemberId  = report.getTargetMemberId();
this.targetProductId = report.getTargetProductId();

// after
this.reporterId      = report.getReporter().getId();
this.targetMemberId  = report.getTargetMember()  != null ? report.getTargetMember().getId()  : null;
this.targetProductId = report.getTargetProduct() != null ? report.getTargetProduct().getId() : null;
```

> `reporter`는 엔티티에서 `nullable = false`라 항상 존재 → null 체크 불필요.
> `getReporter().getId()`는 LAZY 프록시라도 id는 이미 알고 있어 **추가 DB 조회를 일으키지 않는다**.

### 파일 2 — `global/init/BaseInitDataInitializer.java` (신고 시드 5건)

팩토리 시그니처가 엔티티를 받도록 바뀌었으므로 **`.getId()` 제거**하고 엔티티를 그대로 전달.

```java
// before
Report.ofProduct(user02.getId(), p1.getId(), ReportReason.FRAUD_SUSPECTED, "...");
Report.ofMember (user03.getId(), user04.getId(), ReportReason.INAPPROPRIATE_CONTENT, "...");

// after
Report.ofProduct(user02, p1, ReportReason.FRAUD_SUSPECTED, "...");
Report.ofMember (user03, user04, ReportReason.INAPPROPRIATE_CONTENT, "...");
```
(총 5개 호출: `ofProduct` 3건 + `ofMember` 2건)

## 4. 검증

- 옛 게터의 다른 참조 여부 확인 → 남은 3건은 `AdminReportResponse` **자신의 게터**(DTO 공개 API)라 정상, `Report` 호출 아님.
- `./gradlew clean compileJava -x test` → **BUILD SUCCESSFUL**
- `./gradlew bootJar -x test` 통과.

## 5. 영향 & 후속

- 이 수정으로 **develop 계열 컴파일 복구**. 배포용 JAR 빌드 가능 상태 확보.
- 팀 공유 필요(다른 브랜치도 같은 갭을 가질 수 있음).
- 재발 방지 제언: 리팩터링 시 **엔티티 API 변경 → 전체 호출부 컴파일 확인**을 PR 체크에 포함(시드/DTO 등 부수 코드가 누락되기 쉬움).
