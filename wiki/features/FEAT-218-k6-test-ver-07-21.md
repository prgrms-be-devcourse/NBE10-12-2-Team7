---
id: FEAT-218
type: feature
status: done
author: Crispy-down
date: 2026-07-23
related: []
tags: [infra]
pr: 218
---

## 무엇을 / 왜

[feat] k6 API 성능테스트 - 2차 프로젝트 기준

## 어떻게 (구현 요약)

2차 프로젝트 기준 k6 API 성능 테스트 스크립트 및 부하테스트 환경을 추가했습니다.

- **부하 테스트 스크립트** (`infra/onprem/loadtest/`)
  - 회원(`member-load.js`), 읽기(`read-load.js`), 쓰기(`write-load.js`)
  - 상품 목록 커서(`product-list-cursor.js`), 검색(`product-search.js`), 카테고리(`category-products.js`), 상세 hotrow(`product-detail-hotrow.js`)
  - 신고 목록 N+1(`report-list-n1.js`)
- **시드 데이터**: `seed-loadtest.sql`, `dummy-data.sql`
- **Grafana 대시보드**: 부하테스트용 프로비저닝(`monitoring/grafana/.../loadtest.json`) 및 datasource 설정
- `infra/onprem/docker-compose.yml` 관련 설정 보강

**검증**

100k 시드 기준 baseline(before) 측정 완료. 병목 3개 확인(cursor / search / category — ~100VU에서 Hikari 풀 포화).

## 건드린 파일

- `infra/onprem/docker-compose.yml` (+1/-0)
- `infra/onprem/loadtest/README.md` (+80/-0)
- `infra/onprem/loadtest/category-products.js` (+82/-0)
- `infra/onprem/loadtest/dummy-data.sql` (+60/-0)
- `infra/onprem/loadtest/member-load.js` (+369/-0)
- `infra/onprem/loadtest/product-detail-hotrow.js` (+92/-0)
- `infra/onprem/loadtest/product-list-cursor.js` (+85/-0)
- `infra/onprem/loadtest/product-search.js` (+85/-0)
- `infra/onprem/loadtest/read-load.js` (+97/-0)
- `infra/onprem/loadtest/report-list-n1.js` (+83/-0)
- `infra/onprem/loadtest/seed-loadtest.sql` (+207/-0)
- `infra/onprem/loadtest/write-load.js` (+60/-0)
- `infra/onprem/monitoring/grafana/provisioning/dashboards/dashboards.yml` (+12/-0)
- `infra/onprem/monitoring/grafana/provisioning/dashboards/loadtest.json` (+135/-0)
- `infra/onprem/monitoring/grafana/provisioning/datasources/datasource.yml` (+1/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- Grafana 대시보드 프로비저닝 경로 확인
- 시드 SQL 규모/실행 방식 리뷰

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/218
- 이슈: 없음
