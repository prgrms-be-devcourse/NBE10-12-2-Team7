package com.dongnemarket.notification.event;

import com.dongnemarket.global.common.event.CommentCreatedEvent;
import com.dongnemarket.global.common.event.ProductPriceChangedEvent;
import com.dongnemarket.notification.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 도메인 이벤트를 받아 알림을 저장하는 핸들러.
 * <p>{@code AFTER_COMMIT} + {@code REQUIRES_NEW}로 동작한다:
 * <ul>
 *   <li><b>AFTER_COMMIT</b> — 원본 작업(댓글 저장)이 커밋된 뒤에만 알림을 만든다(롤백된 댓글엔 알림 없음).</li>
 *   <li><b>REQUIRES_NEW</b> — 새 트랜잭션에서 처리한다. AFTER_COMMIT 리스너는 이미 커밋된 트랜잭션 밖이라
 *       새 트랜잭션이 없으면 저장이 커밋되지 않는 함정이 있다. 또한 알림 저장 실패가 댓글 작성을 되돌리지 않는다(best-effort).</li>
 * </ul>
 */
@Component
public class NotificationEventHandler {

    private final NotificationService notificationService;

    public NotificationEventHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCommentCreated(CommentCreatedEvent event) {
        notificationService.notifyComment(event.recipientId(), event.productId(), event.productTitle());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePriceChanged(ProductPriceChangedEvent event) {
        notificationService.notifyPriceChange(event.productId(), event.productTitle());
    }
}
