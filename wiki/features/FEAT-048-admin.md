---
id: FEAT-048
type: feature
status: done
author: jomin4
date: 2026-06-26
related: []
tags: [global, backend]
pr: 48
---

> ⚠️ **이 문서는 PR 본문이 비어 있어 제목·변경 파일에서 역구성했다.** 의도·트레이드오프는 추정하지 않고 공란으로 두었다.

## 무엇을 / 왜

global security 변경

## 어떻게 (구현 요약)

> PR 본문에 구현 설명이 없어 **변경 파일에서 구조만 도출**했다. 의도·근거는 기록되지 않음.

- `global` 도메인 — JwtAuthenticationEntryPoint(기타, 수정), JwtAuthenticationFilter(기타, 수정)
- 테스트 — 1개 파일 (+33줄)

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/global/security/jwt/JwtAuthenticationEntryPoint.java` (+2/-1)
- `backend/src/main/java/com/dongnemarket/global/security/jwt/JwtAuthenticationFilter.java` (+9/-3)
- `backend/src/test/java/com/dongnemarket/global/security/SecurityPolicyTest.java` (+33/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/48
- 이슈: 없음
