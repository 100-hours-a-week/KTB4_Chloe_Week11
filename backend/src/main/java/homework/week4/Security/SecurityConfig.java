package homework.week4.Security;

import homework.week4.Security.JWT.JWTFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthenticationConfiguration authenticationConfiguration;

    private final JWTFilter jwtFilter;

    // AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    // 필터 체인
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

                //CSRF 비활성화
                .csrf(csrf -> csrf.disable())
                //h2 DB 확인을 위해서..
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                //폼 로그인, Basic 인증 비활성화
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                //세션 설정: STATELESS
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                //URL 패턴별로 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        //이미지 요청 허용
                        .requestMatchers("/images/**").permitAll()
                        //회원가입,로그인은 허용
                        .requestMatchers("/users/signup","/auth/**").permitAll()
                        .anyRequest().hasRole("USER")
                )

                // 5. 필터 추가: UsernamePasswordAuthenticationFilter 앞에 JwtFilter 배치
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    //passwordEncoder 구현
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 출처 설정
        configuration.setAllowedOrigins(List.of("http://127.0.0.1:5500","http://localhost:5174"));

        // 허용할 HTTP 메서드 설정
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));

        // 허용할 HTTP 헤더 설정
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // 예비 요청(Preflight) 결과 캐시 시간 설정
        configuration.setMaxAge(3600L);

        // SSE 인증 쿠키가 크로스 오리진 요청에도 실려가도록 허용 (EventSource의 withCredentials와 짝을 이룬다)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 모든 경로에 대해 위에서 정의한 CORS 정책 적용
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
