package homework.week4.Notification.event;

import homework.week4.Notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

//좋아요/댓글/블라인드 처리 서비스는 이벤트만 발행하고, 알림 생성은 여기서 원본 트랜잭션 커밋 이후에 비동기로 처리한다.
//좋아요 저장이 롤백되면 알림만 남는 상황을 막기 위해 AFTER_COMMIT에서만 동작한다.
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeCreated(LikeCreatedEvent event) {
        notificationService.handleLikeCreated(
                event.getPostId(),
                event.getActorUserId(),
                event.getReceiverUserId(),
                event.getOccurredAt()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentCreatedEvent event) {
        notificationService.handleCommentCreated(
                event.getPostId(),
                event.getCommentId(),
                event.getActorUserId(),
                event.getReceiverUserId(),
                event.getOccurredAt()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostBlinded(PostBlindedEvent event) {
        notificationService.handlePostBlinded(
                event.getPostId(),
                event.getReceiverUserId(),
                event.getOccurredAt()
        );
    }
}
