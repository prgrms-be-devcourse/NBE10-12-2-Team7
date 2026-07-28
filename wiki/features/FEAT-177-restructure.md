---
id: FEAT-177
type: feature
status: done
author: jomin4
date: 2026-07-07
related: []
tags: [infra, docs]
pr: 177
---

## 무엇을 / 왜

docs 폴더를 **Docs-as-Code**(코드와 함께 관리되는 기술 문서만) 구조로 재편하고, AI 에이전트가 프로젝트를 이해·개발 프로세스를 따르도록 하는 진입점을 신설합니다. 목표는 "AI로 개발은 빠른데 문서가 최신화 안 되는" 드리프트를 프로세스로 막는 것.

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- 문서 — `docs/` 1개, `docs/adr/` 2개, `docs/ai/` 7개, `docs/api/` 1개, `docs/architecture/` 5개, `docs/conventions/` 1개, `docs/getting-started/` 1개, `docs/postman/` 30개, `docs/runbook/` 1개
- 인프라·CI — `.github/` 1개, `infra/` 1개

## 건드린 파일

- `.github/pull_request_template.md` (+43/-0)
- `AGENTS.md` (+106/-0)
- `README.md` (+11/-6)
- `docs/README.md` (+39/-61)
- `docs/adr/0001-adopt-docs-as-code-structure.md` (+45/-0)
- `docs/adr/README.md` (+40/-0)
- `docs/ai/00-ai-common-rules.md` (+0/-376)
- `docs/ai/01-auth-member-agent.md` (+0/-39)
- `docs/ai/02-product-category-agent.md` (+0/-187)
- `docs/ai/03-favorite-comment-agent.md` (+0/-40)
- `docs/ai/04-report-agent.md` (+0/-38)
- `docs/ai/05-admin-common-agent.md` (+0/-124)
- `docs/ai/ai-native-collaboration-scenario.md` (+0/-87)
- `docs/api/README.md` (+52/-0)
- `docs/architecture/01-context.md` (+48/-0)
- … 외 38개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/177
- 이슈: 없음
