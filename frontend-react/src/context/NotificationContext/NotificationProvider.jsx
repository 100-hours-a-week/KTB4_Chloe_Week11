import { fetchEventSource } from '@microsoft/fetch-event-source';
import { useEffect, useState } from 'react';
import { NotificationContext } from './NotificationContext.js';
import { useAuth } from '../AuthContext/useAuth';

const BASE_URL = import.meta.env.VITE_API_URL;

// SSE로 내려오는 이벤트 전부(init/like/comment/blind)가 unreadCount를 담고 있어서
// 이벤트 종류와 무관하게 unreadCount만 그대로 반영하면 된다.
const SSE_EVENT_NAMES = new Set(['init', 'like', 'comment', 'blind']);

// onerror에서 던지면 재시도를 완전히 멈춘다 (인증 실패처럼 재시도해봐야 소용없는 경우).
class FatalError extends Error {}
// onerror에서 던지면 라이브러리 기본 백오프로 재시도를 계속한다 (일시적 네트워크/서버 오류).
class RetriableError extends Error {}

export function NotificationProvider({ children }) {
  const { isAuthenticated, accessToken } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }

    const controller = new AbortController();

    fetchEventSource(`${BASE_URL}/notifications/subscribe`, {
      signal: controller.signal,
      openWhenHidden: true, // 탭이 백그라운드로 가도 연결을 유지한다
      headers: {
        // request.js와 동일하게 Authorization 헤더로 인증한다 (네이티브 EventSource는 커스텀
        // 헤더를 못 보내서 쿠키로 우회해야 했지만, fetch 기반이라 헤더를 그대로 실어 보낼 수 있다)
        Authorization: `Bearer ${accessToken}`,
      },

      async onopen(response) {
        if (response.ok) {
          return;
        }
        if (response.status === 401 || response.status === 403) {
          // 인증 만료: 기존 401 처리 흐름(재로그인 유도)을 그대로 태우고 재시도는 멈춘다
          window.dispatchEvent(new Event('auth:unauthorized'));
          throw new FatalError(`SSE 인증 실패: ${response.status}`);
        }
        throw new RetriableError(`SSE 연결 실패: ${response.status}`);
      },

      onmessage(event) {
        if (!SSE_EVENT_NAMES.has(event.event)) {
          return;
        }
        try {
          const payload = JSON.parse(event.data);
          setUnreadCount(payload.unreadCount);
        } catch (e) {
          console.error('알림 페이로드 파싱 실패', e);
        }
      },

      onclose() {
        // 서버가 정상적으로 연결을 끊어도(emitter 타임아웃 등) 재연결을 시도해야 한다
        throw new RetriableError('SSE 연결이 서버 측에서 종료됨');
      },

      onerror(err) {
        // 401,403 에러일 경우에는 재시도 해도 의미가 없기 때문에 재시도 중단
        if (err instanceof FatalError) {
          throw err; // 다시 던져서 재시도를 중단시킨다
        }
        console.warn('SSE 연결 오류, 재시도합니다', err);
        // 리턴 없이 넘어가면 라이브러리 기본 재시도 간격을 그대로 쓴다
      },
    }).catch((err) => {
      if (!(err instanceof FatalError)) {
        console.error('SSE 연결이 예기치 않게 완전히 종료됐습니다', err);
      }
    });

    return () => controller.abort();
  }, [isAuthenticated, accessToken]);

  // 로그아웃 상태에서는 내부에 남아있는 값과 무관하게 항상 0으로 노출한다.
  // 다음 로그인 시에는 SSE의 init 이벤트가 다시 실제 값으로 갱신해준다.
  return (
    <NotificationContext.Provider value={{ unreadCount: isAuthenticated ? unreadCount : 0 }}>
      {children}
    </NotificationContext.Provider>
  );
}
