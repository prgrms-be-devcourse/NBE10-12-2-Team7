---
id: FEAT-222
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-23
related: []
tags: [admin, manner, product, backend]
pr: 222
---

## 무엇을 / 왜

신고 목록 신뢰도 가중 정렬과 대칭되는 기능으로, 매너온도가 낮은 판매자의 상품이 계속 상단에 노출되는 문제를 해결했다.

Closes #221

## 어떻게 (구현 요약)

- `ProductService`에 `demoteLowTrustSellers()` 추가, 상품 목록 / 카테고리별 목록 / 검색 결과 3곳에 모두 적용
- `MannerScore`에 공용 저신뢰 임계치 상수(`LOW_TRUST_THRESHOLD = 20.0`)를 추가하고,
  `AdminMannerService`도 이 상수를 참조하도록 정리
- 상품 목록(`getProducts`)은 커서 기반 페이지네이션이라, `nextCursor`는 항상 원래 id 내림차순 조회 결과로 계산하고,
  신뢰도 하락 정렬은 확정된 페이지 내부 노출 순서에만 적용해 페이지네이션 정합성을 유지

**검증**

- 단위 테스트: 3개 메서드 각각에 저신뢰 판매자 상품이 뒤로 밀리는지 검증하는 테스트 추가,
  전체 테스트 스위트(`./gradlew test`) 통과
- 실 서버 기동 후 curl로 라이브 검증: 판매자 매너온도를 15.0으로 직접 설정 →
  `/api/products`, `/api/products/search`에서 해당 판매자 상품이 실제로 맨 뒤로 밀려나는 것을 확인함
  (검증 후 테스트 데이터는 정리함)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/admin/service/AdminMannerService.java` (+2/-4)
- `backend/src/main/java/com/dongnemarket/manner/entity/MannerScore.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/product/service/ProductService.java` (+53/-17)
- `backend/src/test/java/com/dongnemarket/product/service/ProductServiceTest.java` (+68/-1)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 상품(Product) 도메인은 한상민님 담당이나, 사전 협의를 거쳐 본인이 직접 구현하고
  변경 diff를 리뷰용으로 전달하는 방식으로 진행함

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/222
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/221
