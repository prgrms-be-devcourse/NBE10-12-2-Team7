---
id: FEAT-213
type: feature
status: done
author: NextWave-Dev-Space
date: 2026-07-23
related: []
tags: [admin, chat, global, manner, product, my-reports]
pr: 213
---

## 무엇을 / 왜

매너온도 시스템 프론트엔드를 구현했다.
Closes #212
백엔드 PR #210 에 이어지는 프론트엔드 작업이다.

## 어떻게 (구현 요약)

- MannerScoreBadge: 온도 등급별 색상(위험/주의/기본/우수)이 적용된 작은 뱃지 컴포넌트.
  상품 상세 판매자 정보, 채팅방 헤더에서 재사용
- MannerScoreCard: 현재 온도 + 등급별 그라데이션 게이지 + 변화 이력 타임라인을 보여주는 상세 카드
- MannerRatingModal: 거래 후 별점(1~5점) 등록 모달, 등록 성공/중복/미완료 등 에러 메시지 처리
- mannerScoreBand.ts / mannerScoreReason.ts: 온도 등급 색상·라벨, 변화 이력 사유 라벨 헬퍼
- my-reports 페이지: 제목을 "신고내역"으로 변경, 매너온도 카드를 최상단에 배치하고
  기존 신고 통계 요약 카드를 하단으로 이동 (Header 내비게이션 라벨도 함께 변경)
- products/[id] 페이지: 판매자 정보에 매너온도 뱃지 연동
- chat/[roomId] 페이지: 상대방 닉네임 옆 매너온도 뱃지 연동, 거래완료 시 "거래가 완료됐어요 · 후기 남기기" 배너 추가
- chat/[roomId] 페이지: `viewerRole === 'BUYER'`일 때만 "후기 남기기" 배너 노출하도록 수정
- (채팅 도메인) `ChatRoomListResponse`/`ChatService`에 `viewerRole`("BUYER"/"SELLER") 필드 추가
   — 권건우님 승인 하에 최소 범위로 수정

**검증**

- 로컬 서버(백엔드 + MySQL + Redis) 기동 후 실제 로그인해서 전체 플로우 확인: 상품 상세 뱃지 → 채팅방 생성 →
  거래완료 상품 채팅방에서 별점 등록 → 매너온도 실제 반영(36.5 → 36.7) → 중복 등록 시 에러 메시지 노출까지 확인
- 매너온도 API 4종(별점 등록/공개 조회/내 이력 조회/관리자 저신뢰 조회) 개별 curl 호출로 정상 케이스 및 주요 예외 케이스
  (권한 없음, 중복, 미완료 거래, 범위 밖 점수) 전부 검증 완료
- `tsc --noEmit`, `eslint` 통과 확인함 (신규 에러 없음, 경고는 기존 코드에서도 있던 img 태그 관련뿐)
- 콘솔 에러 없음
- 같은 채팅방을 구매자 계정으로 조회 시 `viewerRole: "BUYER"`, 판매자 계정으로 조회 시
  `viewerRole: "SELLER"` 정상 반환 확인
- 실제 로그인해서 구매자는 "후기 남기기" 배너가 보이고, 판매자는 보이지 않는 것까지 양쪽 다 브라우저로 확인

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/DongneMarketApplication.java` (+2/-0)
- `backend/src/main/java/com/dongnemarket/admin/controller/AdminMannerController.java` (+33/-0)
- `backend/src/main/java/com/dongnemarket/admin/dto/AdminMannerResponse.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminMannerService.java` (+30/-0)
- `backend/src/main/java/com/dongnemarket/admin/service/AdminReportService.java` (+11/-2)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatRoomListResponse.java` (+9/-3)
- `backend/src/main/java/com/dongnemarket/chat/service/ChatService.java` (+6/-1)
- `backend/src/main/java/com/dongnemarket/global/common/event/ProductCompletedEvent.java` (+13/-0)
- `backend/src/main/java/com/dongnemarket/global/common/event/ReportStatusChangedEvent.java` (+16/-0)
- `backend/src/main/java/com/dongnemarket/global/exception/ErrorCode.java` (+5/-0)
- `backend/src/main/java/com/dongnemarket/global/security/SecurityConfig.java` (+1/-0)
- `backend/src/main/java/com/dongnemarket/manner/controller/MannerRatingController.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/manner/controller/MannerScoreController.java` (+40/-0)
- `backend/src/main/java/com/dongnemarket/manner/dto/MannerRatingCreateRequest.java` (+26/-0)
- `backend/src/main/java/com/dongnemarket/manner/dto/MannerRatingResponse.java` (+32/-0)
- … 외 30개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- 상품 상세 페이지에서의 "후기 남기기" 트리거는 거래완료 상품이 상세 조회 자체가 막혀 있어(`PRODUCT_NOT_FOUND`)
  채팅방 쪽으로 옮겼다.
- 채팅방에서는 판매자/구매자 구분 없이 거래완료 상태면 "후기 남기기" 배너가 보이는 현상을 `viewerRole` 필드 추가로 해결,
  구매자에게만 노출되도록 수정 완료

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/213
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/212
