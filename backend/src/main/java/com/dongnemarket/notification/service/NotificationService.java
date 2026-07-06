package com.dongnemarket.notification.service;

import com.dongnemarket.chat.dto.ChatRoomListResponse;
import com.dongnemarket.chat.service.ChatService;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.notification.dto.NotificationResponse;
import com.dongnemarket.notification.entity.Notification;
import com.dongnemarket.notification.entity.NotificationType;
import com.dongnemarket.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    /** 알림 목록 상한. 개인 목록이지만 읽은 알림이 누적되므로 최근순으로 캡한다. */
    private static final Limit MY_NOTIFICATIONS_LIMIT = Limit.of(100);

    private final NotificationRepository notificationRepository;
    private final ChatService chatService;
    private final EntityManager entityManager;

    public NotificationService(NotificationRepository notificationRepository,
                               ChatService chatService,
                               EntityManager entityManager) {
        this.notificationRepository = notificationRepository;
        this.chatService = chatService;
        this.entityManager = entityManager;
    }

    /**
     * 댓글 알림을 저장한다. 같은 상품의 안읽은 알림이 이미 있으면 새 row 대신 발생 시각만 갱신(코얼레싱)한다.
     * <p>동시 이벤트 레이스로 안읽은 알림이 2행 이상이면, 최신 1개만 갱신하고 나머지는 삭제해 1행으로 수렴시킨다
     * (DB 부분 유니크로 못 막는 대신 애플리케이션이 수렴). 상품 삭제·개명에도 안정적이도록 문구는 스냅샷으로 저장한다.
     */
    @Transactional
    public void notifyComment(Long recipientId, Long productId, String productTitle) {
        List<Notification> unread = notificationRepository
                .findByRecipient_IdAndProductIdAndTypeAndIsReadFalseOrderByLastNotifiedAtDesc(
                        recipientId, productId, NotificationType.COMMENT);

        if (unread.isEmpty()) {
            Member recipient = entityManager.getReference(Member.class, recipientId);
            notificationRepository.save(
                    Notification.of(recipient, NotificationType.COMMENT, buildCommentMessage(productTitle), productId));
            return;
        }

        // 최신 1개만 남겨 발생 시각을 끌어올리고(코얼레싱), 레이스로 중복된 나머지는 정리해 1행으로 수렴시킨다.
        unread.get(0).renotify();
        if (unread.size() > 1) {
            notificationRepository.deleteAll(unread.subList(1, unread.size()));
        }
    }

    /**
     * 내 알림 피드를 최근 발생순으로 조회한다.
     * <p>저장형 <b>댓글 알림</b>과, 저장하지 않고 안읽은 채팅방에서 파생한 <b>채팅 알림</b>(방마다 1건)을 합쳐
     * {@code occurredAt} DESC로 정렬한다. 채팅은 방 입장(읽음 처리) 시 안읽음이 0이 되어 자동으로 사라진다.
     */
    public List<NotificationResponse> getMyNotifications(Long memberId) {
        Stream<NotificationResponse> comments = notificationRepository
                .findByRecipient_IdOrderByLastNotifiedAtDesc(memberId, MY_NOTIFICATIONS_LIMIT)
                .stream()
                .map(NotificationResponse::from);
        Stream<NotificationResponse> chats = unreadRooms(memberId)
                .map(this::toChatNotification);
        return Stream.concat(comments, chats)
                .sorted(Comparator.comparing(NotificationResponse::getOccurredAt, Comparator.reverseOrder()))
                .toList();
    }

    /** 안읽은 알림 총 개수(안읽은 댓글 알림 + 안읽은 채팅방 수). 헤더 배지용. */
    public long getUnreadCount(Long memberId) {
        long unreadComments = notificationRepository.countByRecipient_IdAndIsReadFalse(memberId);
        long unreadRooms = unreadRooms(memberId).count();
        return unreadComments + unreadRooms;
    }

    /** 내 안읽은 알림을 전부 읽음 처리한다(알림 패널 열람 시). 채팅 알림은 방 읽음 처리로 사라지므로 여기서 건드리지 않는다. */
    @Transactional
    public void markAllRead(Long memberId) {
        notificationRepository.markAllReadByRecipientId(memberId);
    }

    /** 내가 참여한 방 중 안읽은 메시지가 있는 방(채팅 알림의 원천). 상대·상품·마지막 메시지를 그대로 재사용한다. */
    private Stream<ChatRoomListResponse> unreadRooms(Long memberId) {
        return chatService.getMyRooms(memberId).stream()
                .filter(room -> room.getUnreadCount() > 0);
    }

    private NotificationResponse toChatNotification(ChatRoomListResponse room) {
        LocalDateTime occurredAt = room.getLastMessage() != null
                ? room.getLastMessage().getCreatedAt()
                : room.getCreatedAt();
        String message = buildChatMessage(room.getProduct().getTitle(), room.getOpponent().getNickname());
        return NotificationResponse.chat(message, room.getProduct().getProductId(), room.getRoomId(), occurredAt);
    }

    private String buildCommentMessage(String productTitle) {
        return "\"" + productTitle + "\" 글에 새로운 댓글이 작성되었습니다.";
    }

    private String buildChatMessage(String productTitle, String opponentNickname) {
        return "\"" + productTitle + "\"에 대해 \"" + opponentNickname + "\"님의 새로운 채팅이 도착했습니다!";
    }
}
