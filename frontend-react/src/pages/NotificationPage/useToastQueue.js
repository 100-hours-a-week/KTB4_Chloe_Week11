import { useRef, useState } from 'react';

// 개별 토스트는 최대 3개까지만 쌓이고, 그 상태에서 알림이 더 도착하면 새로운 알림이
// 존재합니다 토스트를 별도의 한 자리로 더 띄운다(3개 제한에 포함되지 않는다).
// 대기열은 두지 않으므로 처리는 그 즉시 끝난다.
const MAX_ITEM_TOASTS = 3;

export function useToastQueue() {
  const [toasts, setToasts] = useState([]);
  const seqRef = useRef(0);

  const push = (notification) => {
    setToasts((prev) => {
      const itemCount = prev.filter((t) => t.kind === 'item').length;
      if (itemCount < MAX_ITEM_TOASTS) {
        seqRef.current += 1;
        return [...prev, { id: seqRef.current, kind: 'item', type: notification.type, postTitle: notification.postTitle }];
      }
      const hasOverflow = prev.some((t) => t.kind === 'overflow');
      if (hasOverflow) return prev; // 개수를 세지 않고 그대로 둔다
      seqRef.current += 1;
      return [...prev, { id: seqRef.current, kind: 'overflow' }];
    });
  };

  const dismiss = (id) => setToasts((prev) => prev.filter((t) => t.id !== id));

  return { toasts, push, dismiss };
}
