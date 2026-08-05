package homework.week4.Auth.controller;

import homework.week4.Auth.dto.LoginRequestDto;
import homework.week4.Auth.dto.LoginResponseDto;
import homework.week4.Auth.service.AuthService;
import homework.week4.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins="http://127.0.0.1:5500")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> LoginUser (@Valid @RequestBody LoginRequestDto request){

        LoginResponseDto result = authService.LoginUser(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("로그인 성공",result));

    }
}
