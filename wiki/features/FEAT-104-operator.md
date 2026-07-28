---
id: FEAT-104
type: feature
status: done
author: jomin4
date: 2026-07-03
related: []
tags: [global, backend]
pr: 104
---

## 무엇을 / 왜

인프라 환경구성

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `global` 도메인 — SecurityConfig(기타, 신규)
- 리소스 — `application-operator.yml` (+11), `application.yml` (+11)

## 건드린 파일

- `backend/.env.example` (+10/-0)
- `backend/build.gradle` (+1/-1)
- `backend/docker-compose.prod.yml` (+1/-0)
- `backend/docker-compose.yml` (+66/-5)
- `backend/monitoring/grafana/provisioning/datasources/datasource.yml` (+8/-0)
- `backend/monitoring/prometheus.yml` (+15/-0)
- `backend/src/main/java/com/dongnemarket/global/security/SecurityConfig.java` (+3/-0)
- `backend/src/main/resources/application-operator.yml` (+11/-0)
- `backend/src/main/resources/application.yml` (+11/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/104
- 이슈: 없음
