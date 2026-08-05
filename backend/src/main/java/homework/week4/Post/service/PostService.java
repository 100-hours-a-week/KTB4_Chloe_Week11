package homework.week4.Post.service;

import homework.week4.Comment.dto.CommentResponseDto;
import homework.week4.Comment.repository.CommentRepository;
import homework.week4.Comment.service.CommentService;
import homework.week4.FileUpload.FileStorageService;
import homework.week4.Notification.event.LikeCreatedEvent;
import homework.week4.Notification.event.PostBlindedEvent;
import homework.week4.Post.dto.*;
import homework.week4.Post.entity.Like;
import homework.week4.Post.entity.Post;
import homework.week4.Post.entity.PostChangeHistory;
import homework.week4.Post.entity.PostReportHistory;
import homework.week4.Post.repository.ChangeRepository;
import homework.week4.Post.repository.LikeRespoitory;
import homework.week4.Post.repository.PostRepository;
import homework.week4.Post.repository.ReportRepository;
import homework.week4.User.entity.User;
import homework.week4.User.service.UserService;
import homework.week4.exception.DuplicateResourceException;
import homework.week4.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final LikeRespoitory likeRespoitory;
    private final ReportRepository reportRepository;
    private final ChangeRepository changeRepository;
    private final CommentRepository commentRepository;

    private final UserService userService;
    private final CommentService commentService;
    private final PostVerifyService postVerifyService;

    private final FileStorageService fileStorageService;

    private final ApplicationEventPublisher eventPublisher;

    //게시글 등록
    @Transactional
    public PostResponseDto createPost(Long userId, @ModelAttribute PostRequestDto request ){
        User writer = userService.getValidUser(userId);
        LocalDateTime createdDateTime = LocalDateTime.now();

        String postImagePath = fileStorageService.storePostImage(request.getPostImage());

        Post post = new Post(
                request.getTitle(),
                request.getContent(),
                postImagePath,
                writer,
                createdDateTime
        );

        postRepository.save(post);

        return new PostResponseDto(
                post.getPostId(),
                post.getTitle(),
                post.getContent(),
                post.getPostImage(),
                post.getDateWritten(),
                post.getWriter().getNickname(),
                post.getWriter().getProfileImage(),
                0L,0L,0L
        );
    }

    //게시글 목록 조회
    @Transactional(readOnly = true)
    public List<PostResponseDto> listPost (Long userId, Long cursorId, int limitCount) {
        userService.checkUser(userId); //사용자 여부 확인
        Pageable pageable = PageRequest.of(0, limitCount);
        List<Post> postList = postRepository.findLatestPosts(cursorId,pageable);


        List<PostResponseDto> postsListDto = new ArrayList<>();
        for(Post postdto : postList){
            postsListDto.add(
                    new PostResponseDto(
                        postdto.getPostId(),
                        postdto.getTitle(),
                        postdto.getContent(),
                        postdto.getPostImage(),
                        postdto.getDateWritten(),
                        postdto.getWriter().getNickname(),
                        postdto.getWriter().getProfileImage(),
                        postdto.getLikeCount(),
                        postdto.getCommentCount(),
                        postdto.getViewCount()
                    )
            );
        }
        return postsListDto;
    }



    // 상세 게시글 조회
    @Transactional
    public PostDetailResponseDto getPost(Long userId, Long postId){
        User user = userService.getValidUser(userId);
        Post post = postVerifyService.getValidPost(postId);

        Boolean is_liked =likeRespoitory.existsByPostAndUser(post,user);

        Boolean isOwner = false;

        if(user.getUserId().equals(post.getWriter().getUserId()) ){
            isOwner = true;
        }

        post.viewCountIncrement();

        PostResponseDto postResponseDto = new PostResponseDto(
                post.getPostId(),
                post.getTitle(),
                post.getContent(),
                post.getPostImage(),
                post.getDateWritten(),
                post.getWriter().getNickname(),
                post.getWriter().getProfileImage(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getViewCount(),
                isOwner

        );

        List<CommentResponseDto> commentResponseDto = commentService.listComment(postId);

        return new PostDetailResponseDto(postResponseDto,commentResponseDto,is_liked);
    }


    //게시글 수정을 위한 정보 조회
    @Transactional(readOnly = true)
    @PreAuthorize("@postVerifyService.getValidPost(#postId).getWriter().getUserId().equals(authentication.principal.userId)")
    public PostEditResponseDto editPost(Long userId,Long postId){
        userService.checkUser(userId);
        Post post = postVerifyService.getValidPost(postId);

        return new PostEditResponseDto(
                post.getTitle(),
                post.getContent(),
                post.getPostImage()
        );
    }



    //게시글 수정
    @Transactional
    @PreAuthorize("@postVerifyService.getValidPost(#postId).getWriter().getUserId().equals(authentication.principal.userId)")
    public void modifyPost (Long userId, Long postId, @ModelAttribute PostRequestDto request){
        userService.checkUser(userId); //에외가 일어나면 밑에도 실행 X

        Post post = postVerifyService.getValidPost(postId);
        LocalDateTime changedAt = LocalDateTime.now();

        //게시글 수정 이력 보존
        PostChangeHistory postChangeHistory = new PostChangeHistory(
                post,
                changedAt,
                post.getTitle(),
                post.getContent(),
                post.getPostImage()
        );

        //수정 이력 저장
        changeRepository.save(postChangeHistory);

        //게시글 이미지 저장
        String postImagePath;
        if (request.getPostImage() != null && !request.getPostImage().isEmpty()) {
            postImagePath = fileStorageService.storePostImage(request.getPostImage());
        } else if (request.isRemoveImage()) {
            postImagePath = null; // 프론트에서 명시적으로 이미지 삭제를 요청한 경우
        } else {
            postImagePath = post.getPostImage(); // 기존 값 그대로 유지
        }

        //게시글 수정
        post.modifyPost(
                request.getTitle(),
                request.getContent(),
                postImagePath,
                changedAt
        );

    }

    //게시글 삭제
    @Transactional
    @PreAuthorize("@postVerifyService.getValidPost(#postId).getWriter().getUserId().equals(authentication.principal.userId)")
    public void deletePost(Long userId, Long postId){
        userService.checkUser(userId);

        Post post = postVerifyService.getValidPost(postId);
        LocalDateTime deletedDateTime = LocalDateTime.now();

        post.isDeleted(deletedDateTime);
        //게시글을 참조하고 있는 댓글 다 삭제..
        commentRepository.deletePostIdComment(postId,deletedDateTime);

    }

    //게시글 좋아요 등록
    @Transactional
    public PostLikeResponseDto createLike(Long userId, Long postId){

        //게시글 & 사용자 검증 다 함.
        User user = userService.getValidUser(userId);
        Post post = postVerifyService.getValidPost(postId);

        //좋아요 이미 있는지 확인
        likeRespoitory.findByUserUserIdAndPostPostId(user.getUserId(), post.getPostId())
                .ifPresent(like -> {
                    throw new DuplicateResourceException("이미 존재하는 좋아요입니다.","like");
                });

        Like like = new Like(user,post);

        likeRespoitory.save(like);

        Long likeCount = likeRespoitory.countByPostPostId(postId);

        post.likeCount(likeCount);

        //본인 게시글에 본인이 좋아요를 누른 경우는 알림을 만들지 않는다
        if (!user.getUserId().equals(post.getWriter().getUserId())) {
            eventPublisher.publishEvent(new LikeCreatedEvent(
                    post.getPostId(),
                    user.getUserId(),
                    post.getWriter().getUserId(),
                    LocalDateTime.now()
            ));
        }

        return new PostLikeResponseDto(likeCount);
    }

    //게시글 좋아요 취소
    @Transactional
    public PostLikeResponseDto cancelLike(Long userId, Long postId){

        User user = userService.getValidUser(userId);
        Post post = postVerifyService.getValidPost(postId);

        Like like = likeRespoitory.findByUserUserIdAndPostPostId(user.getUserId(),post.getPostId())
                .orElseThrow(() -> new NotFoundException("해당 좋아요가 존재하지 않습니다."));

        //좋아요 삭제하고
        likeRespoitory.delete(like);

        //삭제가 적용된 좋아요 수 반환
        Long likeCount = likeRespoitory.countByPostPostId(postId);

        //좋아요 수 게시글 likeCount에 업데이트
        post.likeCount(likeCount);

        return new PostLikeResponseDto(likeCount);

    }

    //해당 신고가 존재하는지 확인
    void verifyUserInPost(Long userId, Long postId){
        if (reportRepository.existsByUserUserIdAndPostPostId(userId, postId)) {
            throw new DuplicateResourceException("이미 신고한 게시글입니다.","report");
        }
    }


    //게시글 신고
    @Transactional
    public PostReportResponseDto reportPost(Long userId, Long postId){
        User user = userService.getValidUser(userId);
        Post post = postVerifyService.getValidPost(postId);

        //이미 신고한 게시글인지 확인
        verifyUserInPost(userId,postId);

        LocalDateTime reportedDateTime = LocalDateTime.now();

        PostReportHistory postReportHistory = new PostReportHistory(user,post,reportedDateTime);

        //신고 저장
        reportRepository.save(postReportHistory);

        //count 해서 신고 횟수 계산
        int reportCount = reportRepository.countByPostPostId(postId);

        if(reportCount >= 5){
            post.PostHideTrue();
            eventPublisher.publishEvent(new PostBlindedEvent(
                    post.getPostId(),
                    post.getWriter().getUserId(),
                    reportedDateTime
            ));
        }

        return new PostReportResponseDto(
                post.getPostId(),
                post.getPostHide()
        );
    }



}
