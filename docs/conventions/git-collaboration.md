# 협업 규칙 — Git/GitHub

> 최종 수정일: 2026-07-07 · 상태: draft
> 1차 프로젝트의 실제 Git 워크플로우를 복원한 문서다. **팀장 상시 통합(PR 단위)** 환경에 맞춘 규칙이며, 기본 흐름은 팀장이 공통 베이스·환경을 먼저 세팅한 뒤 팀원이 각자 도메인 역할에 맞게 진행하는 방식이다. 시스템 구조는 [architecture](../architecture/README.md), 이 규칙을 정한 배경은 [ADR](../adr/README.md) 참고.

---

## 0. 핵심 원칙

```
1. 충돌이 생길 자리를 구조적으로 없앤다 (공통 베이스 선고정 + 도메인 패키지 분리).
2. PR은 작게, 자주 올린다. 큰 PR은 리뷰가 불가능하다.
3. develop 브랜치는 항상 실행 가능(green) 상태를 유지한다.
4. AI가 생성한 코드의 최종 책임은 담당 개발자에게 있다.
```

---

## 1. Day 0 — 팀장 공통 베이스 세팅 (기능 개발 전 필수)

팀원이 도메인 작업을 시작하기 **전에**, 팀장이 아래 공통 기반을 먼저 `develop`에 올려 고정한다.
가장 큰 충돌 원인은 "공통 기반이 없는 상태에서 5명이 각자 다른 ApiResponse·ErrorCode·예외구조를 만드는 것"이므로, 이 단계가 가장 중요하다.

```
- global 패키지 골격: common / config / exception / response / security
- ApiResponse<T>, ErrorResponse 포맷
- ErrorCode.java 골격 (COMMON 영역 + 도메인별 주석 영역)
- GlobalExceptionHandler
- SecurityConfig (JWT 기본 구조)
- build.gradle, application.yml
- Swagger / Springdoc 설정
- main, develop 브랜치 생성 및 보호 설정
```

**이 베이스가 깔리기 전에는 도메인 기능 PR을 올리지 않는다.**

---

## 2. 브랜치 전략 (Git Flow Lite)

```
main
 └── develop
      ├── feature/auth_signup
      ├── feature/auth_login
      ├── feature/product_create
      └── ...
```

| 브랜치 | 역할 |
| --- | --- |
| `main` | 최종 제출·발표 가능한 안정 버전 (팀장만 merge) |
| `develop` | 팀원 기능을 통합하는 개발 브랜치 (항상 green) |
| `feature/*` | 각 팀원이 기능을 구현하는 작업 브랜치 |

- `main`, `develop`에 직접 push 금지. 모든 작업은 `feature/*`에서.
- feature 브랜치는 `develop`에서 분기한다.
- **기능을 잘게 가져간다.** `feature/{도메인}_{기능명}` 형식.

```bash
git checkout develop
git pull origin develop
git checkout -b feature/auth_signup
```

---

## 3. 일일 개발 흐름

```
09:00 ~ 17:00  [상시 루프]
   팀원 - 기능 구현 전, Notion WBS 'task 03.개발' 그룹에 기능 단위로 "개발할 것" 문서를 먼저 작성한다
        - 그 뒤 각자 feature 브랜치에서 기능 구현 (자기 도메인만)
        - 개발 흐름(ErrorCode → 구현 → 단위 테스트 → 통합 테스트 → API 테스트(Postman) → PR)으로 진행
        - 기능이 완성되는 대로 작은 PR을 develop로 올린다 (하루치를 몰아두지 않는다)
   팀장 - PR이 올라올 때마다 코드 로직·구조 이해 → 검증·테스트
        - 문제 없으면 그때그때 develop에 통합 (PR 단위 상시 통합)

17:00 ~ 18:00  [마무리]
   팀원 - 오늘 작업 내용 · 테스트 결과 · 내일 계획 문서 정리
        - 추가한 기능이 시스템 구성·API 등에 영향을 주는지 확인하고,
          영향이 있으면 docs/(architecture · api 등)를 갱신한다
   팀장 - develop 한 번 더 코드 검증 → 정상 실행 확인 → develop 최종 통합 상태 확정
```

