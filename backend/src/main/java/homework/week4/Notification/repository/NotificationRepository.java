package homework.week4.Notification.repository;

import homework.week4.Notification.entity.Notification;
import homework.week4.Notification.entity.NotificationType;

import org.springframework.data.domain.Pageable;
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

    //알림 목록 커서 페이지네이션. createdAt DESC, unreadOnly 필터, opaque 커서(createdAt) 적용
    @Query("""
        SELECT n
        FROM Notification n
        JOIN FETCH n.post
        LEFT JOIN FETCH n.actor
        WHERE n.receiver.userId = :userId
        AND n.deletedAt IS NULL
        AND (:unreadOnly = false OR n.isRead = false)
        AND (:cursor IS NULL OR n.createdAt < :cursor)
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findNotifications(
            @Param("userId") Long userId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable
    );

    //읽음 처리용 조회. 이미 읽었어도 조회는 되어야 멱등하게 200을 낼 수 있다
    Optional<Notification> findByNotificationIdAndReceiverUserIdAndDeletedAtIsNull(Long notificationId, Long receiverId);

    //삭제용 조회. 읽음 처리된 알림만 삭제 대상이므로 is_read = true 조건을 함께 건다 (미읽음이면 존재하지 않는 것과 동일하게 404)
    Optional<Notification> findByNotificationIdAndReceiverUserIdAndIsReadTrueAndDeletedAtIsNull(Long notificationId, Long receiverId);
}
