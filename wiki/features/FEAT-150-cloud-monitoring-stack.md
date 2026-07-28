---
id: FEAT-150
type: feature
status: done
author: jomin4
date: 2026-07-06
related: []
tags: [모니터링, prometheus, loki, grafana, infra]
pr: 150
---

> ⚠️ **이 문서는 PR 본문이 비어 있어 변경 파일·커밋에서 역구성했다.** 의도·트레이드오프는 추정이 아니라 공란으로 두었다. 원문 근거는 `docs/deployment/09-monitoring-ec2-setup.md`.

## 무엇을 / 왜

클라우드(AWS) 환경에 관측 스택을 세웠다. 메트릭은 Prometheus, 로그는 Loki, 대시보드는 Grafana로 묶고, 앱 EC2에는 로그 수집기(Promtail)를 붙여 모니터링 EC2로 로그를 보내는 구성이다.

## 어떻게 (구현 요약)

- **모니터링 EC2** — `infra/cloud/monitoring/`에 Prometheus·Loki·Grafana를 띄우는 compose 구성 추가. Grafana 데이터소스는 provisioning으로 자동 등록해 수동 설정을 없앴다.
- **앱 EC2** — `infra/cloud/app/`의 compose에 Promtail을 추가해 앱 로그를 모니터링 쪽 Loki로 전송.
- 함께 들어간 변경: 앱 쪽 MAIL 환경변수와 증빙 이미지 볼륨 설정(`.env.example`, `docker-compose.yml`).
- 구축 절차는 `docs/deployment/09-monitoring-ec2-setup.md`에 138줄 분량으로 별도 문서화.

## 건드린 파일

- `infra/cloud/monitoring/docker-compose.yml` (+51/-0)
- `infra/cloud/monitoring/loki-config.yml` (+30/-0)
- `infra/cloud/monitoring/prometheus.yml` (+11/-0)
- `infra/cloud/monitoring/grafana/provisioning/datasources/datasource.yml` (+12/-0)
- `infra/cloud/monitoring/.env.example` (+3/-0)
- `infra/cloud/app/docker-compose.yml` (+24/-4)
- `infra/cloud/app/promtail-config.yml` (+19/-0)
- `infra/cloud/app/.env.example` (+7/-2)
- `docs/deployment/09-monitoring-ec2-setup.md` (+138/-0)

## 결정과 트레이드오프

- PR 본문이 비어 있어 확인 불가. (앱과 모니터링을 별도 EC2로 분리한 이유, Prometheus·Loki 조합을 고른 이유가 기록되지 않음)

## 남은 이슈 / 후속 작업

- 이 PR로 관측 스택이 **클라우드 전용**이 되면서, 로컬 관측 스택은 이후 별도 브랜치(`fix/remove-local-monitoring`)에서 제거가 시도됐다. 해당 브랜치는 미머지 상태로 폐기됐고, 인프라 경계 정리는 `docs/adr/0004-infra-boundary.md`로 이어진다.

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/150
- 이슈: 없음
- 구축 절차: `docs/deployment/09-monitoring-ec2-setup.md`
