package homework.week4.Notification.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//사용자 한 명당 SseEmitter 하나를 userId 기준으로 관리한다 (Map<Long, SseEmitter>).
//여러 탭·기기 동시 접속 시 연결을 여러 개 유지하는 것은 추후 과제로 남겨두고, 지금은 1인 1연결만 지원한다.
@Slf4j
@Component
public class SseEmitterRegistry {

    private static final long TIMEOUT_MILLIS = 24 * 60 * 60 * 1000L; // 24시간. 인증 쿠키 만료(1일)와 맞춰 자연스러운 재연결 주기를 만든다

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    //신규 구독 연결 등록. 같은 사용자의 기존 연결이 있으면 명시적으로 종료한 뒤 새 연결로 교체한다
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);

        SseEmitter previous = emitters.put(userId, emitter);
        if (previous != null) {
            previous.complete();
        }

        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(throwable -> emitters.remove(userId, emitter));

        return emitter;
    }

    //해당 사용자가 연결 중이면 이벤트를 보낸다. eventId는 재연결 시 Last-Event-ID로 쓰이는 값({알림 생성 일시}_{notificationId})이며, 없는 이벤트(init 등)는 null로 둔다
    public void send(Long userId, String eventName, Object data, String eventId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event().name(eventName).data(data);
            if (eventId != null) {
                event.id(eventId);
            }
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            emitters.remove(userId, emitter);
            emitter.complete();
        }
    }

    //15초 주기 하트비트. 빈 comment 라인만 보내 클라이언트 메시지 리스너에는 영향을 주지 않으면서 중간 프록시의 유휴 타임아웃으로 인한 연결 종료를 막는다
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeatToAll() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(userId, emitter);
                emitter.complete();
            }
        });
    }
}
