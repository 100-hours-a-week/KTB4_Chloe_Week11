import { useEffect, useState } from 'react';
import { NotificationContext } from './NotificationContext.js';
import { useAuth } from '../AuthContext/useAuth';

const BASE_URL = import.meta.env.VITE_API_URL;

// SSE로 내려오는 이벤트 전부(init/like/comment/blind)가 unreadCount를 담고 있어서
// 이벤트 종류와 무관하게 unreadCount만 그대로 반영하면 된다.
const SSE_EVENT_NAMES = ['init', 'like', 'comment', 'blind'];

export function NotificationProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }

    const eventSource = new EventSource(`${BASE_URL}/notifications/subscribe`, {
      withCredentials: true, // 쿠키 기반 인증이라 SSE 요청에도 인증 쿠키를 실어 보내야 한다
    });

    const handleNotificationEvent = (event) => {
      const payload = JSON.parse(event.data);
      setUnreadCount(payload.unreadCount);
    };

    SSE_EVENT_NAMES.forEach((eventName) => {
      eventSource.addEventListener(eventName, handleNotificationEvent);
    });

    eventSource.onerror = () => {
      // 네트워크 순단 등 일시적 오류는 브라우저가 알아서 재연결을 시도한다(readyState: CONNECTING).
      // readyState가 CLOSED라는 건 브라우저가 재연결을 포기했다는 뜻으로, 인증 쿠키 만료로 인한
      // 401/403 응답이 대표적인 경우다. 이때만 기존 401 처리 흐름(재로그인 유도)을 그대로 태운다.
      if (eventSource.readyState === EventSource.CLOSED) {
        window.dispatchEvent(new Event('auth:unauthorized'));
      }
    };

    return () => {
      SSE_EVENT_NAMES.forEach((eventName) => {
        eventSource.removeEventListener(eventName, handleNotificationEvent);
      });
      eventSource.close();
    };
  }, [isAuthenticated]);

  // 로그아웃 상태에서는 내부에 남아있는 값과 무관하게 항상 0으로 노출한다.
  // 다음 로그인 시에는 SSE의 init 이벤트가 다시 실제 값으로 갱신해준다.
  return (
    <NotificationContext.Provider value={{ unreadCount: isAuthenticated ? unreadCount : 0 }}>
      {children}
    </NotificationContext.Provider>
  );
}
