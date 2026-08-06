package homework.week4.Auth.service;


import homework.week4.Auth.dto.LoginRequestDto;
import homework.week4.Auth.dto.LoginResponseDto;
import homework.week4.Notification.service.NotificationService;
import homework.week4.Security.JWT.JwtToken;
import homework.week4.Security.JWT.JwtTokenProvider;
import homework.week4.User.entity.User;
import homework.week4.User.repository.UserRepository;
import homework.week4.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    // 실제 서명 검증을 위해 테스트 전용 시크릿을 직접 지정 (JwtTokenProvider가 요구하는 base64, 256bit 이상)
    private static final String TEST_JWT_SECRET = "6FMa1vF353oD9tieBfWk1aOTfIrLkXxkUvzeKhohVyw=";

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    // JwtTokenProvider는 mock이 아니라 실제 객체를 사용 -> createToken/validateToken이 실제로 서명·만료를 구성하는지 검증하기 위함
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_JWT_SECRET, userRepository);
        authService = new AuthService(authenticationManager, jwtTokenProvider, userRepository, notificationService);
    }

    @Test
    @DisplayName("이메일,비밀번호를 담아 정상적으로 로그인 요청을 하면 성공한다.")
    void loginTest(){

        //준비
        LoginRequestDto request = new LoginRequestDto(
                "chloe@test.com",
                "Chloe1234**"
        );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "chloe@test.com",
                        "Chloe1234**",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(authentication);

        User user = User.builder()
                .email("chloe@test.com")
                .nickname("chloe")
                .profileImage("이미지 경로")
                .build();

        given(userRepository.findByEmailAndIsMemberTrue("chloe@test.com"))
                .willReturn(Optional.of(user));

        given(notificationService.getUnreadCount(user.getUserId()))
                .willReturn(5L);

        long beforeCreate = System.currentTimeMillis();

        //실행
        LoginResponseDto response = authService.LoginUser(request);

        //검증 - JwtTokenProvider가 실제로 만든 토큰의 서명/만료/클레임을 확인
        JwtToken jwtToken = response.getJwtToken();
        assertThat(jwtToken.getGrantType()).isEqualTo("Bearer");
        assertThat(response.getUnreadCount()).isEqualTo(5L);

        // 서명 검증: 실제 키로 서명되지 않았다면 validateToken/파싱 단계에서 실패한다
        assertThat(jwtTokenProvider.validateToken(jwtToken.getAccessToken())).isTrue();
        assertThat(jwtTokenProvider.validateToken(jwtToken.getRefreshToken())).isTrue();

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_JWT_SECRET));

        Claims accessClaims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(jwtToken.getAccessToken()).getPayload();
        Claims refreshClaims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(jwtToken.getRefreshToken()).getPayload();

        // 클레임 구성 검증
        assertThat(accessClaims.getSubject()).isEqualTo("chloe@test.com");
        assertThat(accessClaims.get("auth", String.class)).isEqualTo("ROLE_USER");

        // 만료 시간 검증: 액세스 토큰은 발급 시점 + 1일, 리프레시 토큰은 발급 시점 + 7일 (5초 오차 허용)
        assertThat(accessClaims.getExpiration())
                .isCloseTo(new Date(beforeCreate + 86_400_000L), 5_000);
        assertThat(refreshClaims.getExpiration())
                .isCloseTo(new Date(beforeCreate + 604_800_000L), 5_000);
    }

    @Test
    @DisplayName("로그인한 이메일이 존재하지 않아서 예외가 발생한다.")
    void login_EmailNotFoundTest(){
        //준비
        LoginRequestDto request = new LoginRequestDto(
                "chloe@test.com",
                "Chloe1234**"
        );

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("User not found"));

        //실횅 및 검증
        assertThrows(UnauthorizedException.class,
                () -> authService.LoginUser(request));
    }

    @Test
    @DisplayName("위조된 토큰은 유효성 검증에 실패한다.")
    void validateToken_TamperedTokenTest(){
        //준비 - 정상 토큰을 실제로 발급받는다
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "chloe@test.com",
                        "Chloe1234**",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
        JwtToken jwtToken = jwtTokenProvider.createToken(authentication);

        // 페이로드 중간 문자를 변조 -> 서명과 내용이 어긋나게 됨
        String tamperedToken = tamperPayload(jwtToken.getAccessToken());

        //실행 및 검증
        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    private String tamperPayload(String token) {
        String[] parts = token.split("\\.");
        char[] payloadChars = parts[1].toCharArray();
        int mid = payloadChars.length / 2;
        payloadChars[mid] = payloadChars[mid] == 'A' ? 'B' : 'A';
        parts[1] = new String(payloadChars);
        return parts[0] + "." + parts[1] + "." + parts[2];
    }
}
