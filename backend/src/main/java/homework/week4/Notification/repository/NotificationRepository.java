package homework.week4.Notification.repository;

import homework.week4.Notification.entity.Notification;
import homework.week4.Notification.entity.NotificationType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    //안 읽은 전체 알림 개수. 로그인 응답 / SSE init·push / 읽음 처리 응답에서 공용으로 사용
    Long countByReceiverUserIdAndIsReadFalseAndDeletedAtIsNull(Long userId);

    //아직 끝나지 않은(읽지 않았고, 그룹 생성 4시간 이내인) 같은 게시글의 좋아요 그룹 알림 조회
    Optional<Notification> findByReceiverUserIdAndPostPostIdAndTypeAndIsReadFalseAndDeletedAtIsNullAndGroupCreatedAtAfter(
            Long receiverId,
            Long postId,
            NotificationType type,
            LocalDateTime after
    );
}
