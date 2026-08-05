CREATE TABLE IF NOT EXISTS `likes` (
    `like_id`	BIGINT	AUTO_INCREMENT PRIMARY KEY	COMMENT '좋아요 식별 번호',
    `post_id`	BIGINT	NOT NULL	COMMENT '게시글 식별 번호',
    `user_id`	BIGINT	NOT NULL	COMMENT '사용자 식별 번호'
);

CREATE TABLE IF NOT EXISTS `comments` (
    `comment_id`	BIGINT AUTO_INCREMENT PRIMARY KEY	COMMENT '댓글 식별 번호',
    `post_id`	BIGINT	NOT NULL	COMMENT '게시글 식별 번호',
    `user_id`	BIGINT	NOT NULL	COMMENT '사용자 식별 번호',
    `parent_comment_id`	BIGINT	NULL	COMMENT '부모 댓글 번호',
    `comment_content`	TEXT	NOT NULL	COMMENT '댓글 본문',
    `comment_date_written`	TIMESTAMP	NOT NULL	COMMENT '댓글 작성 시각',
    `is_having_child`	BOOLEAN	NOT NULL	DEFAULT false	COMMENT '대댓글 여부',
    `is_blinded`	BOOLEAN	NOT NULL	DEFAULT false	COMMENT '댓글 표시 여부',
    `created_at`	TIMESTAMP	NOT NULL	COMMENT '댓글 생성 일시',
    `updated_at`	TIMESTAMP	NULL	COMMENT '댓글 수정 일시',
    `deleted_at`	TIMESTAMP	NULL	COMMENT '댓글 삭제 일시',
    INDEX idx_comments_post_id (post_id)
);

CREATE TABLE IF NOT EXISTS `posts` (
    `post_id`	BIGINT	AUTO_INCREMENT PRIMARY KEY	COMMENT '게시글 식별 번호',
    `user_id`	BIGINT	NOT NULL	COMMENT '사용자 식별 번호',
    `title`	VARCHAR(26)	NOT NULL	COMMENT '게시글 제목',
    `content`	TEXT	NOT NULL	COMMENT '게시글 본문 내용',
    `post_image`	VARCHAR(512)	NULL	COMMENT '게시글 이미지 URL',
    `date_written`	TIMESTAMP	NOT NULL	COMMENT '게시글 작성 시각',
    `post_hide`	BOOLEAN	NOT NULL	DEFAULT false	COMMENT '게시글 숨김 여부',
    `is_edited`	BOOLEAN	NOT NULL	DEFAULT false	COMMENT '게시글 수정 여부',
    `comment_count`	BIGINT	NOT NULL	COMMENT '댓글 수',
    `view_count`	BIGINT	NOT NULL	COMMENT '조회수',
    `like_count`	BIGINT	NOT NULL	COMMENT '좋아요 수',
    `created_at`	TIMESTAMP	NOT NULL	COMMENT '게시글 생성 일시',
    `updated_at`	TIMESTAMP	NULL	COMMENT '게시글 수정 일시',
    `deleted_at`	TIMESTAMP	NULL	COMMENT '게시글 삭제 일시'
);

CREATE TABLE IF NOT EXISTS `post_change_history` (
    `change_id`	BIGINT AUTO_INCREMENT PRIMARY KEY	COMMENT '게시글 수정 이력 식별번호',
    `post_id`	BIGINT	NOT NULL	COMMENT '게시글 식별 번호',
    `changed_at`	TIMESTAMP	NOT NULL	COMMENT '게시글 수정 이력 시각',
    `changed_title`	VARCHAR(26)	NOT NULL	COMMENT '게시글 제목 수정본',
    `changed_content`	TEXT	NOT NULL	COMMENT '게시글 본문 수정본',
    `changed_post_image`	VARCHAR(512)	NULL	COMMENT '게시글 이미지 수정본'
);

CREATE TABLE IF NOT EXISTS `users` (
    `user_id`	BIGINT AUTO_INCREMENT PRIMARY KEY	COMMENT '사용자 식별 번호',
    `email`	VARCHAR(320)	NOT NULL	COMMENT '사용자 이메일',
    `password`	VARCHAR(100)	NOT NULL	COMMENT '사용자 비밀번호',
    `nickname`	VARCHAR(10)	NOT NULL	COMMENT '사용자 닉네임',
    `profile_image`	VARCHAR(512)	NULL	COMMENT '사용자 프로필 이미지 URL',
    `is_member`	BOOLEAN	NOT NULL	DEFAULT true	COMMENT '사용자 탈퇴 여부',
    `created_at`	TIMESTAMP	NOT NULL	COMMENT '사용자 생성 일시',
    `updated_at`	TIMESTAMP	NULL	COMMENT '사용자 정보 수정 일시',
    `deleted_at`	TIMESTAMP	NULL	COMMENT '사용자 탈퇴 일시'
);

CREATE TABLE IF NOT EXISTS `post_report_history` (
    `report_id`	BIGINT AUTO_INCREMENT PRIMARY KEY	COMMENT '게시글 신고 식별 번호',
    `post_id`	BIGINT	NOT NULL	COMMENT '게시글 식별 번호',
    `user_id`	BIGINT	NOT NULL	COMMENT '사용자 식별 번호',
    `reported_at`	TIMESTAMP	NOT NULL	COMMENT '신고 시각'
);

CREATE TABLE IF NOT EXISTS `notifications` (
    `notification_id`	BIGINT	AUTO_INCREMENT PRIMARY KEY	COMMENT '알림 식별 번호',
    `user_id`	BIGINT	NOT NULL	COMMENT '알림 수신자 사용자 식별 번호',
    `notification_type`	VARCHAR(10)	NOT NULL	COMMENT '알림 종류 (LIKE, COMMENT, BLIND)',
    `post_id`	BIGINT	NOT NULL	COMMENT '관련 게시글 식별 번호',
    `actor_user_id`	BIGINT	NULL	COMMENT '알림 발생 행위자 사용자 식별 번호 (블라인드는 NULL)',
    `comment_id`	BIGINT	NULL	COMMENT '관련 댓글 식별 번호 (댓글 알림만)',
    `like_group_count`	INT	NOT NULL	DEFAULT 1	COMMENT '좋아요 그룹 인원 수 (댓글·블라인드는 1 고정)',
    `is_read`	BOOLEAN	NOT NULL	DEFAULT false	COMMENT '읽음 여부',
    `group_created_at`	TIMESTAMP	NULL	COMMENT '좋아요 그룹 최초 생성 시각 (좋아요 알림만)',
    `created_at`	TIMESTAMP	NOT NULL	COMMENT '알림 생성/그룹 갱신 일시',
    `deleted_at`	TIMESTAMP	NULL	COMMENT '알림 소프트 삭제 일시',
    FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`user_id`),
    FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`),
    FOREIGN KEY (`comment_id`) REFERENCES `comments` (`comment_id`),
    INDEX idx_notifications_receiver (user_id, deleted_at, created_at)
);
