# Admin 설계 복기 Q&A — 회원 상태변경 슬라이스

> AI에 맡겼던 admin 코드를, 30년차 시각의 **소크라테스식 문답**으로 다시 뜯어보며 설계 의도를 복기하는 학습 로그.
> 형식: **코드 → 질문 → 정답**. 한 슬라이스씩 · 한 질문씩 · 기초(파악+의도) 깊이.
> 시작 2026-06-29 · 슬라이스: 회원 상태변경 (Controller → Service → Entity)

## 진행 범위
- [x] Controller (Q1~Q3)
- [x] Service — 트랜잭션·DTO (Q4~Q6)
- [x] Service — `parseStatus()` — Q7~Q9 (String 파싱·에러 통제·fail-fast)
- [x] Entity — `member.changeStatus()` — Q10~Q12 (캡슐화·불변식·상태vs이력)
- ✅ **슬라이스 완료** (Q1~Q12)

---

## Q1 · Controller · 파악 — 매핑과 입출력

**코드** — `AdminMemberController`
```java
@PatchMapping("/{memberId}/status")   // 클래스: @RequestMapping("/api/admin/members")
public ResponseEntity<ApiResponse<AdminMemberResponse>> changeMemberStatus(
        @PathVariable Long memberId,
        @RequestBody AdminMemberStatusUpdateRequest request) {
    AdminMemberResponse response = adminMemberService.changeMemberStatus(memberId, request);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

**질문** — 어떤 HTTP 요청에 매핑되고, 무엇을 받아 무엇을 돌려주나?

**정답**
```
PATCH /api/admin/members/42/status
Body: { "status": "SUSPENDED" }
```
- 입력 2개의 **출처가 다름**: `memberId`는 `@PathVariable`(URL 경로) · `request`는 `@RequestBody`(요청 body).
- 출력: 서비스 결과(`AdminMemberResponse`)를 공용 `ApiResponse`로 감싸고, 다시 `ResponseEntity`로 감싸 반환.

---

## Q2 · Controller · 의도 — 왜 `@PatchMapping`?

**코드** — `@PatchMapping("/{memberId}/status")`

**질문** — `@PostMapping`/`@PutMapping`이 아니라 왜 `@PatchMapping`인가?

**정답** — `Member`의 여러 필드 중 **`status` 하나만** 바꾸는 부분 수정이라서.

| | 의미 | 언제 |
|---|---|---|
| **PUT** | 리소스 **전체 교체** (빠진 필드는 null로 덮일 위험) | 전체 수정 폼 저장 |
| **PATCH** | **보낸 필드만** 수정, 나머지는 유지 | 상태 변경, 토글 등 부분 업데이트 |

> 판단 기준: 요청 body가 **리소스 전체면 PUT, 일부면 PATCH**.

---

## Q3 · Controller · 의도 — 왜 로직을 Service로 분리?

**코드** — 컨트롤러는 ①받고 ②서비스에 넘기고 ③감싸서 반환만 함. 비즈니스 로직 0줄.

**질문** — 상태 변경 로직을 컨트롤러에 직접 써도 동작하는데 왜 Service로 분리?

**정답**

| 이유 | 설명 |
|---|---|
| **단일 책임(SRP)** | Controller = HTTP 요청/응답만, Service = 비즈니스 로직만 |
| **재사용성** | 슬랙 봇·스케줄러 등 다른 진입점도 **같은 Service를 그대로 호출**. 컨트롤러에 로직을 두면 복붙해야 함 |
| **테스트 용이** | Service만 떼어 단위테스트 가능 |

---

## Q4 · Service · 파악 — `save()` 없이 왜 저장되나?

**코드** — `AdminMemberService`
```java
@Transactional
public AdminMemberResponse changeMemberStatus(Long memberId, AdminMemberStatusUpdateRequest request) {
    Member member = adminMemberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    member.changeStatus(parseStatus(request));   // save() 호출 없음
    return AdminMemberResponse.from(member);
}
```

**질문** — `save()`를 안 불렀는데 왜 DB에 반영되나?

**정답** — **더티 체킹(Dirty Checking)**.
```
findById()            → DB 조회 + "이 시점 상태"를 스냅샷으로 저장
member.changeStatus() → 자바 객체 필드만 변경 (DB는 아직)
@Transactional 종료   → 스냅샷과 비교 → 바뀐 필드 자동 UPDATE → 커밋
```
- `@Transactional`이 있어야 영속성 컨텍스트(감시 공간)가 유지되어 동작함.
- `save()`는 **새 엔티티를 만들 때**만 필요. 이미 영속 상태인 엔티티의 수정엔 불필요.

---

## Q5 · Service · 의도 — 왜 클래스는 `readOnly`, 변경 메서드만 `@Transactional`?

**코드**
```java
@Transactional(readOnly = true)   // 클래스 = 모든 메서드 기본값
public class AdminMemberService {
    public List<AdminMemberResponse> getMembers() { ... }   // readOnly 상속
    @Transactional                                          // 이 메서드만 오버라이드(쓰기)
    public AdminMemberResponse changeMemberStatus(...) { ... }
}
```

**선행 개념** — 어노테이션은 **클래스에 붙이면 모든 메서드 기본값**, **메서드에 붙이면 그 메서드만 덮어씀(오버라이드)**.

**질문** — 읽기엔 `readOnly = true`, 쓰기만 `@Transactional`로 나눈 이유는? 전부 `@Transactional`만 붙이면 안 되나?

**정답** — 읽기 메서드는 변경이 없으니 더티 체킹 부담을 빼는 게 이득이라서.

| 이점 | 설명 |
|---|---|
| **성능** | 스냅샷 생성·종료 시 flush(변경 비교)를 생략 → 조회 많은 곳에서 누적 효과 |
| **의도 선언/안전** | "이 메서드는 데이터를 안 바꾼다"를 코드로 명시 → 실수 변경 사고↓ |
| **(확장) 읽기 복제본 라우팅** | 읽기 전용 트랜잭션을 read replica로 보내는 분기점 |

> 표준 패턴: **클래스에 `readOnly = true`를 깔고, 쓰기 메서드만 `@Transactional`로 예외 처리.**

---

## Q6 · Service · 의도 — 왜 엔티티 말고 DTO로 변환해 반환?

**코드** — `return AdminMemberResponse.from(member);` (엔티티 직접 반환 아님)

**질문** — `return member;`가 더 짧은데 왜 굳이 DTO로 한 번 변환하나?

**정답**

| 이유 | 설명 |
|---|---|
| **민감정보 차단** | `Member`엔 `password`가 있음. 엔티티를 직접 반환하면 JSON 직렬화로 **비밀번호 해시까지 노출**. DTO엔 내보낼 필드만 담음(실제 `AdminMemberResponse`엔 password 없음) |
| **API 계약 안정성** | 엔티티(=DB 테이블 구조)가 바뀌어도 응답 형태를 그대로 유지. DTO가 DB와 API 사이의 **방패** |

> 원칙: **엔티티는 계층 경계 밖(컨트롤러/응답)으로 내보내지 않는다.** `from()` 팩토리가 그 경계를 지키는 장치.

---

## Q7 · Service · 파악 — `parseStatus()`가 하는 일

**코드**
```java
member.changeStatus( parseStatus(request) );   // parseStatus는 enum을 "반환", 변경은 changeStatus가 함

