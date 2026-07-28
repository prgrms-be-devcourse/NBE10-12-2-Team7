---
id: FEAT-007
type: feature
status: done
author: Crispy-down
date: 2026-06-25
related: []
tags: [favorite, backend]
pr: 7
---

## 무엇을 / 왜

[Favorite] 관심 상품 엔티티 & DTO 골격 구현 #5

## 어떻게 (구현 요약)

- `Favorite` Entity: `favorites` 테이블 매핑, `(member_id, product_id)` UNIQUE 제약 적용
  - `FavoriteRepository`: `existsByMemberIdAndProductId`, `findByMemberIdAndProductId`, `findAllByMemberId`
  - `FavoriteResponse` DTO: `from(Favorite)` 정적 팩토리 메서드

**검증**

- `FavoriteRepositoryTest` 3건 전체 통과 (`BUILD SUCCESSFUL`, 성공/실패 케이스 가정)
    - 성공: 관심 저장 및 DTO 변환
    - 실패 1: 중복 저장 시 UniqueConstraint 예외
    - 실패 2: 미존재 관심 조회 시 빈 Optional 반환

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/favorite/dto/FavoriteResponse.java` (+30/-0)
- `backend/src/main/java/com/dongnemarket/favorite/entity/Favorite.java` (+36/-0)
- `backend/src/main/java/com/dongnemarket/favorite/repository/FavoriteRepository.java` (+16/-0)
- `backend/src/test/java/com/dongnemarket/favorite/FavoriteRepositoryTest.java` (+52/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- Postman 테스트: 이번 PR은 API 엔드포인트 없음 (엔티티/DTO 골격만) — PR-2부터 포함 예정
   - Member, Product 엔티티가 아직 없어서 
   Favorite 작업은 Member , Product 엔티티가 생성 되고 난 뒤 진행하는게 좋을 것 같아보입니다. 
  비동기적으로 진행하기 위해 해당 PR 승인이 된다면 댓글 기능 구현을 시작해도 괜찮을까요?

  **> Member/Product 엔티티가 아직 없으므로 `memberId`, `productId`를 Long 타입 컬럼으로 관리합니다.
  > Service 레이어(PR-2~4)는 해당 엔티티가 develop에 머지된 후 진행합니다.**
 -> 현재는 이런 상황입니다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/7
- 이슈: 없음
