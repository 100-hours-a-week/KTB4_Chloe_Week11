package homework.week4.Notification.service;

import homework.week4.Comment.entity.Comment;
import homework.week4.Comment.repository.CommentRepository;
import homework.week4.Notification.entity.Notification;
import homework.week4.Notification.entity.NotificationType;
import homework.week4.Notification.repository.NotificationRepository;
import homework.week4.Post.entity.Post;
import homework.week4.Post.repository.PostRepository;
import homework.week4.User.entity.User;
import homework.week4.User.repository.UserRepository;
import homework.week4.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public void handleLikeCreated(Long postId, Long actorUserId, Long receiverUserId, LocalDateTime occurredAt) {
        User receiver = getUserOrThrow(receiverUserId);
        User actor = getUserOrThrow(actorUserId);
        Post post = getPostOrThrow(postId);

        LocalDateTime groupWindowStart = occurredAt.minusHours(4);

        Optional<Notification> activeGroup = notificationRepository
                .findByReceiverUserIdAndPostPostIdAndTypeAndIsReadFalseAndDeletedAtIsNullAndGroupCreatedAtAfter(
                        receiverUserId, postId, NotificationType.LIKE, groupWindowStart);

        if (activeGroup.isPresent()) {
            activeGroup.get().updateLikeGroup(actor, occurredAt);
        } else {
            notificationRepository.save(new Notification(receiver, post, actor, occurredAt));
        }
    }

    //댓글 알림 생성. 댓글은 좋아요와 달리 묶지 않고 매번 개별 알림으로 만든다
    @Transactional
    public void handleCommentCreated(Long postId, Long commentId, Long actorUserId, Long receiverUserId, LocalDateTime occurredAt) {
        User receiver = getUserOrThrow(receiverUserId);
        User actor = getUserOrThrow(actorUserId);
        Post post = getPostOrThrow(postId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("해당 댓글이 존재하지 않습니다."));

        notificationRepository.save(new Notification(receiver, post, actor, comment, occurredAt));
    }

    //블라인드 알림 생성. 신고 누적으로 인한 자동 처리이므로 행위자가 없다
    @Transactional
    public void handlePostBlinded(Long postId, Long receiverUserId, LocalDateTime occurredAt) {
        User receiver = getUserOrThrow(receiverUserId);
        Post post = getPostOrThrow(postId);

        notificationRepository.save(new Notification(receiver, post, occurredAt));
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
