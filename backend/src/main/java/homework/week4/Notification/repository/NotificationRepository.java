package homework.week4.Notification.repository;

import homework.week4.Notification.entity.Notification;
import homework.week4.Notification.entity.NotificationType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    //SSE 재연결 시 Last-Event-ID(알림 생성 일시)보다 최신인 미삭제 알림을 오래된 순으로 재조회. 별도 이벤트 로그 없이 notifications 테이블을 그대로 재사용한다
    @Query("""
        SELECT n
        FROM Notification n
        JOIN FETCH n.post
        WHERE n.receiver.userId = :userId
        AND n.deletedAt IS NULL
        AND n.createdAt > :after
        ORDER BY n.createdAt ASC
    """)
    List<Notification> findMissedNotifications(@Param("userId") Long userId, @Param("after") LocalDateTime after);
}
