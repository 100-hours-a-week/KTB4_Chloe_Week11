package homework.week4.Post.controller;

import homework.week4.Post.dto.*;
import homework.week4.Post.service.PostService;
import homework.week4.Security.Userdetails.CustomUserDetails;
import homework.week4.User.service.UserService;
import homework.week4.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins="http://127.0.0.1:5500")
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponseDto>>>listPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "cursor", required = false) Long cursorId, //null 허용
            @RequestParam(name = "limit", defaultValue = "10") int limit_count
    ){
        Long userId = userDetails.getUserId();
        List<PostResponseDto> result= postService.listPost(userId,cursorId, limit_count);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("게시글목록 조회_성공",result));

    }

    //게시글 생성
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<PostResponseDto>> createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute PostRequestDto request) {

        Long userId = userDetails.getUserId();
        PostResponseDto result = postService.createPost(userId, request);

        PostResponseDto response = new PostResponseDto(
                result.getPost_id(),
                result.getTitle(),
                result.getContent(),
                result.getPost_image(),
                result.getDatewritten(),
                result.getWriter(),
                result.getProfileImage(),
                result.getLike_count(),
                result.getComment_count(),
                result.getView_count(),
                true //게시글 생성했을 때는 무조건 true
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of("게시글 생성 완료", response));
    }

    @GetMapping( "/{post_id}")
    public ResponseEntity<ApiResponse<PostDetailResponseDto>> getPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long post_id
    ){
        Long userId = userDetails.getUserId();
        PostDetailResponseDto result = postService.getPost(userId,post_id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("상세게시글 조회 성공",result));
    }


    @GetMapping(value = "/{post_id}/edit")
    public ResponseEntity<ApiResponse<PostEditResponseDto>> editPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long post_id
    ){
        Long userId = userDetails.getUserId();
        PostEditResponseDto result = postService.editPost(userId,post_id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("수정페이지 정보 조회 성공",result));
    }



    @PutMapping(value = "/{post_id}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> modifyPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long post_id,
            @ModelAttribute PostRequestDto request
    ){
        Long userId = userDetails.getUserId();
        postService.modifyPost(userId, post_id, request);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @DeleteMapping("/{post_id}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long post_id
    ){
        Long userId = userDetails.getUserId();
        postService.deletePost(userId,post_id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PostMapping("/{post_id}/declaration")
    public ResponseEntity<ApiResponse<PostReportResponseDto>> declarePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long post_id
    ){
        Long userId = userDetails.getUserId();
        PostReportResponseDto result = postService.reportPost(userId,post_id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("게시글 신고 요청 완료",result));
    }

    //좋아요 등록
    @PostMapping("/{post_id}/like")
    public ResponseEntity<ApiResponse<PostLikeResponseDto>> likeCreatePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long post_id
            ){
        Long userId = userDetails.getUserId();
        PostLikeResponseDto result = postService.createLike(userId,post_id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("좋아요 요청 완료",result));
    }

    //좋아요 취소
    @DeleteMapping("/{post_id}/like")
    public ResponseEntity<ApiResponse<PostLikeResponseDto>> likeCancelPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long post_id

    ){
        Long userId = userDetails.getUserId();
        PostLikeResponseDto result = postService.cancelLike(userId,post_id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("좋아요 삭제 완료",result));
    }

}
