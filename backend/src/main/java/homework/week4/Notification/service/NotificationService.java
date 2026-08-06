package homework.week4.Notification.service;

import homework.week4.Notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    //안 읽은 알림 개수 조회. 로그인 응답, SSE init/push, 읽음 처리 응답에서 재사용
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiverUserIdAndIsReadFalseAndDeletedAtIsNull(userId);
    }
}
