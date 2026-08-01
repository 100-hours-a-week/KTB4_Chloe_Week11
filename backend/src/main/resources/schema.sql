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
