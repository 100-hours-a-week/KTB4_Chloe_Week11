package homework.week4.Auth.controller;

import homework.week4.Auth.dto.LoginRequestDto;
import homework.week4.Auth.dto.LoginResponseDto;
import homework.week4.Auth.service.AuthService;
import homework.week4.Security.JWT.JWTFilter;
import homework.week4.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@CrossOrigin(origins="http://127.0.0.1:5500")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> LoginUser (@Valid @RequestBody LoginRequestDto request){

        LoginResponseDto result = authService.LoginUser(request);

        // SSE 구독(GET /notifications/subscribe) 전용 httpOnly 쿠키. access token과 동일한 값을 담아
        // REST 인증(Authorization 헤더)과는 분리된 방식으로 SSE 연결만 쿠키로 인증할 수 있게 한다.
        // 액세스 토큰 만료(1일)와 동일하게 맞췄다.
        // 주의: SameSite=None은 Secure 쿠키에서만 브라우저가 허용하므로, 로컬 http 개발 환경(127.0.0.1:5500 -> localhost:8080)에서는
        // 브라우저가 이 쿠키 저장을 거부할 수 있다. HTTPS 배포 환경을 기준으로 한 설정이다.
        ResponseCookie sseCookie = ResponseCookie.from(JWTFilter.ACCESS_TOKEN_COOKIE_NAME, result.getJwtToken().getAccessToken())
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(1))
                .sameSite("None")
                .secure(true)
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, sseCookie.toString())
                .body(ApiResponse.of("로그인 성공",result));

    }
}
