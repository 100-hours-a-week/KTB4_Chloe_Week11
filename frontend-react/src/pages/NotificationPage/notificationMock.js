// 디자인 전용 목데이터다. 실제 API 연동(getNotifications 등)은 이번 작업 범위가 아니다.
// 필드 구성은 NotificationListItemDto(백엔드)와 동일하게 맞춰서, 나중에 실제 데이터로
// 교체할 때 이 파일만 지우면 되도록 했다.
import { truncateTitle } from '../../utils/format';

const minutesAgo = (min) => new Date(Date.now() - min * 60000).toISOString();

let seq = 100;
function makeNotification({ type, isRead, minutesAgo: min, postTitle, actorNickname, likeGroupCount }) {
  seq += 1;
  return {
    notificationId: seq,
    type,
    isRead,
    createdAt: minutesAgo(min),
    postId: type === 'BLIND' ? null : seq * 10,
    postTitle,
    actorNickname: actorNickname ?? null,
    actorProfileImage: null,
    likeGroupCount: likeGroupCount ?? null,
  };
}

export const INITIAL_NOTIFICATIONS = [
  makeNotification({ type: 'LIKE', isRead: false, minutesAgo: 3, postTitle: '오늘 하체 운동 인증합니다 💪', actorNickname: '김민지' }),
  makeNotification({ type: 'LIKE', isRead: false, minutesAgo: 40, postTitle: '다이어트 3개월차 기록 공유합니다', actorNickname: '박준서', likeGroupCount: 4 }),
  makeNotification({ type: 'COMMENT', isRead: false, minutesAgo: 95, postTitle: '헬스장에서 만난 인생 조언', actorNickname: '이서연' }),
  makeNotification({ type: 'BLIND', isRead: false, minutesAgo: 190, postTitle: '무릎 부상 회복 루틴 공유해요' }),
  makeNotification({ type: 'COMMENT', isRead: true, minutesAgo: 1500, postTitle: '운동 후 단백질 보충 꿀팁', actorNickname: '최도윤' }),
  makeNotification({ type: 'LIKE', isRead: false, minutesAgo: 1600, postTitle: '3대 500 도전기 (스쿼트 편)', actorNickname: '한지우' }),
  makeNotification({ type: 'BLIND', isRead: true, minutesAgo: 4000, postTitle: '광고성 게시글 신고합니다' }),
  makeNotification({ type: 'LIKE', isRead: true, minutesAgo: 7200, postTitle: '아침 러닝 5km 완주 인증', actorNickname: '정하늘' }),
];

export const OLDER_NOTIFICATIONS_POOL = [
  makeNotification({ type: 'COMMENT', isRead: true, minutesAgo: 8600, postTitle: '요즘 핫한 헬스장 추천해요', actorNickname: '오유진' }),
  makeNotification({ type: 'LIKE', isRead: true, minutesAgo: 10000, postTitle: '홈트 3주차 변화 기록', actorNickname: '서지훈' }),
];

// 실시간 토스트 데모 트리거용 데이터. 실제로는 SSE 페이로드로 들어온다.
export const TOAST_DEMO_ACTORS = ['윤소민', '강태오', '배지훈'];
export const TOAST_DEMO_POSTS = ['오늘 상체 운동 루틴 정리해봤어요', '단백질 쉐이크 직접 만들어 먹기', '헬스 3개월 전후 비교 사진'];

export function makeDemoNotification(type) {
  const postTitle = TOAST_DEMO_POSTS[Math.floor(Math.random() * TOAST_DEMO_POSTS.length)];
  const actorNickname = type === 'COMMENT' ? TOAST_DEMO_ACTORS[Math.floor(Math.random() * TOAST_DEMO_ACTORS.length)] : null;
  return makeNotification({ type, isRead: false, minutesAgo: 0, postTitle, actorNickname });
}

// 실제로는 서버가 알림 생성 시점에 이 문구를 완성해서 SSE payload(message 필드)로 그대로 내려준다.
// 프론트는 받은 문자열을 그대로 그리기만 하면 되고, 여기서는 데모를 위해 같은 규칙을 흉내만 낸다.
export function buildToastMessage(type, postTitle) {
  const truncated = truncateTitle(postTitle, 10);
  const suffix = type === 'COMMENT' ? '에 새 댓글이 달렸습니다.' : '이 신고 누적으로 블라인드 처리되었습니다.';
  return `${truncated}${suffix}`;
}