private MemberStatus parseStatus(AdminMemberStatusUpdateRequest request) {
    if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
        throw new BusinessException(ErrorCode.INVALID_MEMBER_STATUS);
    }
    try {
        return MemberStatus.valueOf(request.getStatus().trim());
    } catch (IllegalArgumentException e) {
        throw new BusinessException(ErrorCode.INVALID_MEMBER_STATUS);
    }
}
```

**질문** — 무엇을 입력받아 무엇을 반환하고, 언제 실패하나?

**정답**
- 책임 1가지: **String → `MemberStatus` enum "변환 + 검증"**. 상태를 직접 바꾸는 게 아니라 enum을 돌려줌(변경은 `member.changeStatus()` 몫).
- 실패: request/status가 null·blank → `INVALID_MEMBER_STATUS`. `valueOf` 매칭 실패(예: `"BANNED"`) → `IllegalArgumentException` → catch → `INVALID_MEMBER_STATUS`.
- `valueOf`는 **이름이 정확히 일치**하는 enum 상수를 찾고, 없으면 예외.

---

## Q8 · Service · 의도 — 왜 enum 직접 바인딩이 아니라 String + 직접 파싱?

**대안 비교**
```java
private String status;        // 현재: 받아서 parseStatus로 변환
private MemberStatus status;  // 대안: Jackson이 자동 변환 → parseStatus 불필요(더 짧음)
```

**질문** — 더 짧은 대안 대신 왜 String + 수동 파싱?

**정답** — **에러 응답의 통제권** 때문.

| 잘못된 값 `"BANNED"` 수신 시 | enum 직접 바인딩 | String + 직접 파싱(현재) |
|---|---|---|
| 어디서 실패 | 프레임워크 역직렬화 단계(내 코드 밖) | 내 서비스 코드 안 |
| 에러 모양 | 프레임워크 기본 메시지(제각각) | `BusinessException(INVALID_MEMBER_STATUS)` — 내 API 일관 포맷 |

> 원칙: **프레임워크가 대신 던지는 에러는 통제 밖. 일관된 에러를 원하면 직접 받아 직접 검증.**
> 단 트레이드오프 — 코드 길어짐 + 패턴 반복(신고 상태변경에도 동일). enum 바인딩 + 전역 예외처리로도 해결 가능. **"틀림"이 아니라 "의도된 선택".**

---

## Q9 · Service · 의도 — 검증 순서의 아쉬움 (fail-fast)

**코드**
```java
Member member = adminMemberRepository.findById(memberId)  // ① DB 조회 먼저
        .orElseThrow(...);
