# 마켓온  API — AI Native 협업 환경 (팀원용)

팀원이 **각기 다른 AI 에이전트(Claude / Gemini / ChatGPT 등)** 를 써도, 프로젝트 이해·개발 범위·개발 방식이 **일관되게** 유지되도록 만든 문서 모음이다.

---

## 어떻게 동작하는가 (3단계)

```
1단계  공통 이해   →  docs/ai/00-ai-common-rules.md 를 에이전트에게 먼저 학습시킨다.
2단계  역할 적용   →  00이 "어떤 역할?"을 묻는다. 역할(1~5)을 고르면 01~05 문서로 넘어가 그 도메인만 개발한다.
3단계  개발 흐름   →  모든 기능을 아래 5단계 순서로 동일하게 진행한다.
```

## 핵심 개발 흐름 (모든 기능 공통, 5단계)

```
① ErrorCode 작성  →  ② 기능 구현  →  ③ 단위 테스트 코드 작성  →  ④ Postman 테스트  →  ⑤ PR 요청
```

이게 **초기 공통 기준**이다. 각 팀원은 자기 도메인에 맞게 통합 테스트·성능 테스트·로그·API 문서화 등을 **추가 확장**해도 좋다(줄이지는 않는다).

---

## 하루 일과

```
09:00 ~ 17:00   기능 구현 → 5단계 진행 → 완성되는 대로 PR 요청
                (팀장이 PR마다 상시로 검수·통합하므로, 작게 자주 올린다)
17:00 ~ 18:00   오늘 작업 내용 · 테스트 결과 · 내일 계획 문서 정리
```

PR을 올리면 팀장이 별도 도구로 코드 로직·구조를 검증·테스트한 뒤 develop에 통합한다. 보류되면 사유를 받아 수정 후 다시 올린다.

---

## 폴더 구조 (팀원용)

> **모노레포**: 레포 루트에 **`backend/`**(Spring Boot 백엔드 — `build.gradle`·`src`·`docker-compose.yml`)와 **`docs/`**·**`lead-only/`**가 함께 있습니다. 백엔드 작업은 `cd backend` 후 진행합니다.

```
docs/
├── README.md                          (이 문서)
├── ai/
│   ├── 00-ai-common-rules.md          [1단계] 공통 이해 + 역할 선택 + 5단계 흐름
│   ├── 01-auth-member-agent.md        김대연 — auth, member
│   ├── 02-product-category-agent.md   한상민 — product, category, trade, search
│   ├── 03-favorite-comment-agent.md   권건우 — favorite, comment
│   ├── 04-report-agent.md             서유진 — report
│   ├── 05-admin-common-agent.md       조민석(팀장, 개발자로서)
│   └── ai-native-collaboration-scenario.md   하루 흐름 시나리오(예시)
└── convention/
    └── git-collaboration.md           Git/GitHub 협업 규칙
```

> PR 리뷰·통합 도구는 팀장이 별도로 보유한다(팀원 폴더에는 없음).

---

## 역할 분담 (DDD 기반)

| 선택 | 담당자 | 역할 문서 | 도메인 |
| --- | --- | --- | --- |
| 1 | 조민석(팀장) | 05-admin-common-agent.md | global, admin, 공통구조 |
| 2 | 김대연 | 01-auth-member-agent.md | auth, member |
| 3 | 한상민 | 02-product-category-agent.md | product, category, trade, search |
| 4 | 권건우 | 03-favorite-comment-agent.md | favorite, comment |
| 5 | 서유진 | 04-report-agent.md | report |

---

## 팀원이 하면 되는 것 (한 줄 요약)

> 에이전트에게 `docs/ai/00-ai-common-rules.md` 먼저 읽으라고 한다 → "어떤 역할?"에 본인 역할을 고른다 → 기능을 요청하면 ErrorCode → 구현 → 단위테스트 → Postman → PR 순서로 같이 개발한다.
