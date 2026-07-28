---
id: FEAT-118
type: feature
status: done
author: jomin4
date: 2026-07-03
related: []
tags: [backend, frontend, docs, chore]
pr: 118
---

## 무엇을 / 왜

local-deploy 환경(compose/nginx/observability/edge) + 프로파일 정리 + 문서

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- 리소스 — `application-dev.yml` (+16), `application-local.yml` (+17), `application-operator.yml` (+0), `application-prod.yml` (+0), `application.yml` (+14)
- 문서 — `docs/` 1개, `docs/deployment/` 3개

## 건드린 파일

- `.env.example` (+0/-0)
- `README.md` (+93/-0)
- `backend/docker-compose.prod.yml` (+0/-45)
- `backend/docker-compose.yml` (+0/-83)
- `backend/src/main/resources/application-dev.yml` (+16/-0)
- `backend/src/main/resources/application-local.yml` (+17/-0)
- `backend/src/main/resources/application-operator.yml` (+0/-11)
- `backend/src/main/resources/application-prod.yml` (+0/-33)
- `backend/src/main/resources/application.yml` (+14/-25)
- `docker-compose.yml` (+138/-0)
- `docs/README.md` (+1/-1)
- `docs/deployment/03-local-deploy.md` (+180/-0)
- `docs/deployment/04-local-deploy-build.md` (+169/-0)
- `docs/deployment/README.md` (+3/-1)
- `frontend/.dockerignore` (+9/-0)
- … 외 8개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/118
- 이슈: 없음
