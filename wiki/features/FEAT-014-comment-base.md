---
id: FEAT-014
type: feature
status: done
author: Crispy-down
date: 2026-06-25
related: []
tags: [comment, backend]
pr: 14
---

## 무엇을 / 왜

[Comment] 댓글 엔티티 & DTO 골격 구현

## 어떻게 (구현 요약)

- `Comment` Entity: `comments` 테이블 매핑, 소프트 삭제 `deleted_at`, `updateContent()` / `softDelete()` / `isDeleted()` 메서드
- `CommentRepository`: `findAllByProductIdAndDeletedAtIsNull`, `findByIdAndDeletedAtIsNull`
- `CommentCreateRequest` DTO: `@NotBlank`, `@Size(max=500)` 검증
- `CommentUpdateRequest` DTO: `@NotBlank`, `@Size(max=500)` 검증
- `CommentResponse` DTO: `from(Comment)` 정적 팩토리

> Member/Product 엔티티가 아직 없으므로 `memberId`, `productId`를 Long 타입 컬럼으로 관리합니다.
> Service 레이어(PR-2~5)는 해당 엔티티가 develop에 머지된 후 진행합니다.

**검증**

- `CommentRepositoryTest` 3건 전체 통과 (`BUILD SUCCESSFUL`)
  - 성공: 댓글 저장/조회 및 소프트 삭제 동작 확인
  - 실패 1: 삭제된 댓글 단건 조회 시 빈 Optional 반환
  - 실패 2: 존재하지 않는 댓글 조회 시 빈 Optional 반환

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/comment/dto/CommentCreateRequest.java` (+15/-0)
- `backend/src/main/java/com/dongnemarket/comment/dto/CommentResponse.java` (+43/-0)
- `backend/src/main/java/com/dongnemarket/comment/dto/CommentUpdateRequest.java` (+15/-0)
- `backend/src/main/java/com/dongnemarket/comment/entity/Comment.java` (+55/-0)
- `backend/src/main/java/com/dongnemarket/comment/repository/CommentRepository.java` (+14/-0)
- `backend/src/test/java/com/dongnemarket/comment/CommentRepositoryTest.java` (+63/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- Postman 테스트: 이번 PR은 API 엔드포인트 없음 (엔티티/DTO 골격만) — PR-2부터 포함 예정
- `CommentCreateRequest` / `CommentUpdateRequest`가 현재 동일 구조 — 전체 기능 완료 후 리팩토링 예정

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/14
- 이슈: 없음
