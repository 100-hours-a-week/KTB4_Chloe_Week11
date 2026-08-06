package homework.week4.Security.JWT;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component //스프링 컨테이너에서 관리되게끔 해주는
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    //SSE 구독(EventSource)은 커스텀 헤더를 보낼 수 없어, 로그인 시 발급되는 이 httpOnly 쿠키로만 인증한다.
    //REST 요청은 기존과 동일하게 Authorization 헤더를 우선 사용하고, 헤더가 없을 때만 쿠키를 폴백으로 확인한다.
    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && tokenProvider.validateToken(token)) {
            // 3. 토큰이 유효하면 Authentication 객체를 가져와서 SecurityContext에 저장
            Authentication authentication = tokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return resolveTokenFromCookie(request);
    }

    private String resolveTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
