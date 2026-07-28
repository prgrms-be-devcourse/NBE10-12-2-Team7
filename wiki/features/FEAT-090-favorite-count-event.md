---
id: FEAT-090
type: feature
status: done
author: Crispy-down
date: 2026-07-02
related: []
tags: [찜, 이벤트, 트랜잭션, 카운트, backend]
pr: 90
---

## 무엇을 / 왜

찜 등록·취소가 일어날 때 `Product.favoriteCount`를 갱신해야 하는데, 찜 도메인이 상품 도메인을 직접 건드리면 패키지 경계가 무너진다. 그래서 찜 쪽은 **이벤트만 발행**하고, 카운트 증감은 별도 리스너(PR C)가 맡는 구조로 나눴다. 이 PR은 그중 **발행 배선(PR B)**만 담당한다.

## 어떻게 (구현 요약)

- `FavoriteService`에 `ApplicationEventPublisher`를 주입.
- `add()` 성공 시 `FavoriteAddedEvent(productId)`, `remove()` 성공 시 `FavoriteRemovedEvent(productId)` 발행.
- 발행을 `@Transactional` **안**에서 수행 → 같은 트랜잭션의 동기 리스너가 카운트를 증감하고, 실패하면 찜 저장/삭제와 **함께 롤백**된다.
- `add()`의 발행은 `catch(DataIntegrityViolationException)` **바깥**에 배치.

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/favorite/service/FavoriteService.java` (+15/-3)
- `backend/src/test/java/com/dongnemarket/favorite/service/FavoriteServiceTest.java` (+56/-0)

## 결정과 트레이드오프

- **발행을 트랜잭션 안에 둔 이유**: 카운트와 찜 레코드의 무결성을 트랜잭션 하나로 묶기 위해서. `@TransactionalEventListener(AFTER_COMMIT)`나 `@Async`를 쓰면 롤백 동반 보장이 깨지므로, **리스너는 순수 `@EventListener`(동기·same-tx)여야 한다**는 계약이 붙는다.
- **발행을 catch 블록 밖에 둔 이유**: 안에 두면 리스너에서 터진 예외가 중복 등록 race condition으로 오분류되어 `FAVORITE_ALREADY_EXISTS`로 둔갑한다.
- 이벤트 record 자체는 이 PR이 아니라 선행 PR #89에서 추가됨.

## 남은 이슈 / 후속 작업

- same-tx 롤백 동반 무결성은 mock 기반 단위 테스트로 검증 불가 → 발행↔리스너 연동 통합 테스트는 PR C/통합 단계에서 커버 예정.
- 카운트가 실제로 화면에 반영되려면 리스너·`favoriteCount` 컬럼 마이그레이션(PR C)이 함께 있어야 한다. 두 PR은 독립 머지 가능하지만 기능은 둘 다 있어야 완성.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/90
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/86
- 협업 규약: `docs/design/favorite-comment-count-collaboration.md`
