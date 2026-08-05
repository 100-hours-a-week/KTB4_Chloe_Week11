package homework.week4.Notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationListItemDto {
    private Long notificationId;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Long postId;
    private String postTitle;
    private String actorNickname;
    private String actorProfileImage;
    private Integer likeGroupCount;
}
