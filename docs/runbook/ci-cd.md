# CI/CD 파이프라인

> 최종 수정일: 2026-07-07 · 상태: draft

GitHub Actions 워크플로우. 정의는 [`.github/workflows/`](../../.github/workflows/).

```mermaid
graph LR
    fpush["feature/* push"] --> ci
    pr["PR → develop"] --> ci["CI (ci.yml)<br/>테스트 게이트"]
    ci -->|"통과"| merge["develop 머지"]
    merge --> cd["CD (cd-app.yml)<br/>빌드·push·배포"]
    cd -->|"OIDC push"| ecr[(Amazon ECR)]
    cd -->|"SSH pull·up"| ec2["앱 EC2"]
    ecr -.->|"pull"| ec2
```

## CI — `ci.yml` (테스트 게이트)

**트리거**: `feature/*` push(솔로 작업 중 즉시 피드백) · `develop` 대상 PR(머지 전 최종 게이트). `develop` push는 제외(그건 CD 담당). 수동 실행 가능.

| Job | 내용 |
| --- | --- |
| **backend-test** | JDK 21 · `./gradlew test` — **단위/슬라이스 테스트(H2)**. 실패 시 리포트 업로드 |
| **frontend-check** | Node 22 · deps 설치 · **lint · build** |

> 같은 ref의 이전 실행은 자동 취소(러너 낭비 방지). 테스트 계층 전략은 [conventions/testing.md](../conventions/testing.md) 참고.

## CD — `cd-app.yml` (이미지 빌드 & push & 배포)

**트리거**: `develop` push 중 `backend/**`·`frontend/**` 변경 시. 수동 실행 가능.

1. **changes** — `dorny/paths-filter`로 어떤 앱이 바뀌었는지 감지(바뀐 것만 빌드).
2. **build-backend / build-frontend** — 변경된 앱 이미지를 빌드해 **ECR로 push**.
   - AWS 인증: **OIDC**(`AWS_ROLE_ARN` 시크릿, `id-token: write` 권한). 정적 키 미사용.
   - 태그: `latest` + 커밋 SHA(`${{ github.sha }}`).
   - 리전: `ap-northeast-2`.
   - 빌드 가속: Buildx + GitHub Actions 캐시(`cache-from/to: type=gha`).
3. **deploy** — 앱 EC2에 **SSH 접속(`appleboy/ssh-action`)** 해 재배포.
   - `docker login`(ECR) → `docker compose pull` → `up -d` → `image prune -f`.
   - 빌드 job 중 **하나라도 성공하면**(=새 이미지 있으면) 실행(`if: always() && (build-backend 성공 || build-frontend 성공)`).
   - 접속: `EC2_APP_HOST` · `EC2_SSH_KEY` 시크릿(사용자 `ubuntu`, 작업 디렉터리 `~/app`).

> develop 머지 → 이미지 빌드·push → 앱 EC2 자동 재배포까지 **엔드투엔드 자동화**돼 있다. `:latest` 태그를 pull하므로 특정 SHA 롤백·헬스체크는 아직 수동(후속 개선 여지).

## 필요한 시크릿 / 설정

| 항목 | 용도 |
| --- | --- |
| `AWS_ROLE_ARN` | GitHub Actions OIDC로 assume할 IAM Role |
| (IAM Role 권한) | ECR push |
| `EC2_APP_HOST` | 배포 대상 앱 EC2 호스트(SSH) |
| `EC2_SSH_KEY` | 앱 EC2 SSH 개인키 |
| ECR 리포지토리 | `dongnemarket-app` · `dongnemarket-next` |

## 브랜치 전략과의 연결

`feature/*` → (CI 통과) → `develop` PR 머지 → (CD가 ECR push → 앱 EC2 자동 재배포) 흐름. 브랜치·머지 규칙은 [conventions/git-collaboration.md](../conventions/git-collaboration.md).
