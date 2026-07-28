---
id: FEAT-136
type: feature
status: done
author: jomin4
date: 2026-07-06
related: []
tags: [backend, infra, docs]
pr: 136
---

## 무엇을 / 왜

인프라세팅

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- 문서 — `docs/deployment/` 4개
- 인프라·CI — `.github/workflows/` 2개, `infra/` 1개, `infra/cloud/` 5개

## 건드린 파일

- `.github/workflows/cd-app.yml` (+97/-0)
- `.github/workflows/ci.yml` (+74/-0)
- `backend/.dockerignore` (+4/-0)
- `backend/Dockerfile` (+17/-4)
- `docs/deployment/05-aws-cicd-design.md` (+165/-0)
- `docs/deployment/06-ec2-app-deploy.md` (+131/-0)
- `docs/deployment/07-aws-operations-log.md` (+109/-0)
- `docs/deployment/README.md` (+3/-0)
- `infra/README.md` (+24/-0)
- `infra/cloud/app/.env.example` (+13/-0)
- `infra/cloud/app/docker-compose.yml` (+42/-0)
- `infra/cloud/app/nginx.conf` (+31/-0)
- `infra/cloud/db/.env.example` (+4/-0)
- `infra/cloud/db/docker-compose.yml` (+32/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/136
- 이슈: 없음
