import PostTopBar from './Post/PostTopBar';
import PostHeader from './Post/PostHeader';
import PostEngagementRow from './Post/PostEngagementRow';
import CommentSection from './Comment/CommentSection';
import ConfirmModal from '../../components/ConfirmModal/ConfirmModal';
import './PostDetailMain.css';

function PostDetailMain({
  post,
  isLiked,
  isLiking,
  likeCount,
  onEdit,
  onRequestDelete,
  onToggleLike,
  onReport,
  deleteModalOpen,
  onConfirmDelete,
  onCancelDelete,
  comments,
  commentCount,
  editingComment,
  onSubmitComment,
  onCancelEditComment,
  onEditComment,
  onRequestDeleteComment,
  commentDeleteModalOpen,
  commentDeleteModalDescription,
  onConfirmDeleteComment,
  onCancelDeleteComment,
}) {
  return (
    <article className="post-detail">
      <PostTopBar
        viewCount={post.view_count}
        isOwner={post.isOwner}
        onEdit={onEdit}
        onRequestDelete={onRequestDelete}
      />

      <PostHeader post={post} />

      <PostEngagementRow
        liked={isLiked}
        isLiking={isLiking}
        likeCount={likeCount}
        onToggleLike={onToggleLike}
        onReport={onReport}
      />

      <CommentSection
        comments={comments}
        commentCount={commentCount}
        editingComment={editingComment}
        onSubmitComment={onSubmitComment}
        onCancelEdit={onCancelEditComment}
        onEditComment={onEditComment}
        onRequestDeleteComment={onRequestDeleteComment}
      />

      <ConfirmModal
        open={deleteModalOpen}
        title="게시글을 삭제하시겠습니까?"
        description="삭제한 내용은 복구할 수 없습니다."
        onConfirm={onConfirmDelete}
        onCancel={onCancelDelete}
      />

      <ConfirmModal
        open={commentDeleteModalOpen}
        title="댓글을 삭제하시겠습니까?"
        description={commentDeleteModalDescription}
        onConfirm={onConfirmDeleteComment}
        onCancel={onCancelDeleteComment}
      />
    </article>
  );
}

export default PostDetailMain;
