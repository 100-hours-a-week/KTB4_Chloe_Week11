package homework.week4.Notification.controller;

import homework.week4.Notification.entity.Notification;
import homework.week4.Notification.service.NotificationService;
import homework.week4.Notification.sse.SseEmitterRegistry;
import homework.week4.Security.Userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            HttpServletResponse response
    ) {
        Long userId = userDetails.getUserId();

        // nginx가 응답을 버퍼링하지 않고 바로 클라이언트로 흘려보내게 하는 헤더
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");

        SseEmitter emitter = sseEmitterRegistry.register(userId);

        if (!StringUtils.hasText(lastEventId)) {
            // 최초 연결: 현재 시점 안 읽은 알림 개수를 init 이벤트로 한 번 보내준다
            Long unreadCount = notificationService.getUnreadCount(userId);
            sseEmitterRegistry.send(userId, "init", Map.of("unreadCount", unreadCount), null);
        } else {
            resendMissedNotifications(userId, lastEventId);
        }

        return emitter;
    }

    //재연결: 클라이언트가 보낸 Last-Event-ID(알림 생성 일시)보다 최신인 알림을 놓치지 않고 다시 보내준다
    private void resendMissedNotifications(Long userId, String lastEventId) {
        LocalDateTime after;
        try {
            after = LocalDateTime.parse(lastEventId);
        } catch (DateTimeParseException e) {
            log.warn("잘못된 Last-Event-ID 형식이라 재전송을 건너뜁니다. userId={}, lastEventId={}", userId, lastEventId);
            return;
        }

        List<Notification> missed = notificationService.getMissedNotifications(userId, after);
        for (Notification notification : missed) {
            sseEmitterRegistry.send(
                    userId,
                    notification.getType().name().toLowerCase(),
                    notificationService.buildPushPayload(notification),
                    notification.getCreatedAt().toString()
            );
        }
    }
}
