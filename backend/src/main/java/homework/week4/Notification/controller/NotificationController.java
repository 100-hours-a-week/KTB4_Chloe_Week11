package homework.week4.Notification.controller;

import homework.week4.Notification.dto.NotificationListResponseDto;
import homework.week4.Notification.dto.NotificationReadResponseDto;
import homework.week4.Notification.entity.Notification;
import homework.week4.Notification.service.NotificationService;
import homework.week4.Notification.sse.SseEmitterRegistry;
import homework.week4.Security.Userdetails.CustomUserDetails;
import homework.week4.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponseDto>> listNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly
    ) {
        Long userId = userDetails.getUserId();
        NotificationListResponseDto result = notificationService.listNotifications(userId, cursor, limit, unreadOnly);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("알림 목록 조회 성공", result));
    }

    @PatchMapping("/{notification_id}/read")
    public ResponseEntity<ApiResponse<NotificationReadResponseDto>> readNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("notification_id") Long notificationId
    ) {
        Long userId = userDetails.getUserId();
        Long unreadCount = notificationService.markAsRead(userId, notificationId);

        // 댓글·좋아요는 data가 null, 블라인드만 unreadCount를 담아 응답한다
        NotificationReadResponseDto data = unreadCount != null ? new NotificationReadResponseDto(unreadCount) : null;

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("알림 읽음 처리 완료", data));
    }

    @DeleteMapping("/{notification_id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("notification_id") Long notificationId
    ) {
        Long userId = userDetails.getUserId();
        notificationService.deleteNotification(userId, notificationId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of("알림 삭제 완료", null));
    }

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
