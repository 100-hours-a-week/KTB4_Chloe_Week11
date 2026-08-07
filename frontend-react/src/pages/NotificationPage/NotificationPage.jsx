import { useEffect, useMemo, useRef, useState } from 'react';
import NotificationCard from './NotificationCard';
import NotificationToastStack from './NotificationToast';
import { useToastQueue } from './useToastQueue';
import { INITIAL_NOTIFICATIONS, OLDER_NOTIFICATIONS_POOL, makeDemoNotification } from './notificationMock';
import './NotificationPage.css';

// 이 페이지는 디자인 전용 구현이다. 목록, 읽음 처리, 삭제, 토스트가 전부 로컬 목데이터와
// 로컬 state로만 동작하고, 실제 API(notificationRequest.js)나 NotificationContext는
// 이번 작업에서 건드리지 않았다. 실제 연동은 이후 별도 작업에서 진행한다.

function groupLabel(createdAt) {
  const date = new Date(createdAt);
  const now = new Date();
  const isToday = date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate();
  return isToday ? '오늘' : '이전 알림';
}

function NotificationPage() {
  const [notifications, setNotifications] = useState(INITIAL_NOTIFICATIONS);
  const [filter, setFilter] = useState('all');
  const [olderPool, setOlderPool] = useState(OLDER_NOTIFICATIONS_POOL);
  const [loadingMore, setLoadingMore] = useState(false);
  const [listEnd, setListEnd] = useState(false);
  const [banner, setBanner] = useState(null);
  const sentinelRef = useRef(null);
  const { toasts, push: pushToast, dismiss: dismissToast } = useToastQueue();

  const unreadCount = useMemo(() => notifications.filter((n) => !n.isRead).length, [notifications]);

  const visible = useMemo(
    () => (filter === 'unread' ? notifications.filter((n) => !n.isRead) : notifications),
    [notifications, filter],
  );

  const groups = useMemo(() => {
    const map = new Map();
    visible.forEach((n) => {
      const label = groupLabel(n.createdAt);
      if (!map.has(label)) map.set(label, []);
      map.get(label).push(n);
    });
    return Array.from(map.entries());
  }, [visible]);

  // 좋아요, 댓글 카드는 클릭 자체가 읽음 처리 트리거이자 게시글 이동이다. 블라인드 카드의 확인
  // 버튼도 같은 낙관적 읽음 처리를 한다. 둘 다 서버 응답을 기다리지 않고 그 자리에서 바로 반영한다.
  const markRead = (id) => {
    setNotifications((prev) => prev.map((n) => (n.notificationId === id ? { ...n, isRead: true } : n)));
  };

  const handleActivate = (id) => {
    const target = notifications.find((n) => n.notificationId === id);
    if (!target) return;
    if (!target.isRead) markRead(id);
    showBanner(`『${target.postTitle}』 게시글로 이동합니다 (디자인 목업이라 실제 이동은 생략)`);
  };

  // 블라인드는 확인을 눌러도 사라지지 않는다. 읽음 스타일로 바뀔 뿐이다. 삭제하려면 X 버튼을 눌러야 한다.
  const handleConfirm = (id) => markRead(id);

  const handleDelete = (id) => {
    setNotifications((prev) => prev.filter((n) => n.notificationId !== id));
  };

  const bannerTimerRef = useRef(null);
  const showBanner = (text) => {
    setBanner(text);
    clearTimeout(bannerTimerRef.current);
    bannerTimerRef.current = setTimeout(() => setBanner(null), 1800);
  };

  const handleGoToNotifications = () => {
    document.querySelector('.notif-shell')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  // 무한 스크롤 데모. 실제 커서 기반 API 호출 대신 로컬 목데이터 풀에서만 이어 붙인다.
  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return undefined;

    const io = new IntersectionObserver((entries) => {
      if (!entries[0].isIntersecting || loadingMore || olderPool.length === 0) return;
      setLoadingMore(true);
      setTimeout(() => {
        setNotifications((prev) => [...prev, olderPool[0]]);
        setOlderPool((prev) => {
          const next = prev.slice(1);
          if (next.length === 0) setListEnd(true);
          return next;
        });
        setLoadingMore(false);
      }, 450);
    }, { rootMargin: '80px' });

    io.observe(sentinel);
    return () => io.disconnect();
  }, [loadingMore, olderPool]);

  return (
    <div className="notif-shell">
      <header className="notif-header">
        <div className="notif-header-top">
          <h1 className="notif-title">
            알림 <span className="count">{unreadCount}</span>
          </h1>
        </div>
        <div className="filter-chips" role="group" aria-label="알림 필터">
          <button
            type="button"
            className={`chip${filter === 'all' ? ' active' : ''}`}
            onClick={() => setFilter('all')}
          >
            전체
          </button>
          <button
            type="button"
            className={`chip${filter === 'unread' ? ' active' : ''}`}
            onClick={() => setFilter('unread')}
          >
            안 읽음
          </button>
        </div>
      </header>

      {/* 디자인 데모 전용 컨트롤. 실제 SSE 연동 시 이 블록은 제거한다 */}
      <div className="notif-demo-panel">
        <span className="notif-demo-label">테스트용 알림 발생 버튼</span>
        <span className="notif-demo-hint">좋아요는 토스트 대상이 아니라 버튼이 없다</span>
        <button type="button" onClick={() => pushToast(makeDemoNotification('COMMENT'))}>댓글 알림 하나 도착</button>
        <button type="button" onClick={() => pushToast(makeDemoNotification('BLIND'))}>블라인드 알림 하나 도착</button>
        <button
          type="button"
          onClick={() => {
            ['COMMENT', 'BLIND', 'COMMENT', 'BLIND'].forEach((t, i) => {
              setTimeout(() => pushToast(makeDemoNotification(t)), i * 150);
            });
          }}
        >
          연속 4개 도착 (초과 동작 확인)
        </button>
      </div>

      {groups.length === 0 && (
        <div className="notif-empty">
          <strong>{filter === 'unread' ? '안 읽은 알림이 없습니다.' : '받은 알림이 없습니다.'}</strong>
          새로운 좋아요, 댓글 소식이 오면 여기에 표시돼요.
        </div>
      )}

      {groups.map(([label, items]) => (
        <section className="notif-group" key={label}>
          <p className="notif-group-label">{label}</p>
          <ul className="notif-list">
            {items.map((n) => (
              <NotificationCard
                key={n.notificationId}
                notification={n}
                onActivate={handleActivate}
                onConfirm={handleConfirm}
                onDelete={handleDelete}
              />
            ))}
          </ul>
        </section>
      ))}

      <div className="scroll-sentinel" ref={sentinelRef} />
      {loadingMore && <p className="load-status">이전 알림 불러오는 중…</p>}
      {listEnd && <p className="load-status">모든 알림을 확인했습니다.</p>}

      {banner && <div className="notif-nav-banner">{banner}</div>}

      <NotificationToastStack
        toasts={toasts}
        onDismiss={dismissToast}
        onCommentClick={(toast) => showBanner(`『${toast.postTitle}』 게시글로 이동합니다 (디자인 목업이라 실제 이동은 생략)`)}
        onGoToNotifications={handleGoToNotifications}
      />
    </div>
  );
}

export default NotificationPage;
