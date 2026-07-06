# AWS 작업 로그 & 관리 대장

CI/CD 구축 중 실제로 AWS에 실행한 작업 기록. "무엇을 바꿨나 / 지금 상태 / 되돌리는 법"을
남겨 유령 리소스(과금)·보안 구멍을 방지한다. 계정 `773505972890`, 리전 `ap-northeast-2`.

> 인프라 개요·접속법은 메모리 `aws-infra-topology` 및 [06-ec2-app-deploy](06-ec2-app-deploy.md) 참고.

---

## 1. 현재 리소스 상태 (스냅샷)

| 리소스 | ID | 상태 | 비고 |
|---|---|---|---|
| 앱 EC2 (MarketON) | `i-0b584b3f2453bbed1` | 실행 중 | pub 43.203.245.141 / priv 10.0.2.155, SG `sg-00f5452805b1bbd96` |
| DB EC2 (RDB) | `i-0fd200a9c778029d7` | 실행 중 | priv 10.0.135.59(퍼블릭 없음), SG `sg-042f14d60bb233588` |
| VPC | `vpc-033cfae137ae96d6c` | - | IGW `igw-02184b52d851c10ca` |
| RDB 서브넷 | `subnet-043875169e703c474` | - | 라우트테이블 `rtb-0b1a435f3ea8366a2` (RDB 단독) |
| MySQL 컨테이너 | `dongne-mysql` | Up(healthy) | RDB 위 Docker, DB `dongne_market`, 포트 3306 |

---

## 2. 영구적으로 남은 변경 (← 관리 대상)

이번 세션에서 만들어 **지금도 유지되는** 것들. 나중에 정리·점검 대상.

| # | 변경 | 명령/위치 | 관리 포인트 |
|---|---|---|---|
| P1 | RDB SG에 3306 인바운드 허용(앱 SG만) | `authorize-security-group-ingress` | 정상. 앱↔DB 통신용 |
| P2 | `dongne_ci_deploy.pub`를 두 EC2 authorized_keys에 등록 | SSH | 배포 키. 유지 |
| P3 | RDB에 Docker 설치 + MySQL 컨테이너 | SSH | 유지(서비스 본체) |
| P4 | 로컬 PC에 IAM 액세스키로 `aws configure` | `~/.aws/credentials` | ⚠️ **스크린샷 노출된 키 — 폐기·교체 필요** |
| P5 | ECR 리포 2개 (`dongnemarket-app`/`-next`) | `ecr create-repository` | 이미지 저장소 |
| P6 | `marketon-ec2-role`에 `AmazonEC2ContainerRegistryReadOnly` 추가 | `attach-role-policy` | MarketON의 ECR pull용 |
| P7 | OIDC 공급자 + Role `github-actions-ecr-push`(인라인 `ecr-push` 정책) | `create-open-id-connect-provider`, `create-role`, `put-role-policy` | GitHub Actions의 ECR push용. repo `NBE10-12-2-Team7`로 제한 |

### ⚠️ 정리 필요 항목
- **P4**: 세션 중 노출된 IAM 액세스키(`AKIA3IGEMPKNPQPR7OBN`)는 **IAM에서 비활성화·삭제 후 재발급** 권장.
- **RDB SSH(22)가 `0.0.0.0/0` 개방** 상태 → 본인 IP 또는 앱 SG로 좁히기 권장.

---

## 3. 임시로 했다가 되돌린 것 (기록용 — 현재 없음)

RDB에 Docker/이미지를 받기 위해 **egress를 잠깐 열고 닫음.** 아래는 **이미 원복 완료**.

| 순서 | 작업 | 원복 |
|---|---|---|
| 1 | `create-route 0.0.0.0/0 → igw` (rtb-0b1a435f3ea8366a2) | `delete-route` 완료 |
| 2 | `allocate-address` → `eipalloc-05803e64c94fb6129` | `release-address` 완료(과금 없음) |
| 3 | `associate-address` → `eipassoc-0ca2112bea56a3534` | `disassociate-address` 완료 |

→ 현재 RDB 라우트는 `10.0.0.0/16`(local) + S3 엔드포인트만. egress 차단(정상).

---

## 4. 재사용 명령어 모음

### RDB 접속 (MarketON 경유)
```bash
ssh -i ~/.ssh/dongne_ci_deploy -J ubuntu@43.203.245.141 ubuntu@10.0.135.59
```

### RDB egress 임시 개방 → 닫기 (Docker/이미지 갱신 필요 시)
```bash
R=ap-northeast-2; RTB=rtb-0b1a435f3ea8366a2; IGW=igw-02184b52d851c10ca; ENI=eni-006de8d9819968cf5
# --- 열기 ---
aws ec2 create-route --region $R --route-table-id $RTB --destination-cidr-block 0.0.0.0/0 --gateway-id $IGW
ALLOC=$(aws ec2 allocate-address --region $R --domain vpc --query AllocationId --output text)
ASSOC=$(aws ec2 associate-address --region $R --allocation-id $ALLOC --network-interface-id $ENI --query AssociationId --output text)
echo "ALLOC=$ALLOC ASSOC=$ASSOC"   # ← 닫을 때 필요, 반드시 기록
# --- (작업 수행) ---
# --- 닫기(반드시!) ---
aws ec2 disassociate-address --region $R --association-id $ASSOC
aws ec2 release-address --region $R --allocation-id $ALLOC
aws ec2 delete-route --region $R --route-table-id $RTB --destination-cidr-block 0.0.0.0/0
```

### RDB 키 재주입 (authorized_keys 날아갔을 때)
```bash
aws ec2-instance-connect send-ssh-public-key --region ap-northeast-2 \
  --instance-id i-0fd200a9c778029d7 --availability-zone ap-northeast-2a \
  --instance-os-user ubuntu --ssh-public-key "$(cat ~/.ssh/dongne_ci_deploy.pub)"
# 60초 안에 위 ssh 접속
```

### 상태 확인 (읽기 전용, 안전)
```bash
# 인스턴스 상태
aws ec2 describe-instances --region ap-northeast-2 \
  --instance-ids i-0b584b3f2453bbed1 i-0fd200a9c778029d7 \
  --query 'Reservations[].Instances[].{Name:Tags[?Key==`Name`]|[0].Value,State:State.Name,Pub:PublicIpAddress,Priv:PrivateIpAddress}' --output table
# EIP 유령 확인(과금 방지)
aws ec2 describe-addresses --region ap-northeast-2 --query 'Addresses[].{Ip:PublicIp,Assoc:AssociationId}' --output table
# RDB 인바운드 규칙
aws ec2 describe-security-groups --region ap-northeast-2 --group-ids sg-042f14d60bb233588 \
  --query 'SecurityGroups[0].IpPermissions' --output json
# ECR 이미지 목록
aws ecr describe-images --region ap-northeast-2 --repository-name dongnemarket-app --query 'imageDetails[].imageTags' --output json
```

---

## 5. 관리 체크리스트

- [ ] 노출된 IAM 액세스키(`AKIA3IGEMPKNPQPR7OBN`) 폐기 → 재발급
- [ ] RDB SSH(22) 소스를 `0.0.0.0/0`에서 좁히기
- [ ] `describe-addresses`로 유령 EIP 없는지 주기 확인(과금)
- [ ] DB 비밀번호는 각 EC2 `~/db`·`~/app`의 `.env`에만 보관(커밋 금지)
- [ ] (예정) 앱 EC2 IAM Role(ECR pull) 부여 — Stage 2
