package homework.week4.Notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NotificationListResponseDto {
    private List<NotificationListItemDto> notifications;
    private String nextCursor;
}
