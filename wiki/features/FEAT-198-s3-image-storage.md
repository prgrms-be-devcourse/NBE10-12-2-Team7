---
id: FEAT-198
type: feature
status: done
author: horangnabi97
date: 2026-07-08
related: []
tags: [global, product, report, backend, infra, docs]
pr: 198
---

## 무엇을 / 왜

report(`EvidenceImageStorageService`)와 product(`ProductImageStorageService`)가 완전히 중복된 로컬 디스크 저장 로직을 각자 갖고 있었고, 로컬 파일 기반 저장 방식은 배포 환경에서 영속성 관리가 어려워 S3 기반으로 일원화했습니다. 다만 팀 내 온프레미스/로컬 파일 저장 운용도 필요해, 저장소를 프로파일이 아닌 설정값으로 선택하도록 설계했습니다.

## 어떻게 (구현 요약)

- 신고 증빙 이미지·상품 이미지 저장소를 `file.storage.type` 설정값(local/s3)으로 선택하도록 전환
- 프로파일이 아니라 이 값 하나로 구현체가 갈려서, `test`뿐 아니라 온프레미스/로컬 배포도 로컬 디스크를 그대로 쓸 수 있음
- 운영 EC2는 `FILE_STORAGE_TYPE=s3`로 Amazon S3 사용
- `global/storage`에 공통 모듈 신설, report/product의 기존 서비스는 시그니처를 유지한 채 이 모듈에 위임하는 얇은 어댑터로 전환

**검증**

- `./gradlew test`: 518/518 통과 (외부 AWS 불필요)
- `S3FileStorageServiceTest`: Mockito로 `S3Client` 모킹, putObject/getObject 호출 파라미터 및 예외 변환(`NoSuchKeyException`→`StorageFileNotFoundException` 등) 검증 (LocalStack/Testcontainers는 이번 범위 제외)
- `LocalFileStorageServiceTest`: 실제 파일 I/O로 저장/조회/경로 격리/path-traversal 방지 검증
- report/product 기존 테스트(`EvidenceImageStorageServiceTest`, `ProductImageStorageServiceTest`, `ProductImageControllerTest` 등) 어댑터 전환에 맞춰 생성자/경로만 조정, 통과

## 건드린 파일

- `.env.example` (+6/-0)
- `backend/build.gradle` (+4/-0)
- `backend/src/main/java/com/dongnemarket/global/storage/FileStorageService.java` (+21/-0)
- `backend/src/main/java/com/dongnemarket/global/storage/LocalFileStorageService.java` (+77/-0)
- `backend/src/main/java/com/dongnemarket/global/storage/S3Config.java` (+29/-0)
- `backend/src/main/java/com/dongnemarket/global/storage/S3FileStorageService.java` (+86/-0)
- `backend/src/main/java/com/dongnemarket/global/storage/StorageException.java` (+12/-0)
- `backend/src/main/java/com/dongnemarket/global/storage/StorageFileNotFoundException.java` (+12/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductImageStorageService.java` (+17/-39)
- `backend/src/main/java/com/dongnemarket/report/service/EvidenceImageStorageService.java` (+15/-40)
- `backend/src/main/resources/application-local.yml` (+0/-4)
- `backend/src/main/resources/application.yml` (+11/-4)
- `backend/src/test/java/com/dongnemarket/global/storage/LocalFileStorageServiceTest.java` (+82/-0)
- `backend/src/test/java/com/dongnemarket/global/storage/S3FileStorageServiceTest.java` (+118/-0)
- `backend/src/test/java/com/dongnemarket/product/controller/ProductImageControllerTest.java` (+1/-1)
- … 외 6개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- EC2 S3 배포 환경에서는 로컬 볼륨 대신 S3 환경변수를 사용합니다

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/198
- 이슈: 없음
