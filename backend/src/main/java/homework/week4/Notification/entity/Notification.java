package homework.week4.Notification.entity;

import homework.week4.Comment.entity.Comment;
import homework.week4.Post.entity.Post;
import homework.week4.User.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    //알림 수신자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type")
    private NotificationType type;

    //관련 게시글 (전체 알림 공통)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    //알림 발생 행위자 (좋아요·댓글만, 블라인드는 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    //관련 댓글 (댓글 알림만)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column(name = "like_group_count")
    private Integer likeGroupCount;

    @Column(name = "is_read")
    private Boolean isRead;

    //좋아요 그룹 최초 생성 시각 (좋아요 알림만, 4시간 윈도우 판단용)
    @Column(name = "group_created_at")
    private LocalDateTime groupCreatedAt;

    //알림 생성/그룹 갱신 일시. 정렬, SSE 이벤트 id, REST 커서 기준
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    //좋아요 알림 생성용 생성자
    public Notification(User receiver, Post post, User actor, LocalDateTime createdAt) {
        this.receiver = receiver;
        this.post = post;
        this.actor = actor;
        this.type = NotificationType.LIKE;
        this.likeGroupCount = 1;
        this.isRead = false;
        this.groupCreatedAt = createdAt;
        this.createdAt = createdAt;
    }

    //댓글 알림 생성용 생성자
    public Notification(User receiver, Post post, User actor, Comment comment, LocalDateTime createdAt) {
        this.receiver = receiver;
        this.post = post;
        this.actor = actor;
        this.comment = comment;
        this.type = NotificationType.COMMENT;
        this.likeGroupCount = 1;
        this.isRead = false;
        this.createdAt = createdAt;
    }

    //블라인드 알림 생성용 생성자
    public Notification(User receiver, Post post, LocalDateTime createdAt) {
        this.receiver = receiver;
        this.post = post;
        this.type = NotificationType.BLIND;
        this.likeGroupCount = 1;
        this.isRead = false;
        this.createdAt = createdAt;
    }

    //같은 좋아요 그룹에 새 행위자가 들어왔을 때 갱신
    public void updateLikeGroup(User actor, LocalDateTime createdAt) {
        this.actor = actor;
        this.likeGroupCount = this.likeGroupCount + 1;
        this.createdAt = createdAt;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void isDeleted(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
