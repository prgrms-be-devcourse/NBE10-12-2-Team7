---
id: FEAT-180
type: feature
status: done
author: horangnabi97
date: 2026-07-07
related: [FEAT-179]
tags: [signup, frontend]
pr: 180
---

## 무엇을 / 왜

회원가입 약관 동의 UI 및 정책 문서 추가

## 어떻게 (구현 요약)

- 회원가입 화면에 전체동의 / 이용약관 / 개인정보 수집 및 이용 동의 체크박스(`AgreementSection`) 추가
- "보기" 클릭 시 실제 정책 문서를 모달로 표시(`PolicyDocumentView`). 개인정보처리방침은 필수 동의 체크박스가 아니라 별도 상시 열람 링크로 제공(ISMS 기준)
- 이용약관 / 개인정보 처리방침 / 개인정보 수집 및 이용 동의 / 회원탈퇴 및 개인정보 파기 정책 4종 문서 작성(`frontend/src/data/policies/`, 회원탈퇴 정책은 문서만 작성하고 이번 화면에는 연결하지 않음)
- 체크박스 클릭 시 리렌더 범위를 `memo`/`useCallback`/파생 state로 최소화, `.field input`/`.field label`이 자식 컴포넌트까지 새던 CSS 스코프 버그 수정

이 PR은 백엔드 PR(#179, `feature/auth_signup_agreement_backend`)과 짝을 이룹니다. `termsAgreed`/`personalInfoCollectionAgreed` 필드명은 백엔드 PR과 동일하게 맞췄습니다.

**검증**

- [x] `npx tsc --noEmit`, `npx eslint` 통과
- [x] 실제 브라우저(Playwright, 데스크톱 900px + 모바일 375px)로 체크박스 전체동의/개별동의 연동, 모달 열기/닫기(오버레이·ESC·닫기버튼), 모달 스크롤, 회원가입 요청 바디(`termsAgreed`/`personalInfoCollectionAgreed`) 확인

## 건드린 파일

- `frontend/src/app/signup/AgreementSection.module.css` (+173/-0)
- `frontend/src/app/signup/AgreementSection.tsx` (+179/-0)
- `frontend/src/app/signup/page.module.css` (+11/-5)
- `frontend/src/app/signup/page.tsx` (+52/-3)
- `frontend/src/components/PolicyDocumentView.module.css` (+57/-0)
- `frontend/src/components/PolicyDocumentView.tsx` (+53/-0)
- `frontend/src/data/policies/consent.ts` (+51/-0)
- `frontend/src/data/policies/index.ts` (+5/-0)
- `frontend/src/data/policies/privacy.ts` (+156/-0)
- `frontend/src/data/policies/terms.ts` (+130/-0)
- `frontend/src/data/policies/types.ts` (+21/-0)
- `frontend/src/data/policies/withdrawal.ts` (+78/-0)

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 해당 없음

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/180
- 이슈: 없음
