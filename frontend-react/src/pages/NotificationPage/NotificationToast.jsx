import { useEffect, useRef } from 'react';
import { CommentIcon } from '../../components/icons/StatIcons';
import { BlindBadgeIcon, BellIcon } from '../../components/icons/NotificationIcons';
import { buildToastMessage } from './notificationMock';
import './NotificationToast.css';

const DURATION_MS = 2600;

// 개별 토스트 하나. 자체 타이머를 갖고 hover 시 멈췄다가 남은 시간만큼 이어서 진행한다.
function ToastItem({ toast, onDismiss }) {
  const timerRef = useRef(null);
  const remainingRef = useRef(DURATION_MS);
  const startedAtRef = useRef(null);

  useEffect(() => {
    startedAtRef.current = Date.now();
    timerRef.current = setTimeout(() => onDismiss(toast.id), DURATION_MS);
    return () => clearTimeout(timerRef.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [toast.id]);

  const handleMouseEnter = () => {
    clearTimeout(timerRef.current);
    remainingRef.current -= Date.now() - startedAtRef.current;
  };
  const handleMouseLeave = () => {
    startedAtRef.current = Date.now();
    timerRef.current = setTimeout(() => onDismiss(toast.id), Math.max(remainingRef.current, 300));
  };

  const isOverflow = toast.kind === 'overflow';
  const typeClass = isOverflow ? 'type-overflow' : toast.type === 'COMMENT' ? 'type-comment' : 'type-blind';
  const text = isOverflow ? '새로운 알림이 존재합니다' : buildToastMessage(toast.type, toast.postTitle);

  return (
    <div
      className={`notif-toast ${typeClass}`}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onClick={() => onDismiss(toast.id, toast)}
      role="button"
      tabIndex={0}
    >
      <span className="notif-toast-icon">
        {isOverflow && <BellIcon width={14} height={14} />}
        {!isOverflow && toast.type === 'COMMENT' && <CommentIcon width={14} height={14} />}
        {!isOverflow && toast.type === 'BLIND' && <BlindBadgeIcon width={14} height={14} />}
      </span>
      <span className="notif-toast-text">{text}</span>
    </div>
  );
}

// 우측 상단 토스트 스택
function NotificationToastStack({ toasts, onDismiss, onCommentClick, onGoToNotifications }) {
  return (
    <div className="notif-toast-stack" aria-live="polite">
      {toasts.map((toast) => (
        <ToastItem
          key={toast.id}
          toast={toast}
          onDismiss={(id, dismissed) => {
            onDismiss(id);
            if (!dismissed) return;
            if (dismissed.kind === 'overflow' || dismissed.type === 'BLIND') onGoToNotifications();
            else onCommentClick(dismissed);
          }}
        />
      ))}
    </div>
  );
}

export default NotificationToastStack;