핵심: **통합은 하루 끝에 몰아서가 아니라 09:00~17:00 내내 PR 단위로 상시** 일어난다. 그래서 develop이 늘 최신이고 충돌이 작다.

---

## 4. Pull Request 규칙

- PR은 `feature/*` → `develop`으로 생성한다.
- **작게, 자주.** 큰 PR은 금지.
- **PR 본문 템플릿의 정본은 [`.github/pull_request_template.md`](../../.github/pull_request_template.md)** — PR 생성 시 자동으로 채워진다(중복 방지를 위해 여기 다시 적지 않는다).
- 템플릿에는 개발 흐름 체크, **문서 영향 게이트([AGENTS.md](../../AGENTS.md) §5 — 코드와 같은 PR에서 문서 갱신)**, AI 사용 여부, 머지 게이트가 포함된다.

---

## 5. 머지 게이트 (하드 게이트)

아래 조건을 **모두** 충족할 때만 `develop`에 머지한다.

```
1. 빌드 성공 / 애플리케이션 정상 기동
2. 본인이 맡은 API 정상 동작 (Postman 확인)
3. develop 최신 코드 반영
4. 충돌 해결 완료
5. 담당 패키지 외 변경 없음 (범위 위반 없음)
6. 공통 응답 형식(ApiResponse / ErrorResponse) 준수
7. 테스트 코드 / Swagger / Postman 시나리오 존재
8. PR 설명 작성 완료
```

**팀장이 PR 단위로 검수한 뒤, 통과하면 직접 develop에 머지한다.**

---

## 6. 충돌 해결 규칙

| 충돌 파일 | 담당 |
| --- | --- |
| 도메인 관련 파일 | 해당 도메인 주담당자 |
| 공통 설정 파일·문서 | 팀장 |

공통 파일 예시:

```
build.gradle / application.yml / SecurityConfig
GlobalExceptionHandler / ApiResponse / README.md
```

- 공통 파일은 수정 전 팀장에게 공유 → 수정 후 팀원에게 공지.
- **ErrorCode.java 핫스팟 전략**: 5명이 같은 파일에 추가하므로 충돌이 잦다. 기본은 "자기 도메인 주석 영역에만 append". 충돌이 계속 발생하면 **도메인별 ErrorCode 파일/enum 분리**(`AuthErrorCode`, `ProductErrorCode` 등)를 도입해 공유 파일 충돌 자체를 제거한다.

---

## 7. 커밋 메시지 규칙

```
타입: 작업 내용
```

| 타입 | 의미 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `refactor` | 코드 리팩토링 |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 설정·빌드·기타 작업 |

예: `feat: 회원가입 API 구현` / `fix: 로그인 토큰 검증 오류 수정`

---

## 8. AI 에이전트 연동 규칙

```
1. 개발 에이전트는 이 협업 규칙을 함께 따른다.
   - 브랜치명(feature/도메인_기능), 커밋 타입, PR 템플릿을 규칙대로 제안한다.
2. 팀장의 통합 도구는 리뷰·충돌분석·머지판정만 한다.
   - 실제 머지 실행, 도메인 로직 충돌 최종 해결, 공통 파일 수정은 팀장(사람)이 한다.
3. AI가 생성한 코드는 담당 개발자가 직접 검토한 뒤 PR을 올린다.
```

---

## 9. develop / main 관리

- `develop`은 항상 실행 가능 상태 유지. 머지 후 깨지면 PR 작성자가 우선 복구하고, 오래 걸리면 해당 머지를 revert 후 재PR.
- `main`은 팀장이 마일스톤·발표 시점에만 `develop`에서 머지.
- release / hotfix 브랜치는 사용하지 않는다 (프로젝트 범위에 과함).

---

## 10. GitHub Project Status

```
Backlog → Ready → In Progress → In Review → Done
```

| Status | 의미 |
| --- | --- |
| Backlog | 아직 정리되지 않은 작업 |
| Ready | 바로 개발 가능한 작업 |
| In Progress | feature 브랜치에서 개발 중 |
| In Review | PR 생성 및 리뷰 중 |
| Done | develop에 merge 완료 |
