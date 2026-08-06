package homework.week4.Notification.repository;

import homework.week4.Notification.entity.Notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    //안 읽은 전체 알림 개수. 로그인 응답 / SSE init·push / 읽음 처리 응답에서 공용으로 사용
    Long countByReceiverUserIdAndIsReadFalseAndDeletedAtIsNull(Long userId);
}
