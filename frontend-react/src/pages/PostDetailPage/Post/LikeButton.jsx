import { memo } from 'react';
import { LikeIcon } from '../../../components/icons/StatIcons';
import { formatCount } from '../../../utils/format';
import './LikeButton.css';

function LikeButton({ liked, isLiking, likeCount, onToggle }) {
  return (
    <button type="button" className={`btn-like-heart${liked ? ' liked' : ''}`} onClick={onToggle} disabled={isLiking}>
      <LikeIcon className="icon-heart" />
      <span className="like-count">{formatCount(likeCount)}</span>
    </button>
  );
}

export default memo(LikeButton);
