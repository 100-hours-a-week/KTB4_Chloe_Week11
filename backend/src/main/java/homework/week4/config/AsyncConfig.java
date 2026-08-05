package homework.week4.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

//알림 생성 리스너(@Async)가 요청 스레드와 분리되어 동작하도록 활성화.
//@EnableScheduling은 SSE 하트비트(SseEmitterRegistry) 주기 전송에 사용
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
