# 테스트 전략

> 최종 수정일: 2026-07-07 · 상태: draft

개발 흐름의 "③ 단위 테스트" 단계를 실제 도구로 구체화한다. 개발 흐름 자체는 [git-collaboration.md](git-collaboration.md), CI 실행은 [runbook/ci-cd.md](../runbook/ci-cd.md).

## 테스트 계층

| 계층 | 도구 | DB | CI 실행 | 목적 |
| --- | --- | --- | --- | --- |
| **단위 / 슬라이스** | JUnit5 · Mockito · `@WebMvcTest`/`@DataJpaTest` 등 | **H2**(인메모리) | ✅ 매 PR·push | 로직·계층 단위 검증. 빠른 피드백 |

- 백엔드 테스트는 **단위/슬라이스로 통일**한다. DB가 필요한 슬라이스는 H2 인메모리로 돈다.
- Testcontainers 기반 통합 테스트는 **도입하지 않는다**(실행 비용·복잡도 대비 효용이 낮다고 판단해 제거). API 레벨 검증은 Postman으로 대신한다.

## 기능별 최소 기준

기능 완료(개발 흐름 ⑤ PR) 전 최소:

```
- 단위 테스트: 성공 1 + 주요 실패 2 (권한·검증·상태 등)
- API 테스트(Postman)로 실제 요청 성공/실패 확인
```

## 로컬 실행

```bash
cd backend
./gradlew test        # 단위/슬라이스 (H2) — CI와 동일
```

## 프론트

CI(`ci.yml`)의 `frontend-check`가 **lint + build**로 프론트 무결성을 검증한다(별도 유닛 테스트는 현재 없음).

## 후속

- 실 DB·전 계층 검증이 필요해지면 통합 테스트 방식(예: Testcontainers 재도입 or 별도 환경)을 ADR로 재검토한다.
- 프론트 컴포넌트/E2E 테스트 도입 여부 검토.
