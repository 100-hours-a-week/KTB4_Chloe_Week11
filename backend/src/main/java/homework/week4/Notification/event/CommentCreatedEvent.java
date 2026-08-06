package homework.week4.Notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

//게시글 댓글 등록 이벤트. 댓글 등록 서비스는 이 이벤트만 발행하고 알림 생성은 리스너가 담당한다
@Getter
@AllArgsConstructor
public class CommentCreatedEvent {
    private final Long postId;
    private final Long commentId;
    private final Long actorUserId;
    private final Long receiverUserId;
    private final LocalDateTime occurredAt;
}
