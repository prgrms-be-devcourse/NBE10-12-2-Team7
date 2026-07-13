---
id: FEAT-209
type: feature
status: done
author: jomin4
date: 2026-07-10
related: []
tags: [관리자, 저장소, 고아파일, 이미지, 보안]
pr: 209
---

## 무엇을 / 왜
저장소(cloud=S3 / onprem·dev=로컬 디스크)에 쌓이는 **고아 이미지 파일**(파일은 실재하나 DB가 참조하지 않는 것)을 관리자가 조회·삭제하는 기능. 저장 계층이 **PUT-only**(삭제 경로 0건)라 고아가 단조 증가 → 특히 클라우드 S3 용량 누수. 발생 경로 3종: A 업로드 후 이탈, B 상품 이미지 교체 시 옛 파일 잔존(ProductService.updateProduct), C 신고 하드삭제(ReportService).

## 어떻게 (구현 요약)
3개 유닛으로 커밋:
- **Unit 1** — `FileStorageService`에 `list()`·`delete()` 추가 + 값 타입 `StoredObject(filename,sizeBytes,lastModified)`. Local=`Files.list`/`deleteIfExists`, S3=`ListObjectsV2`/`headObject`+`DeleteObject`. `delete`는 boolean 멱등, path-traversal 방어 재사용.
- **Unit 2** — `AdminStorageService.scanOrphans(graceHours)`: 디렉터리별 `list()` − DB 참조 집합 = 고아, grace period로 최근 파일 보호. 참조 조회는 **admin 소유 레포**(`AdminProductImageRepository` 신규, `AdminReportRepository`)로 두어 product/report 도메인 파일 무수정. URL→파일명은 마지막 `/` 뒤 추출.
- **Unit 3** — `AdminStorageController` `GET/DELETE /api/admin/storage/orphans`. `deleteOrphans`는 삭제 직전 재스캔으로 "지금도 고아"인 것만 삭제(참조/최근 보호), requested/deleted/skipped 집계. `ApiResponse` 봉투.

## 건드린 파일
- `backend/src/main/java/com/dongnemarket/global/storage/{FileStorageService,StoredObject,LocalFileStorageService,S3FileStorageService}.java`
- `backend/src/main/java/com/dongnemarket/admin/repository/{AdminProductImageRepository,AdminReportRepository}.java`
- `backend/src/main/java/com/dongnemarket/admin/dto/{OrphanFileResponse,OrphanScanResponse,OrphanDeleteRequest,OrphanDeleteResponse}.java`
- `backend/src/main/java/com/dongnemarket/admin/service/AdminStorageService.java`
- `backend/src/main/java/com/dongnemarket/admin/controller/AdminStorageController.java`
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (ADMIN_004 STORAGE_ORPHAN_DELETE_FAILED, ADMIN_005 INVALID_STORAGE_DIRECTORY)
- `backend/src/test/java/com/dongnemarket/admin/service/AdminStorageServiceTest.java` (Mockito 5)
- `backend/src/test/java/com/dongnemarket/admin/controller/AdminStorageControllerTest.java` (@SpringBootTest+MockMvc 4, 보안 200/401/403)

## 결정과 트레이드오프
- **RustFS 미도입** — onprem은 단일 노드 + 영속 볼륨 + `FILE_STORAGE_TYPE=local`로 이미 충분, cloud만 S3(휘발성 컨테이너라 필연). 고아 관리는 저장 백엔드 무관하게 성립해 별개로 진행.
- **관리자 sweep(백스톱) 방식** — 예방형(dereference 동기삭제·S3 Lifecycle)은 후속 feat로 분리, 이번은 "이미 쌓인 것 청소".
- **삭제 안전장치** — grace period(기본 24h) + 삭제 직전 재확인 + 화이트리스트 디렉터리 + 부분성공 집계 응답.
- 참조 조회를 admin 소유 레포에 둬 "담당 패키지 외 수정 없음" 원칙 준수.

## 남은 이슈 / 후속 작업
- 프론트 `/admin/storage` 화면 (후속 feat)
- 예방형 동기삭제(경로 B·C 원천차단) · S3 Lifecycle temp-prefix(경로 A 예방) — 별도 feat
- 소프트삭제 상품 이미지 회수(참조가 살아있어 고아 아님) — 별도 정책

## 링크
- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/209
- Notion 설계·구현·테스트: https://app.notion.com/p/399cd6a1d02381ac8eebf6d1d746d85b
- 브랜치: `feat/admin-storage-orphan`
