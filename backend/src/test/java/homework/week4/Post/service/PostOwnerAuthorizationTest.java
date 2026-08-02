package homework.week4.Post.service;

import homework.week4.Comment.repository.CommentRepository;
import homework.week4.Comment.service.CommentService;
import homework.week4.FileUpload.FileStorageService;
import homework.week4.Post.dto.PostRequestDto;
import homework.week4.Post.entity.Post;
import homework.week4.Post.repository.ChangeRepository;
import homework.week4.Post.repository.LikeRespoitory;
import homework.week4.Post.repository.PostRepository;
import homework.week4.Post.repository.ReportRepository;
import homework.week4.Security.Userdetails.CustomUserDetails;
import homework.week4.User.entity.User;
import homework.week4.User.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 게시글 수정 권한(@PreAuthorize)이 Mockito 단위 테스트로는 검증되지 않는다는 점을 확인하고,
 * 실제 Spring 프록시(@EnableMethodSecurity)를 통해 작성자가 아닌 사용자의 수정 요청이 막히는지 검증한다.
 */
@SpringJUnitConfig(classes = PostOwnerAuthorizationTest.MethodSecurityTestConfig.class)
class PostOwnerAuthorizationTest {

    @EnableMethodSecurity
    @Configuration
    static class MethodSecurityTestConfig {

        @Bean
        PostRepository postRepository() {
            return Mockito.mock(PostRepository.class);
        }

        @Bean
        LikeRespoitory likeRespoitory() {
            return Mockito.mock(LikeRespoitory.class);
        }

        @Bean
        ReportRepository reportRepository() {
            return Mockito.mock(ReportRepository.class);
        }

        @Bean
        ChangeRepository changeRepository() {
            return Mockito.mock(ChangeRepository.class);
        }

        @Bean
        CommentRepository commentRepository() {
            return Mockito.mock(CommentRepository.class);
        }

        @Bean
        UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        CommentService commentService() {
            return Mockito.mock(CommentService.class);
        }

        @Bean
        FileStorageService fileStorageService() {
            return Mockito.mock(FileStorageService.class);
        }

        @Bean
        PostVerifyService postVerifyService(PostRepository postRepository) {
            return new PostVerifyService(postRepository);
        }

        @Bean
        PostService postService(PostRepository postRepository, LikeRespoitory likeRespoitory,
                                 ReportRepository reportRepository, ChangeRepository changeRepository,
                                 CommentRepository commentRepository, UserService userService,
                                 CommentService commentService, PostVerifyService postVerifyService,
                                 FileStorageService fileStorageService) {
            return new PostService(postRepository, likeRespoitory, reportRepository, changeRepository,
                    commentRepository, userService, commentService, postVerifyService, fileStorageService);
        }
    }

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ChangeRepository changeRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 다른 사람의 게시글 수정을 시도하면 거부된다.")
    void modifyPost_deniedForNonOwner() {
        //준비
        Long postId = 100L;

        User owner = User.builder()
                .userId(1L)
                .email("owner@test.com")
                .nickname("owner")
                .build();

        User attacker = User.builder()
                .userId(2L)
                .email("attacker@test.com")
                .nickname("attacker")
                .build();

        Post post = new Post("원본 제목", "원본 내용", null, owner, LocalDateTime.now());

        given(postRepository.findPost(postId)).willReturn(Optional.of(post));

        CustomUserDetails principal = new CustomUserDetails(attacker);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));

        PostRequestDto request = new PostRequestDto("수정 제목", "수정 내용", null, false);

        //실행 및 검증 - 작성자가 아니므로 @PreAuthorize에서 막혀야 한다
        assertThatThrownBy(() -> postService.modifyPost(attacker.getUserId(), postId, request))
                .isInstanceOf(AccessDeniedException.class);

        // 인가 단계에서 막혔다면 게시글 수정 이력 저장까지 도달하지 않아야 한다
        verify(changeRepository, never()).save(any());
    }
}
