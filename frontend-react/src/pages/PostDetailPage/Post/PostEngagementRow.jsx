import LikeButton from './LikeButton';
import ReportButton from './ReportButton';
import './PostEngagementRow.css';

function PostEngagementRow({ liked, isLiking, likeCount, onToggleLike, onReport }) {
  return (
    <div className="post-like-row">
      <LikeButton liked={liked} isLiking={isLiking} likeCount={likeCount} onToggle={onToggleLike} />
      <ReportButton onReport={onReport} />
    </div>
  );
}

export default PostEngagementRow;
