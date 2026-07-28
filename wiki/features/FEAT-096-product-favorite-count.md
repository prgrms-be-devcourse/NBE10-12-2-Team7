---
id: FEAT-096
type: feature
status: done
author: han95white
date: 2026-07-02
related: []
tags: [product, backend]
pr: 96
---

## 무엇을 / 왜

[Product] 관심 수 favoriteCount 이벤트 리스너 및 엣지 케이스 테스트 추가

## 어떻게 (구현 요약)

상품 관심 수(favoriteCount)를 Product 도메인에 추가하고, Favorite 이벤트를 수신해 관심 수를 갱신하는 리스너를 구현했습니다.

  추가로 favoriteCount 갱신 로직의 엣지/더티 케이스 테스트를 보강했습니다.

  - Product 엔티티에 favoriteCount 필드 추가
  - 상품 생성 시 favoriteCount 기본값 0 설정
  - ProductRepository에 원자적 UPDATE 기반 증가/감소 메서드 추가
  - FavoriteAddedEvent / FavoriteRemovedEvent 수신 리스너 추가
  - ProductResponse, ProductSummaryResponse에 favoriteCount 응답 필드 추가
  - Repository 단위 테스트 추가
  - 리스너 통합 테스트 추가
  - favoriteCount 엣지/더티 케이스 테스트 추가

  ## 변경 이유

  Favorite 도메인과 Product 도메인의 순환참조를 피하기 위해, FavoriteService가 ProductService를 직접 호출하지 않고 Spring Event 기반으로 관심 수를 갱신하도록 하기 위함입니다.

  카운트 갱신은 동시성 lost update를 피하기 위해 엔티티 로드 후 setter 변경 방식이 아니라 DB 원자 UPDATE 방식으로 처리했습니다.

  ## 작업 설명

  Product 엔티티에 favoriteCount를 추가하고, 상품 생성 시 0으로 초기화되도록 했습니다.

  ProductRepository에는 JPQL update 기반의 증가/감소 메서드를 추가했습니다. 감소는 favoriteCount > 0 조건을 걸어 0 아래로 내려가지 않도록 했습니다.

  ProductFavoriteCountHandler는 FavoriteAddedEvent를 수신하면 관심 수를 증가시키고, FavoriteRemovedEvent를 수신하면 관심 수를 감소시킵니다. 이 리스너는 ProductRepository만 의존하며 FavoriteService/
  FavoriteRepository/Favorite 엔티티를 참조하지 않습니다.

  응답 DTO인 ProductResponse, ProductSummaryResponse에는 Product 엔티티의 favoriteCount 컬럼 값을 그대로 노출했습니다. 목록 조회 시 favorite 테이블을 별도로 count하는 쿼리는 추가하지 않았습니다.

  ## 추가 테스트

  ### ProductRepositoryTest

  - `존재하지 않는 상품의 관심 수 증가를 요청해도 예외가 발생하지 않는다`
  - `존재하지 않는 상품의 관심 수 감소를 요청해도 예외가 발생하지 않는다`
  - `특정 상품의 관심 수만 증가하고 다른 상품은 변경되지 않는다`
  - `관심 수 증가와 감소를 여러 번 호출하면 최종 값이 정확히 반영된다`

  ### ProductFavoriteCountHandlerIntegrationTest

  - `FavoriteRemovedEvent를 0인 상품에 발행해도 관심 수가 음수가 되지 않는다`
  - `존재하지 않는 상품 이벤트를 발행해도 예외가 발생하지 않고 정상 상품은 변경되지 않는다`

  ## 의존 방향

  FavoriteService(PR B)
  → ApplicationEventPublisher
  → FavoriteAddedEvent / FavoriteRemovedEvent
  → ProductFavoriteCountHandler
  → ProductRepository
  → products.favorite_count

  Product 도메인은 FavoriteService, FavoriteRepository, Favorite 엔티티를 참조하지 않습니다.

  ## 주의 사항

  - @EventListener 동기 방식 사용
  - @TransactionalEventListener, AFTER_COMMIT, REQUIRES_NEW, @Async 사용하지 않음
  - FavoriteService/FavoriteRepository/Favorite 엔티티 수정 없음
  - 이벤트 record(global)는 수정하지 않고 import만 사용
  - favoriteCount 응답은 Product 컬럼 값을 그대로 사용
  - 목록 조회 시 favorite 테이블 count 쿼리 추가 없음
  - AdminProductResponse는 이번 PR에서 제외
  - test resources/sql fixture는 회의 결정에 따라 이번 PR에서 수정하지 않음
  - 트랜잭션 롤백 경계 테스트는 이번 범위에서 제외

  ## DB 공유 사항

  기존 로컬 MySQL에 products 테이블이 이미 있는 경우 아래 SQL 반영이 필요합니다.

  ```sql
  ALTER TABLE products ADD COLUMN favorite_count INT NOT NULL DEFAULT 0;

  기존 favorite 데이터 기준 재집계가 필요하면 별도 작업으로 아래 쿼리를 검토할 수 있습니다.

  UPDATE products p
  SET favorite_count = (
      SELECT COUNT(*)
      FROM favorites f
      WHERE f.product_id = p.id
  );

  ## 테스트

  통과:

  ./gradlew test --tests 'com.dongnemarket.product.repository.ProductRepositoryTest'

  ./gradlew integrationTest --tests 'com.dongnemarket.product.integration.ProductFavoriteCountHandlerIntegrationTest' --rerun-tasks

  참고:

  - 처음에 test/integrationTest를 병렬 실행했을 때 compileTestJava 단계에서 BaseIntegrationTest를 못 찾는 실패가 한 번 있었으나, 두 명령을 순차 실행하니 정상 통과했습니다.
  - 이번 검증은 최종적으로 위 두 명령을 순차 실행해 확인했습니다.

## 건드린 파일

- `backend/src/test/java/com/dongnemarket/product/integration/ProductFavoriteCountHandlerIntegrationTest.java` (+38/-0)
- `backend/src/test/java/com/dongnemarket/product/repository/ProductRepositoryTest.java` (+72/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/96
- 이슈: 없음