member.changeStatus(parseStatus(request));                // ② 그 다음 검증
```

**질문** — 잘못된 `"BANNED"` 요청 시 무엇을 헛되이 하나?

**정답** — 잘못된 입력인데 **DB 조회(①)를 먼저 한 뒤 버림** → 불필요한 쿼리 1번(**fail-slow**).
- 처방: 검증을 앞으로. `MemberStatus s = parseStatus(request);` 먼저 → 그 다음 `findById`. (또는 DTO 경계에서 `@Valid`로 더 일찍)
- 원칙: **입력 검증은 비싼 작업(DB·외부호출) 앞에 둔다 = fail-fast.**
- ※ 버그 아님(응답은 정상). 효율·청결의 문제. 개선 보고서 **C13/D1과 동일 지점**.

---

## Q10 · Entity · 파악 — `changeStatus()`의 추가 동작

**코드**
```java
public void changeStatus(MemberStatus status) {
    this.status = status;
    this.deletedAt = (status == MemberStatus.DELETED) ? LocalDateTime.now() : null;
}
```

**질문** — `status` 대입 말고 추가로 하는 일과 그 규칙은?

**정답** — `deletedAt`을 **함께 동기화**한다.
| status 입력 | deletedAt 결과 |
|---|---|
| `DELETED` | `LocalDateTime.now()` |
| `SUSPENDED`/`ACTIVE` | `null` |
- 진짜 역할: 두 필드를 일관되게 유지 = **불변식**("DELETED인 회원은 deletedAt 있음, 아니면 없음").

---

## Q11 · Entity · 의도 — 왜 setter가 아니라 `changeStatus()`?

**질문** — 단순 `setStatus()` 대신 왜 의미 있는 메서드로?

**정답** — setter면 호출자가 두 줄(`setStatus`+`setDeletedAt`)을 직접 → **한 줄 깜빡하면 모순 상태**(status=DELETED인데 deletedAt=null). 메서드로 묶으면 **불변식이 원천 보장**되고 규칙이 엔티티 한 곳에만 있음.
- 개념: **캡슐화 · Rich Domain Model · "setter를 함부로 열지 마라"**.
- 그래서 Service가 `setStatus`가 아니라 `changeStatus`를 부른다 — Service는 deletedAt 규칙을 몰라도 됨.

---

## Q12 · Entity · 의도 — 숨은 약점 (상태 vs 이력)

**코드** — `this.deletedAt = (status == DELETED) ? now() : null;`

**질문** — DELETED(`deletedAt=6/1`) 회원을 ACTIVE로 복구하면 deletedAt은? 왜 문제?

**정답** — `null`로 **리셋 → 최초 삭제 시각(이력) 영구 소실.** 분쟁·감사·디버깅에서 "언제 삭제됐었나"를 못 답함.
- 근본 원인: `deletedAt` 한 필드가 **현재 상태 플래그 + 과거 이력**을 겸함 → 복구 시 이력이 파괴됨.
- 원칙: **현재 상태(state)와 변경 이력(history)을 한 필드에 섞지 마라.** 이력은 별도 audit/이벤트로.
- 연결: 조사 보고서 **N4**, 더 크게는 **C1(관리자 행위 미기록)** 과 같은 뿌리.

---

## 슬라이스 요약 — 회원 상태변경에서 배운 설계 개념

| 계층 | Q | 개념 |
|---|---|---|
| Controller | Q1·Q2 | HTTP 매핑(PathVariable vs RequestBody), PATCH=부분수정 |
| Controller | Q3 | 계층 분리 / SRP / 재사용성 |
| Service | Q4 | 더티 체킹 (save 없이 UPDATE) |
| Service | Q5 | readOnly 트랜잭션 (성능·의도) |
| Service | Q6 | DTO 경계 (민감정보 차단·API 계약) |
| Service | Q7~Q9 | enum 변환 · 에러 통제권 · fail-fast |
| Entity | Q10·Q11 | 캡슐화 / 불변식 (setter vs 도메인 메서드) |
| Entity | Q12 | 상태 vs 이력 분리 |

---

## 다음 재개 지점 (다른 슬라이스 후보)
- **신고 상태변경** (`AdminReportService` + `Report.changeStatus`) — 회원 슬라이스와 거의 동형. 빠른 복습/대조용.
- **상품 숨김·삭제** (`AdminProductService` + `Product.hide/softDelete`) — 한방향 상태변경, soft delete.
- **대시보드** (`AdminDashboardService`) — 집계·count 설계.
- **`AdminAccountInitializer`** — ApplicationRunner·@Profile·멱등성·BCrypt·시드 비번(의도적 결함) 등 설계결정 밀집.
