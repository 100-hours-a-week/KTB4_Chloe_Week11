package homework.week4.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

//알림 생성 리스너(@Async)가 요청 스레드와 분리되어 동작하도록 활성화
@Configuration
@EnableAsync
public class AsyncConfig {
}
