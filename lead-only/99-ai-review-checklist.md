# 99-ai-review-checklist.md

PR을 develop에 머지하기 전, **리뷰어(사람)와 통합 에이전트가 공용으로 쓰는 체크리스트**다. 하나라도 체크 안 되면 해당 항목을 사유와 함께 보류한다.

## 1. 형식 게이트
- [ ] 브랜치명이 `feature/{도메인}_{기능}` 형식이다.
- [ ] 커밋 메시지가 `타입: 내용` 형식(feat/fix/docs/refactor/test/chore)이다.
- [ ] PR 템플릿(구현 내용 / 테스트 결과 / 확인 필요 / AI 사용 여부)이 채워져 있다.
- [ ] PR이 작은 단위다 (리뷰 가능한 크기, 하나의 작업 단위만 — 여러 기능이 섞이지 않음).

## 2. 경계 / 범위
- [ ] 변경 파일이 **담당 도메인 패키지**에만 있다(역할표 대조).
- [ ] global 구조를 임의로 변경하지 않았다.
- [ ] ErrorCode COMMON 영역을 수정하지 않았다(팀장 전용).
- [ ] 다른 도메인 ErrorCode 영역을 건드리지 않았다.
- [ ] 공통 파일(build.gradle, application.yml, SecurityConfig 등) 수정 시 사전 공유가 있었다.

## 3. 아키텍처 · 계층 규칙
- [ ] Controller에 비즈니스 로직이 없다 (Service 호출·응답 반환만).
- [ ] Service에 비즈니스 로직이 있고, HTTP 객체를 직접 다루지 않는다.
- [ ] Repository에는 DB 접근 로직만 있다.
- [ ] Entity를 API 응답으로 직접 반환하지 않는다.
- [ ] Request DTO와 Response DTO가 분리되어 있다.

## 4. 응답 · 예외 · 검증
- [ ] 성공 응답이 ApiResponse<T> 형식이다.
- [ ] 에러 응답이 ErrorResponse 형식이다.
- [ ] 예외는 BusinessException + ErrorCode를 사용한다(RuntimeException 직접 던지기 금지).
- [ ] 인증이 필요한 API는 현재 로그인 사용자를 사용한다.
- [ ] 권한 검증이 필요한 API는 본인/권한 검증이 들어가 있다.
- [ ] Request DTO에 Validation이 적용되어 있다.

## 5. 완료 기준 (산출물)
- [ ] 예외 상황 분석·필요 ErrorCode가 먼저 정리되었다.
- [ ] 테스트 코드(성공 1 + 주요 실패 2)가 있다.
- [ ] Swagger 문서가 작성되어 있다.
- [ ] Postman 테스트 시나리오 문서가 있다.

## 6. 머지 게이트 (최종 — 모두 충족해야 머지)
- [ ] 빌드 성공 / 서버 정상 기동
- [ ] 본인 API Postman 정상 동작
- [ ] develop 최신 반영
- [ ] 충돌 해결 완료
- [ ] 담당 패키지 외 변경 없음
- [ ] 공통 응답 형식 준수
- [ ] 불필요한 임시 코드·주석 제거

## 7. 도메인별 추가 점검 (참고)

```
Auth/Member  : 이메일 중복 차단, BCrypt 암호화, 소프트 삭제, 탈퇴/정지 회원 로그인 차단
Product/Cat. : 작성자만 수정·삭제·상태변경, 삭제/숨김 목록 제외, 조회수++, 카테고리 존재 검증
Trade        : COMPLETED → ON_SALE/RESERVED 되돌림 금지
Favorite     : member_id+product_id unique, 중복 409, 없음 404
Comment      : 작성자만 수정·삭제, 목록은 비로그인 허용, 소프트 삭제
Report       : 본인 상품/계정 신고 불가, 기본 상태 RECEIVED
Admin        : /api/admin/**, ROLE_ADMIN 전용, 숨김·소프트삭제·회원상태 변경
```

---
**판정 표기**: 각 PR은 `통과` / `조건부 통과(좌항목 명시)` / `보류(사유)` 중 하나로 표기한다. 보류 항목은 담당자에게 돌려보낸다.
