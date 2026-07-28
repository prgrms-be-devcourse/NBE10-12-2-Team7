---
id: FEAT-120
type: feature
status: done
author: Crispy-down
date: 2026-07-03
related: [FEAT-100, FEAT-107]
tags: [chat, backend]
pr: 120
---

## 무엇을 / 왜

1:1 채팅 서비스·컨트롤러·REST API (PR1b, 폴링)

## 어떻게 (구현 요약)

1:1 채팅(폴링 기반)의 서비스·컨트롤러·REST API. PR1a(#107) 골격 위에 실제 동작을 얹어 **폴링만으로 기능 완성**. 방 단위 = **상품별 1개**(`UNIQUE(product_id, buyer_id)`).

**REST 4종**
| 메서드 | 경로 | 설명 | 응답 |
|---|---|---|---|
| POST | `/api/chat-rooms` | 방 연결(get-or-create) | 신규·기존 **모두 200** |
| GET | `/api/chat-rooms` | 내 방 목록 | 200 |
| GET | `/api/chat-rooms/{roomId}/messages?cursor=&size=` | 메시지 커서 조회(최신순) | 200 |
| POST | `/api/chat-rooms/{roomId}/messages` | 메시지 전송 | 201 |

**핵심 설계**
- **get-or-create 경쟁 복구**: 쓰기를 `ChatRoomCreator`(독립 트랜잭션)로 분리. 동시 최초 생성 경쟁으로 UNIQUE 위반 시, 그 트랜잭션 **밖**에서 재조회해 이긴 방 반환 → rollback-only 트랜잭션 재사용으로 인한 `UnexpectedRollbackException`(500) 회피. (Favorite는 예외를 *던지는* 패턴이라 그대로 못 씀 — 여기선 *복구*가 필요)
- **인가**: `ChatRoom.isParticipant`(buyer/seller 순수 row 비교, 추가 쿼리 0). 비참여자 403.
- **상품 정보 자기완결**: 대표사진·제목·가격·상태·지역·설명을 `Product` 연관 fetch join으로 직접 로딩(대표사진 = `Product.thumbnailUrl`, #100 비정규화). 크로스도메인 조회·N+1 없음.
- **방별 마지막 메시지**: `MAX(id)` 한 쿼리로 조회(N+1 없음).
- **커서**: id 단독 커서(size+1 조회로 hasNext 판별), size 상한 100. 요청 DTO는 Jackson용 public 생성자.

**DTO**: 요청 2(`ChatRoomCreateRequest`/`ChatMessageCreateRequest`) + 응답(`ChatRoomListResponse`/`ChatRoomDetailResponse`/`ChatMessageResponse`/`ChatMessagePageResponse`) + 요약(`ChatProductSummary`/`ChatProductDetail`/`ChatMemberSummary`, 닉네임만 노출).

**검증**

- `ChatControllerTest`(통합): 방 연결(신규·기존 모두 200/멱등), 자기상품 400, 없는상품 404, 목록(상품요약·상대닉네임·마지막메시지·참여방만), 커서 페이징 경계, 비참여자 403, content 공백·1000자초과 400, 없는 방 404
- `ChatServiceTest`(단위): **동시성 경쟁 복구**(통합 재현 불가 분기) — 위반 예외를 삼키고 이긴 방 재조회 반환
- `./gradlew test` 전체 스위트 **BUILD SUCCESSFUL**(회귀 없음)
<img width="399" height="829" alt="image" src="https://github.com/user-attachments/assets/1c55ecda-0368-4b73-a79d-4efce7ceac74" />
<img width="461" height="676" alt="image" src="https://github.com/user-attachments/assets/7ea35e3a-e1e6-498b-b21f-b846bbd93967" />
<img width="453" height="556" alt="image" src="https://github.com/user-attachments/assets/23b4d83f-1c9d-40d5-aa9f-791dcc47f188" />
<img width="525" height="723" alt="image" src="https://github.com/user-attachments/assets/97e1e288-8bd6-46bc-86b0-2f0a2ae22bfe" />

## 건드린 파일

- `backend/src/main/java/com/dongnemarket/chat/controller/ChatController.java` (+72/-0)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatMemberSummary.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatMessageCreateRequest.java` (+22/-0)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatMessagePageResponse.java` (+29/-0)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatMessageResponse.java` (+35/-0)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatProductDetail.java` (+49/-0)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatProductSummary.java` (+41/-0)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatRoomCreateRequest.java` (+20/-0)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatRoomDetailResponse.java` (+29/-0)
- `backend/src/main/java/com/dongnemarket/chat/dto/ChatRoomListResponse.java` (+43/-0)
- `backend/src/main/java/com/dongnemarket/chat/repository/ChatMessageRepository.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/chat/repository/ChatRoomRepository.java` (+10/-0)
- `backend/src/main/java/com/dongnemarket/chat/service/ChatRoomCreator.java` (+49/-0)
- `backend/src/main/java/com/dongnemarket/chat/service/ChatService.java` (+130/-0)
- `backend/src/test/java/com/dongnemarket/chat/controller/ChatControllerTest.java` (+353/-0)
- … 외 1개 파일

## 결정과 트레이드오프

- PR 본문에 결정 근거 기록 없음.

## 남은 이슈 / 후속 작업

- **방 단위 = 상품별 1개** 확정(회의). 같은 판매자-구매자라도 상품이 다르면 방 분리.
- **폴링 방식**(REST). WebSocket은 PR2(팀장 SecurityConfig 협의). `/api/chat-rooms/**`는 SecurityConfig `anyRequest().authenticated()`로 이미 인증 적용됨 — 팀장 변경 불요.
- **범위 밖(별도 PR)**: 안읽음 카운트·읽음추적 → PR1c(참여자별 last-read 상태 필요) / WebSocket → PR2.
- 폴링 주기 권장: 열린 방 3초, 목록 10초(프론트, `docs/design/chat-frontend-api-spec.md`).

## 링크

- PR: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/pull/120
- 이슈: https://github.com/prgrms-be-devcourse/NBE10-12-2-Team7/issues/119
