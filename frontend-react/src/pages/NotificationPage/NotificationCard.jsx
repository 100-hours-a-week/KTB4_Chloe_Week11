import { LikeBadgeIcon, BlindBadgeIcon, ShieldIcon, CloseIcon } from '../../components/icons/NotificationIcons';
import { CommentIcon } from '../../components/icons/StatIcons';
import { getProfileImageUrl } from '../../api/imageRequest';
import { formatRelativeTime } from '../../utils/format';
import './NotificationCard.css';

const AVATAR_PALETTE = [
  'linear-gradient(135deg,#FF6F61,#FF9B8F)',
  'linear-gradient(135deg,#4ECDC4,#7FE3DB)',
  'linear-gradient(135deg,#8C8681,#B4ACA6)',
  'linear-gradient(135deg,#E85A4F,#FFB3A6)',
];

function avatarColor(name) {
  if (!name) return undefined;
  return AVATAR_PALETTE[name.charCodeAt(0) % AVATAR_PALETTE.length];
}

// 카드 문구는 모달 요약이 아니라 알림 내용 그 자체다. 좋아요는 그룹을 처음 만든 첫 행위자를
// 기준으로 하고, 이후 새 좋아요가 들어와도 바뀌지 않는다.
function messageFor({ type, actorNickname, likeGroupCount }) {
  if (type === 'LIKE') {
    if (likeGroupCount && likeGroupCount > 1) {
      return (
        <>
          <strong>{actorNickname}</strong>님 외 {likeGroupCount - 1}명이 회원님의 게시글을 좋아합니다
        </>
      );
    }
    return (
      <>
        <strong>{actorNickname}</strong>님이 회원님의 게시글을 좋아합니다
      </>
    );
  }
  if (type === 'COMMENT') {
    return (
      <>
        <strong>{actorNickname}</strong>님이 회원님의 게시글에 댓글을 남겼습니다
      </>
    );
  }
  return (
    <>
      신고가 누적되어 회원님의 게시글이 <strong>블라인드</strong> 처리되었습니다
    </>
  );
}

function NotificationCard({ notification, onActivate, onConfirm, onDelete }) {
  const { notificationId, type, isRead, createdAt, postTitle, actorNickname, actorProfileImage } = notification;
  const isSystem = type === 'BLIND';
  const typeClass = type === 'LIKE' ? 'type-like' : type === 'COMMENT' ? 'type-comment' : 'type-blind';

  // 좋아요, 댓글 카드는 항상 클릭 가능하다(읽음 여부와 무관하게 게시글로 이동). 블라인드 카드는
  // 확인 버튼 밖을 눌러도 반응하지 않는다.
  const clickable = !isSystem;
  // 블라인드 카드는 확인 버튼을 누르면 읽음 스타일로 바뀔 뿐 사라지지 않는다. 삭제는 X 버튼으로만 한다.
  const showConfirm = isSystem && !isRead;
  const showDelete = isRead;

  const rowClassName = [
    'notif-row',
    isRead ? '' : 'unread',
    showDelete ? 'has-delete' : '',
    clickable ? '' : 'no-click',
  ].filter(Boolean).join(' ');

  return (
    <li
      className={rowClassName}
      onClick={clickable ? () => onActivate(notificationId) : undefined}
    >
      <div className="notif-avatar-wrap">
        {isSystem ? (
          <div className="notif-avatar system">
            <ShieldIcon width={18} height={18} />
          </div>
        ) : (
          <div className="notif-avatar" style={{ background: avatarColor(actorNickname) }}>
            {actorProfileImage ? (
              <img className="notif-avatar-img" src={getProfileImageUrl(actorProfileImage)} alt="" />
            ) : (
              actorNickname?.slice(-2)
            )}
          </div>
        )}
        {/* 아이콘은 currentColor를 쓰므로 .notif-type-badge의 color(흰색)를 그대로 물려받는다 */}
        <span className={`notif-type-badge ${typeClass}`}>
          {type === 'LIKE' && <LikeBadgeIcon width={10} height={10} />}
          {type === 'COMMENT' && <CommentIcon width={10} height={10} />}
          {type === 'BLIND' && <BlindBadgeIcon width={10} height={10} />}
        </span>
      </div>

      <div className="notif-body">
        <p className="notif-message">{messageFor(notification)}</p>
        <p className="notif-post-ref">『{postTitle}』</p>
        <div className="notif-meta">
          {!isRead && <span className="unread-dot" aria-hidden="true" />}
          <span>{formatRelativeTime(createdAt)}</span>
        </div>
        {showConfirm && (
          <div className="notif-card-actions">
            <button
              type="button"
              className="btn-confirm"
              onClick={(e) => { e.stopPropagation(); onConfirm(notificationId); }}
            >
              확인
            </button>
          </div>
        )}
      </div>

      {showDelete && (
        <button
          type="button"
          className="btn-delete"
          aria-label="알림 삭제"
          onClick={(e) => { e.stopPropagation(); onDelete(notificationId); }}
        >
          <CloseIcon width={13} height={13} />
        </button>
      )}
    </li>
  );
}

export default NotificationCard;
