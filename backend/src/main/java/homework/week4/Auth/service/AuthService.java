package homework.week4.Auth.service;

import homework.week4.Auth.dto.LoginRequestDto;
import homework.week4.Auth.dto.LoginResponseDto;
import homework.week4.Notification.service.NotificationService;
import homework.week4.Security.JWT.JwtToken;
import homework.week4.Security.JWT.JwtTokenProvider;
import homework.week4.User.entity.User;
import homework.week4.User.repository.UserRepository;
import homework.week4.exception.NotFoundException;
import homework.week4.exception.UnauthorizedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public LoginResponseDto LoginUser ( @RequestBody LoginRequestDto request){
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
            JwtToken jwtToken = jwtTokenProvider.createToken(authentication);

            //프로필 이미지 반환
            User user = userRepository.findByEmailAndIsMemberTrue(request.getEmail())
                    .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));

            //안 읽은 알림 개수 반환
            Long unreadCount = notificationService.getUnreadCount(user.getUserId());

            return new LoginResponseDto(jwtToken, user.getProfileImage(), unreadCount);

        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

    }
}
