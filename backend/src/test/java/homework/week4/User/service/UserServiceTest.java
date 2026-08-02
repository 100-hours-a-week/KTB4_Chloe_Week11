package homework.week4.User.service;

import homework.week4.FileUpload.FileStorageService;
import homework.week4.User.dto.*;
import homework.week4.User.entity.User;
import homework.week4.User.repository.UserRepository;
import homework.week4.exception.DuplicateResourceException;
import homework.week4.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MultipartFile profileImage;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserService userService;



    // 공통으로 쓸 필드 선언
    private Long userId;
    private LocalDateTime createdAt;
    private SignUpRequestDto MocksignUprequest;

    private User createTestUser(){
        return new User(
                "chloe@test.com",
                "Chloe1234**",
                "chloe",
                "이미지 경로",
                createdAt
        );
    }
    private User testUser;

    @BeforeEach
    void setUp(){
        userId = 1L;
        createdAt = LocalDateTime.of(2026, 7, 6, 18, 30, 0);

        MocksignUprequest = new SignUpRequestDto(
                "chloe@test.com",
                "Chloe1234**",
                "chloe",
                profileImage
        );

        testUser = createTestUser();
    }

    @Test
    @DisplayName("사용자의 이메일, 비밀번호,유저 닉네임, 프로필 이미지를 정상적으로 요청하면 회원 가입을 성공한다. ")
    void signUpTest(){

        //준비
        given(userRepository.existsByEmail("chloe@test.com")).willReturn(false);
        given(userRepository.existsByNickname("chloe")).willReturn(false);
        given(passwordEncoder.encode("Chloe1234**")).willReturn("encodedPassword");
        given(fileStorageService.storeProfileImage(profileImage)).willReturn("/UploadPhoto/ProfileImage/profile.jpg");

        //실행
        userService.createUser(MocksignUprequest);

        // 검증
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("chloe@test.com");
        assertThat(savedUser.getNickname()).isEqualTo("chloe");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword"); // 암호화된 값으로 들어갔는지까지 확인
    }

    @Test
    @DisplayName("회원가입 시에, 프로필 이미지 파일 업로드 중 예외가 발생한다.")
    void signUp_RunTimeExceptionTest() throws IOException {

        //준비
        given(userRepository.existsByEmail("chloe@test.com")).willReturn(false);
        given(userRepository.existsByNickname("chloe")).willReturn(false);

        SignUpRequestDto signUprequest = new SignUpRequestDto(
                "chloe@test.com",
                "Chloe1234**",
                "chloe",
                profileImage
        );

        given(fileStorageService.storeProfileImage(any(MultipartFile.class)))
                .willThrow(new RuntimeException("파일 저장 실패"));

        // 검증 및 검증
        assertThrows(RuntimeException.class,
                () -> userService.createUser(signUprequest));
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 요청을 하면 예외가 발생한다.")
    void DuplicateEmailTest(){
        //준비
        given(userRepository.existsByEmail("chloe@test.com")).willReturn(true);


        //실행 및 준비
        assertThrows(DuplicateResourceException.class,
                () -> userService.createUser(MocksignUprequest));
    }

    @Test
    @DisplayName("중복된 닉네임으로 회원가입 요청을 하면 예외가 발생한다.")
    void DuplicateNicknameTest(){
        //준비
        given(userRepository.existsByEmail("chloe@test.com")).willReturn(false);
        given(userRepository.existsByNickname("chloe")).willReturn(true);

        //실행 및 준비
        assertThrows(DuplicateResourceException.class,
                () -> userService.createUser(MocksignUprequest));
    }

    @Test
    @DisplayName("로그인 후 발급된 토큰을 헤더에 붙이고 회원 정보 조회를 요청하면 성공한다.")
    void userGetTest(){
        //준비
        UserGetResponseDto response = new UserGetResponseDto(
                "chloe@test.com",
                "chloe",
                "이미지 경로"
        );

        given(userRepository.findByuserIdAndIsMemberTrue(userId))
                .willReturn(Optional.of(testUser));


        //실행
        UserGetResponseDto result = userService.lookupUser(userId);

        //검증
        assertThat(result.getEmail()).isEqualTo(response.getEmail());
        assertThat(result.getNickname()).isEqualTo(response.getNickname());
        assertThat(result.getProfileImage()).isEqualTo(response.getProfileImage());

    }

    @Test
    @DisplayName("회원 정보가 존재하지 않는다.")
    void usetGet_NotFound(){
        //준비
        given(userRepository.findByuserIdAndIsMemberTrue(userId))
                .willReturn(Optional.empty());

        //실행 및 검증
        assertThrows(NotFoundException.class,
                () -> userService.lookupUser(userId));
    }

    @Test
    @DisplayName("닉네임,이미지를 담아 정상적으로 요청하면 회원 정보 수정 성공한다.")
    void changeUserTest(){
        //준비
        UserChangeRequestDto request = new UserChangeRequestDto(
                "chloe1",
                profileImage
        );

        UserChangeResponseDto response = new UserChangeResponseDto(
                "chloe1",
                "/UploadPhoto/ProfileImage/profile.jpg"

        );

        given(userRepository.existsByNicknameAndUserIdNot(request.getNickname(), userId))
                .willReturn(false);

        given(userRepository.findByuserIdAndIsMemberTrue(userId))
                .willReturn(Optional.of(testUser));
        given(fileStorageService.storeProfileImage(profileImage)).willReturn("/UploadPhoto/ProfileImage/profile.jpg");

        //실행
        UserChangeResponseDto result = userService.changeUser(userId,request);

        //검증
        assertThat(result.getNickname()).isEqualTo(response.getNickname());
        assertThat(result.getProfileImage()).isEqualTo(response.getProfileImage());
        assertThat(testUser.getUpdatedAt()).isNotNull();


    }
    @Test
    @DisplayName("회원 정보 수정 시에, 프로필 이미지 파일 업로드 중 예외가 발생한다.")
    void changeUser_RunTimeExceptionTest() throws IOException{
        //준비
        UserChangeRequestDto request = new UserChangeRequestDto(
                "chloe1",
                profileImage
        );

        given(userRepository.existsByNicknameAndUserIdNot(request.getNickname(), userId))
                .willReturn(false);

        given(userRepository.findByuserIdAndIsMemberTrue(userId))
                .willReturn(Optional.of(testUser));

        given(fileStorageService.storeProfileImage(any(MultipartFile.class)))
                .willThrow(new RuntimeException("파일 저장 실패"));

        // 검증 및 검증
        assertThrows(RuntimeException.class,
                () -> userService.changeUser(userId,request));



    }

    @Test
    @DisplayName("수정 가능한 회원 정보가 존재하지 않아서 예외가 발생한다.")
    void changeUser_NotFoundTest(){
        //준비
        UserChangeRequestDto request = new UserChangeRequestDto(
                "chloe1",
                profileImage
        );

        given(userRepository.findByuserIdAndIsMemberTrue(userId))
                .willReturn(Optional.empty());


        //실행 및 검증
        assertThrows(NotFoundException.class,() -> userService.changeUser(userId,request));


    }

    @Test
    @DisplayName("사용자가 변경할 비밀번호를 담아 정상적으로 요청을 보내면 비밀번호 변경이 성공한다")
    void changePassWordTest(){
        //준비
        String rawPassword = "newPassword123!";
        String encodedPassword = "encoded_newPassword123!";

        User user = User.builder().password("oldEncodedPassword").build();

        given(userRepository.findByuserIdAndIsMemberTrue(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);

        UserPasswordRequestDto request = new UserPasswordRequestDto(rawPassword);

        //실행
        userService.changePassWord(userId, request);

        //검증
        // 1. 올바른 입력으로 호출됐는지
        then(passwordEncoder).should().encode(rawPassword);
        // 2. 결과값이 실제로 반영됐는지
        assertThat(user.getPassword()).isEqualTo(encodedPassword);


    }

    @Test
    @DisplayName("회원 탈퇴 요청이 정상적으로 들어오면 회원 탈퇴가 성공한다. ")
    void deleteUserTest(){
        User user = new User(
                "chloe@test.com",
                "Chloe1234**",
                "chloe",
                "이미지 경로",
                createdAt
        );

        UserDeleteResponseDto response = new UserDeleteResponseDto(
                "chloe",
                false
        );

        given(userRepository.findByuserIdAndIsMemberTrue(userId))
                .willReturn(Optional.of(user));

        //실행
        UserDeleteResponseDto result = userService.deleteUser(userId);

        //검증
        assertThat(result.getNickname()).isEqualTo(response.getNickname());
        assertThat(result.getIs_member()).isEqualTo(response.getIs_member());
        assertThat(user.getDeletedAt()).isNotNull();

    }

    @Test
    @DisplayName("checkUser의 테스트")
    void checkUser_NotFoundTest(){
        //준비
        given(userRepository.existsByuserIdAndIsMemberTrue(userId)).willReturn(false);

        //실행 및 검증
        assertThrows(NotFoundException.class,
                () -> userService.checkUser(userId));
    }

}
