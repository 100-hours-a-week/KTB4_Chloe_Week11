package homework.week4.Post.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class PostResponseDto {

    private Long post_id;

    private String title;
    private String content;
    private String post_image;
    private LocalDateTime datewritten;
    private String writer;

    private String profileImage;

    private Long like_count;
    private Long comment_count;
    private Long view_count;

    private Boolean isOwner;

    public PostResponseDto (
            Long post_id,
            String title,
            String content,
            String post_image,
            LocalDateTime datewritten,
            String writer,
            String profileImage,
            Long like_count,
            Long comment_count,
            Long view_count,
            Boolean isOwner
    ){
        this.post_id = post_id;
        this.title = title;
        this.content = content;
        this.post_image = post_image;
        this.datewritten = datewritten;
        this.writer = writer;
        this.profileImage = profileImage;
        this.like_count = like_count;
        this.comment_count = comment_count;
        this.view_count = view_count;
        this.isOwner = isOwner;
    }
    public PostResponseDto (
            Long post_id,
            String title,
            String content,
            String post_image,
            LocalDateTime datewritten,
            String writer,
            String profileImage,
            Long like_count,
            Long comment_count,
            Long view_count
    ){
        this.post_id = post_id;
        this.title = title;
        this.content = content;
        this.post_image = post_image;
        this.datewritten = datewritten;
        this.writer = writer;
        this.profileImage = profileImage;
        this.like_count = like_count;
        this.comment_count = comment_count;
        this.view_count = view_count;
    }

}
