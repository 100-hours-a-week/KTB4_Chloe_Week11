package homework.week4.Notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

//신고 누적으로 게시글이 자동 블라인드 처리된 이벤트. 행위자가 없으므로 actorUserId가 없다
@Getter
@AllArgsConstructor
public class PostBlindedEvent {
    private final Long postId;
    private final Long receiverUserId;
    private final LocalDateTime occurredAt;
}
