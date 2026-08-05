package homework.week4.User.controller;


import homework.week4.Auth.dto.LoginResponseDto;
import homework.week4.Security.Userdetails.CustomUserDetails;
import homework.week4.User.dto.*;

import homework.week4.response.ApiResponse;
import homework.week4.User.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@CrossOrigin(origins="http://127.0.0.1:5500")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //회원 가입 -> 사용자 생성
    @PostMapping (
            value = "/signup",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<SignUpResponseDto>> createUser(@Valid @ModelAttribute SignUpRequestDto request) {
        SignUpResponseDto result = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of("회원 가입 완료",result));
    }

    //회원 정보 조회
    @GetMapping
    public ResponseEntity<ApiResponse<UserGetResponseDto>> lookupUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long userId = userDetails.getUserId();
        UserGetResponseDto result = userService.lookupUser(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("회원정보 조회 완료",result));
    }

    //회원 탈퇴
    @DeleteMapping
    public ResponseEntity<ApiResponse<UserDeleteResponseDto>> deleteUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        UserDeleteResponseDto result = userService.deleteUser(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("회원 탈퇴 완료",result));
    }

    //회원 정보 수정
    @PatchMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<UserChangeResponseDto>> changeUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid  @ModelAttribute UserChangeRequestDto request) {

        Long userId = userDetails.getUserId();

        UserChangeResponseDto result = userService.changeUser(userId,request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("회원정보 변경 완료",result));

    }

    //비빌번호 변경
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserPasswordRequestDto request
    ){

        Long userId = userDetails.getUserId();
        userService.changePassWord(userId, request);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }



}
