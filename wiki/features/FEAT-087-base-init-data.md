---
id: FEAT-087
type: feature
status: done
author: jomin4
date: 2026-07-01
related: [BUG-088]
tags: [global, backend]
pr: 87
---

## 무엇을 / 왜

관리자 콘솔/기능 검증을 위한 **개발·검증 전용 초기 시드 데이터**를 도입합니다. 현재 부팅 시 관리자 계정·카테고리만 시드되어 회원·상품·신고·댓글이 비어 있어 검증이 어렵던 문제를 해결합니다.

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `global` 도메인 — BaseInitDataInitializer(시더, 신규)

**검증**

- `@ConditionalOnProperty(app.seed.base-data=true)` — 기본 false, 로컬에서 플래그 켤 때만 실행 → 배포 서버 안전
- `@Profile("!test")` — 테스트 오염 방지(기존 테스트 영향 없음)
- `@EventListener(ApplicationReadyEvent)` — 모든 Runner(카테고리 시더) 완료 후 실행해 카테고리 존재 보장
- 센티넬(user01) 멱등 가드로 재부팅 중복 삽입 방지

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/global/init/BaseInitDataInitializer.java` (+155/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 설계 명세: docs/testing/09-base-init-data.md
- 카테고리 시더는 그대로 두고 이름 조회로 재사용(팀원 파일 미변경)

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/87
- 이슈: 없음
