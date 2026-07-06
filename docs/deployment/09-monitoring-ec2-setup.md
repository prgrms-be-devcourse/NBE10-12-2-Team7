# 모니터링 EC2 세팅 총정리 (콘솔 · CLI)

클라우드 통합 모니터링(Prometheus·Loki·Grafana) EC2를 만드는 전 과정.
**웹 콘솔 방식**과 **AWS CLI 한방 스크립트** 두 가지로 정리한다.

## 고정값 (이 프로젝트 기준)

| 이름 | 값 |
|---|---|
| 리전 | `ap-northeast-2` |
| VPC | `vpc-033cfae137ae96d6c` (marketon-vpc) |
| 공용 서브넷 | `subnet-082d1472f7ee760a2` |
| 앱 SG | `sg-00f5452805b1bbd96` (marketon-security-group) |
| 앱 프라이빗 IP | `10.0.2.155` (Prometheus scrape 대상) |
| 내 IP | `121.88.175.243/32` (SSH·필요시 갱신) |
| AMI | `ami-0344289fbfd3bb8f0` (Ubuntu 24.04) |
| 인스턴스 | t3.small · 30GB gp3 |
| 결과 | 퍼블릭 `3.38.246.51` / 프라이빗 `10.0.0.130` |

---

## A. 웹 콘솔 방식

### A-1. 보안그룹 `monitoring-sg` 생성
EC2 → 네트워크 및 보안 → 보안 그룹 → **보안 그룹 생성**
- 이름 `monitoring-sg`, VPC `marketon-vpc`
- 인바운드 (설명은 **영문만**):

| 유형 | 포트 | 소스 |
|---|---|---|
| SSH | 22 | 내 IP |
| 사용자 지정 TCP | 3001 | `0.0.0.0/0` (Grafana 팀 공유) |
| 사용자 지정 TCP | 3100 | `marketon-security-group` (Loki) |

### A-2. 앱 SG에 Prometheus 허용
보안 그룹 → `marketon-security-group` → 인바운드 편집 → 규칙 추가
- 사용자 지정 TCP, `8080`, 소스 `monitoring-sg`

### A-3. 인스턴스 시작
EC2 → 인스턴스 → **인스턴스 시작**
- 이름 `Monitoring` · AMI Ubuntu 24.04 · t3.small
- 키 페어: 새로 생성 `monitoring` (.pem 다운로드·보관)
- **네트워크 편집**: 서브넷 = **공용(`subnet-082d...`)** · 퍼블릭 IP **활성화** · 방화벽 = 기존 **`monitoring-sg`**
- 스토리지 30GB gp3
- 고급 → 사용자 데이터:
  ```bash
  #!/bin/bash
  curl -fsSL https://get.docker.com | sh
  usermod -aG docker ubuntu
  ```

### A-4. 스택 배포 (로컬 터미널)
```bash
scp -i ~/Desktop/monitoring.pem -r infra/cloud/monitoring ubuntu@3.38.246.51:~/
ssh -i ~/Desktop/monitoring.pem ubuntu@3.38.246.51 \
  "cd ~/monitoring && printf 'GRAFANA_ADMIN_USER=admin\nGRAFANA_ADMIN_PASSWORD=%s\n' \$(openssl rand -hex 16) > .env && \
   sudo docker compose --env-file .env up -d && cat .env"
```
→ 출력된 `.env`의 비밀번호로 `http://3.38.246.51:3001` 접속.

---

## B. AWS CLI 한방 스크립트

> 로컬에 `aws configure` 완료 전제. 리포 루트에서 실행.

```bash
R=ap-northeast-2
VPC=vpc-033cfae137ae96d6c
SUBNET=subnet-082d1472f7ee760a2
APP_SG=sg-00f5452805b1bbd96
MYIP=121.88.175.243/32
AMI=ami-0344289fbfd3bb8f0

# 0) 키페어 생성 (CLI로 만들 때. 콘솔로 이미 있으면 생략)
aws ec2 create-key-pair --region $R --key-name monitoring \
  --query KeyMaterial --output text > ~/.ssh/monitoring.pem
chmod 400 ~/.ssh/monitoring.pem

# 1) monitoring-sg + 인바운드
MON_SG=$(aws ec2 create-security-group --region $R --group-name monitoring-sg \
  --description "Monitoring (prometheus/loki/grafana)" --vpc-id $VPC --query GroupId --output text)
aws ec2 authorize-security-group-ingress --region $R --group-id $MON_SG --protocol tcp --port 22   --cidr $MYIP
aws ec2 authorize-security-group-ingress --region $R --group-id $MON_SG --protocol tcp --port 3001 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --region $R --group-id $MON_SG --protocol tcp --port 3100 --source-group $APP_SG

# 2) 앱 SG에 Prometheus scrape 허용
aws ec2 authorize-security-group-ingress --region $R --group-id $APP_SG --protocol tcp --port 8080 --source-group $MON_SG

# 3) user-data (docker 자동설치)
cat > /tmp/mon-userdata.sh <<'EOF'
#!/bin/bash
curl -fsSL https://get.docker.com | sh
usermod -aG docker ubuntu
EOF

# 4) EC2 런치 (공용 서브넷 + 퍼블릭 IP + monitoring-sg)
IID=$(aws ec2 run-instances --region $R --image-id $AMI --instance-type t3.small \
  --key-name monitoring --subnet-id $SUBNET --security-group-ids $MON_SG --associate-public-ip-address \
  --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":30,"VolumeType":"gp3"}}]' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=Monitoring}]' \
  --user-data fileb:///tmp/mon-userdata.sh \
  --query 'Instances[0].InstanceId' --output text)
echo "InstanceId=$IID"

# 5) 퍼블릭 IP 확인 (실행·부팅 대기 후)
aws ec2 wait instance-running --region $R --instance-ids $IID
PUB=$(aws ec2 describe-instances --region $R --instance-ids $IID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)
echo "PublicIP=$PUB"

# 6) 스택 배포 (docker 설치 완료까지 1~2분 여유 후)
scp -i ~/.ssh/monitoring.pem -o StrictHostKeyChecking=accept-new -r infra/cloud/monitoring ubuntu@$PUB:~/
ssh -i ~/.ssh/monitoring.pem ubuntu@$PUB \
  "cd ~/monitoring && printf 'GRAFANA_ADMIN_USER=admin\nGRAFANA_ADMIN_PASSWORD=%s\n' \$(openssl rand -hex 16) > .env && \
   sudo docker compose --env-file .env up -d && cat .env"
```

> Windows(git-bash)에서 `--user-data fileb://` 경로 문제가 나면, user-data를 빼고 런치한 뒤
> SSH로 `curl -fsSL https://get.docker.com | sudo sh` 수동 설치해도 된다.

---

## 접속 & 확인

```bash
ssh -i ~/.ssh/monitoring.pem ubuntu@3.38.246.51        # 서버 접속
curl -s http://3.38.246.51:3001/api/health             # Grafana 헬스
```
- Grafana: `http://3.38.246.51:3001` (admin / `.env`의 비번)
- 데이터소스(Prometheus·Loki) 자동 프로비저닝됨
- 앱 배포 전엔 Prometheus 타깃 DOWN·로그 없음 = 정상 (앱 뜨면 자동 연결)

## 정리(삭제) 명령
```bash
aws ec2 terminate-instances --region ap-northeast-2 --instance-ids <IID>
aws ec2 delete-security-group --region ap-northeast-2 --group-id <MON_SG>   # 인스턴스 종료 후
```
