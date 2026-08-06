package homework.week4.Notification.service;

import homework.week4.Comment.entity.Comment;
import homework.week4.Comment.repository.CommentRepository;
import homework.week4.Notification.dto.NotificationListItemDto;
import homework.week4.Notification.dto.NotificationListResponseDto;
import homework.week4.Notification.entity.Notification;
import homework.week4.Notification.entity.NotificationType;
import homework.week4.Notification.repository.NotificationRepository;
import homework.week4.Post.entity.Post;
import homework.week4.Post.repository.PostRepository;
import homework.week4.User.entity.User;
import homework.week4.User.repository.UserRepository;
import homework.week4.exception.InvalidRequestException;
import homework.week4.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    //안 읽은 알림 개수 조회. 로그인 응답, SSE init/push, 읽음 처리 응답에서 재사용
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiverUserIdAndIsReadFalseAndDeletedAtIsNull(userId);
    }

    //좋아요 알림 생성/그룹 갱신. 같은 게시글에 대해 읽지 않은 채로 4시간 이내인 그룹이 있으면 갱신하고, 없으면 새로 만든다
    @Transactional
    public Notification handleLikeCreated(Long postId, Long actorUserId, Long receiverUserId, LocalDateTime occurredAt) {
        User receiver = getUserOrThrow(receiverUserId);
        User actor = getUserOrThrow(actorUserId);
        Post post = getPostOrThrow(postId);

        LocalDateTime groupWindowStart = occurredAt.minusHours(4);

        Optional<Notification> activeGroup = notificationRepository
                .findByReceiverUserIdAndPostPostIdAndTypeAndIsReadFalseAndDeletedAtIsNullAndGroupCreatedAtAfter(
                        receiverUserId, postId, NotificationType.LIKE, groupWindowStart);

        if (activeGroup.isPresent()) {
            Notification notification = activeGroup.get();
            notification.updateLikeGroup(occurredAt);
            return notification;
        }

        return notificationRepository.save(new Notification(receiver, post, actor, occurredAt));
    }

    //댓글 알림 생성. 댓글은 좋아요와 달리 묶지 않고 매번 개별 알림으로 만든다
    @Transactional
    public Notification handleCommentCreated(Long postId, Long commentId, Long actorUserId, Long receiverUserId, LocalDateTime occurredAt) {
        User receiver = getUserOrThrow(receiverUserId);
        User actor = getUserOrThrow(actorUserId);
        Post post = getPostOrThrow(postId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("해당 댓글이 존재하지 않습니다."));

        return notificationRepository.save(new Notification(receiver, post, actor, comment, occurredAt));
    }

    //블라인드 알림 생성. 신고 누적으로 인한 자동 처리이므로 행위자가 없다
    @Transactional
    public Notification handlePostBlinded(Long postId, Long receiverUserId, LocalDateTime occurredAt) {
        User receiver = getUserOrThrow(receiverUserId);
        Post post = getPostOrThrow(postId);

        return notificationRepository.save(new Notification(receiver, post, occurredAt));
    }

    //SSE 재연결 시 Last-Event-ID(알림 생성 일시)보다 최신인 미삭제 알림을 오래된 순으로 조회
    @Transactional(readOnly = true)
    public List<Notification> getMissedNotifications(Long userId, LocalDateTime after) {
        return notificationRepository.findMissedNotifications(userId, after);
    }

    //SSE로 내려보낼 이벤트 payload 구성. 알림 종류별 필드는 SSE 명세를 그대로 따른다
    @Transactional(readOnly = true)
    public Map<String, Object> buildPushPayload(Notification notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("unreadCount", getUnreadCount(notification.getReceiver().getUserId()));

        switch (notification.getType()) {
            case COMMENT -> {
                payload.put("postId", notification.getPost().getPostId());
                payload.put("message", buildToastMessage(notification));
            }
            case BLIND -> payload.put("message", buildToastMessage(notification));
            case LIKE -> { /* unreadCount만 내려보낸다 */ }
        }

        return payload;
    }

    //게시글 제목을 10자로 자르고 말줄임표를 붙인 뒤, 알림 종류에 맞는 문구를 붙인다
    private String buildToastMessage(Notification notification) {
        String title = notification.getPost().getTitle();
        String truncatedTitle = title.length() > 10 ? title.substring(0, 10) + "…" : title;

        String suffix = switch (notification.getType()) {
            case COMMENT -> "에 새 댓글이 달렸습니다.";
            case BLIND -> "이 신고 누적으로 블라인드 처리되었습니다.";
            case LIKE -> "";
        };

        return truncatedTitle + suffix;
    }

    //알림 목록 커서 페이지네이션. limit+1개를 조회해서 다음 페이지 존재 여부를 판단한다
    @Transactional(readOnly = true)
    public NotificationListResponseDto listNotifications(Long userId, String cursor, int limit, boolean unreadOnly) {
        if (limit <= 0) {
            throw new InvalidRequestException("limit 값이 올바르지 않습니다.");
        }

        LocalDateTime decodedCursor = decodeCursor(cursor);

        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Notification> fetched = notificationRepository.findNotifications(userId, unreadOnly, decodedCursor, pageable);

        boolean hasNext = fetched.size() > limit;
        List<Notification> pageItems = hasNext ? fetched.subList(0, limit) : fetched;

        List<NotificationListItemDto> items = pageItems.stream()
                .map(this::toListItemDto)
                .toList();

        String nextCursor = hasNext ? encodeCursor(pageItems.get(pageItems.size() - 1).getCreatedAt()) : null;

        return new NotificationListResponseDto(items, nextCursor);
    }

    //읽음 처리. 이미 읽은 알림을 다시 호출해도 에러 없이 처리된다(멱등)
    //댓글·좋아요는 클릭 즉시 다른 페이지로 이동하는 프론트 UX라 unreadCount가 필요 없어 null을 반환하고,
    //블라인드는 같은 화면에 머무르며 값을 재조정해야 해서 unreadCount를 반환한다
    @Transactional
    public Long markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndReceiverUserIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다."));

        notification.markAsRead();

        if (notification.getType() == NotificationType.BLIND) {
            return getUnreadCount(userId);
        }
        return null;
    }

    //모두 읽음 처리. 안 읽은 알림 전체를 한 번에 갱신하고 갱신 후 unreadCount(0)를 반환한다
    @Transactional
    public Long markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
        return getUnreadCount(userId);
    }

    //소프트 삭제. 읽음 처리되지 않은 알림은 조회 자체가 안 되어 "존재하지 않음"과 동일하게 404로 수렴한다
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndReceiverUserIdAndIsReadTrueAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않거나 아직 읽지 않은 알림입니다."));

        notification.isDeleted(LocalDateTime.now());
    }

    private NotificationListItemDto toListItemDto(Notification notification) {
        boolean isBlind = notification.getType() == NotificationType.BLIND;

        return new NotificationListItemDto(
                notification.getNotificationId(),
                notification.getType().name().toLowerCase(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                isBlind ? null : notification.getPost().getPostId(),
                notification.getPost().getTitle(),
                isBlind ? null : notification.getActor().getNickname(),
                isBlind ? null : notification.getActor().getProfileImage(),
                notification.getType() == NotificationType.LIKE ? notification.getLikeGroupCount() : null
        );
    }

    private String encodeCursor(LocalDateTime createdAt) {
        return Base64.getEncoder().encodeToString(createdAt.toString().getBytes(StandardCharsets.UTF_8));
    }

    private LocalDateTime decodeCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cursor);
            return LocalDateTime.parse(new String(decoded, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new InvalidRequestException("cursor 형식이 올바르지 않습니다.");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("해당 사용자 정보가 존재하지 않습니다."));
    }

    private Post getPostOrThrow(Long postId) {
        //postVerifyService.getValidPost는 postHide=false만 조회하므로 블라인드 직후 조회가 실패한다. 여기서는 숨김 여부와 무관하게 조회해야 한다
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("해당 게시글이 존재하지 않습니다."));
    }
}
