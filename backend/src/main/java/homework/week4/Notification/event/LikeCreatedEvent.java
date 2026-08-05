package homework.week4.Notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

//게시글 좋아요 발생 이벤트. 좋아요 등록 서비스는 이 이벤트만 발행하고 알림 생성은 리스너가 담당한다
@Getter
@AllArgsConstructor
public class LikeCreatedEvent {
    private final Long postId;
    private final Long actorUserId;
    private final Long receiverUserId;
    private final LocalDateTime occurredAt;
}
