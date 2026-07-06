# EC2 #1 (app) 수동 배포 런북

ECR에 올라온 이미지를 **EC2 #1에서 손으로 한 번** 띄워 배포 절차를 검증하는 단계(로드맵 8-4).
여기서 성공한 절차를 다음 단계(8-5)에서 GitHub Actions로 자동화한다.

> 전제: [8-3](05-aws-cicd-design.md) 완료 — ECR에 `dongnemarket-app:latest`,
> `dongnemarket-next:latest` 이미지가 이미 올라가 있어야 한다(Actions 수동 실행으로 확인).

---

## 0. 개념: 인증이 두 종류다

| 주체 | 동작 | 인증 방식 |
|---|---|---|
| GitHub Actions | ECR에 **push** | OIDC Role (8-3) |
| **EC2 #1** | ECR에서 **pull** | **EC2 IAM Role** (이 문서) |

같은 ECR이라도 "올리는 쪽"과 "받는 쪽"의 신원이 달라, EC2에는 **별도로 읽기 권한**을 준다.

---

## A. EC2에 Docker + Compose 설치 (1회)

Amazon Linux 2023 기준:

```bash
sudo dnf update -y
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker $USER        # sudo 없이 docker 쓰려면 (재로그인 필요)

# docker compose v2 플러그인
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

docker --version && docker compose version   # 확인
```

> 재로그인 후 `docker ps`가 sudo 없이 되면 그룹 적용 완료.

---

## B. EC2에 ECR pull 권한 부여 (1회)

**콘솔**: IAM → Roles → **Create role** → Trusted entity: **AWS service → EC2**
→ 권한 정책 **`AmazonEC2ContainerRegistryReadOnly`** 부착 → 역할 이름 예 `ec2-ecr-pull`.

그런 다음 EC2 콘솔에서 대상 인스턴스 →
**Actions → Security → Modify IAM role** → 방금 만든 역할 연결.

> 이 역할 덕분에 EC2 안에서 `aws ecr get-login-password`가 **키 없이** 동작한다.

---

## C. 배포 파일 배치 + .env 작성

EC2에는 소스 전체가 아니라 **`infra/cloud/app/` 안의 것만** 있으면 된다.

```bash
mkdir -p ~/app && cd ~/app
# 아래 2개 파일을 이 폴더에 둔다 (git clone 후 복사 / scp / 직접 작성 중 택1)
#   - docker-compose.yml   (리포 infra/cloud/app/docker-compose.yml)
#   - nginx.conf           (리포 infra/cloud/app/nginx.conf)

# .env 작성 (.env.example 참고, 실제 값으로)
cp /path/to/repo/infra/cloud/app/.env.example .env
vi .env
```

`.env`에서 반드시 실제 값으로 채울 것:

```bash
ECR_REGISTRY=<계정ID>.dkr.ecr.ap-northeast-2.amazonaws.com
IMAGE_TAG=latest
DB_URL=jdbc:mysql://<rds-endpoint>:3306/dongne_market?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=<긴 랜덤 문자열>
OLLAMA_BASE_URL=http://<접근가능주소>:11434   # 없으면 AI만 실패, 앱은 뜸
```

---

## D. ECR 로그인 → pull → up

```bash
cd ~/app
REGION=ap-northeast-2
REGISTRY=$(grep ECR_REGISTRY .env | cut -d= -f2)

# B의 IAM Role로 ECR 도커 로그인 (키 불필요)
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $REGISTRY

# 이미지 받아서 기동
docker compose --env-file .env pull
docker compose --env-file .env up -d
```

---

## E. 검증

```bash
docker compose ps                          # app/next/nginx 3개 Up 확인
curl -sf http://localhost/api/actuator/health   # {"status":"UP"} 기대
curl -sI http://localhost/                 # 프론트 200 기대
```

브라우저에서 **http://<EC2 퍼블릭 IP>** 접속 → 화면이 뜨면 성공.
(EC2 보안그룹에서 80 포트가 열려 있어야 한다.)

---

## 트러블슈팅

| 증상 | 원인/조치 |
|---|---|
| `docker login` 실패 | B의 IAM Role 미연결. EC2에 `ec2-ecr-pull` 붙었는지 확인 |
| `pull` 권한 오류 | 리포 이름/리전 불일치. `ECR_REGISTRY` 값과 이미지 이름 확인 |
| app 컨테이너가 죽음 | `docker compose logs app` — 대개 RDS 접속 실패(SG에서 3306을 EC2 SG에만 허용했는지) |
| 502 Bad Gateway | app/next가 아직 기동 중이거나 죽음. `docker compose ps`로 상태 확인 |
| RDS 연결 안 됨 | RDS 보안그룹 인바운드 3306 ← EC2 #1 SG 허용, `DB_URL` 엔드포인트 확인 |

---

## 다음 (8-5)

여기 **D 단계(로그인 → pull → up)**를 그대로 GitHub Actions의 deploy job으로 옮겨
`develop` 머지 시 자동 배포되게 한다. 즉 이 런북이 자동화의 "설계도"다.
