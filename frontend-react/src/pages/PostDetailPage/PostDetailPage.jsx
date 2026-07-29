import { useCallback, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import usePostDetail from '../../hooks/usePostDetail';
import PostDetailMain from './PostDetailMain';

function PostDetailPage() {
  const { postId } = useParams();
  const navigate = useNavigate();
  const {
    post,
    isLiked,
    isLiking,
    likeCount,
    commentCount,
    comments,
    editingComment,
    setEditingComment,
    deleteTargetCommentId,
    setDeleteTargetCommentId,
    isLoading,
    error,
    deletePost,
    toggleLike,
    reportPost,
    createComment,
    editComment,
    deleteComment,
  } = usePostDetail(postId);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);

  const onEdit = () => navigate(`/posts/${postId}/edit`);
  const onRequestDelete = () => setDeleteModalOpen(true);
  const onCancelDelete = () => setDeleteModalOpen(false);

  const handleConfirmDelete = useCallback(async () => {
    try {
      await deletePost();
      navigate('/board');
    } catch (err) {
      console.error(err);
    } finally {
      setDeleteModalOpen(false);
    }
  }, [deletePost, navigate]);

  const handleReport = useCallback(async () => {
    try {
      await reportPost();
      alert('게시글 신고가 완료되었습니다.');
      navigate('/board');
    } catch (err) {
      console.error(err);
    }
  }, [reportPost, navigate]);

  async function handleSubmitComment(content) {
    if (editingComment) {
      await editComment(content);
    } else {
      await createComment(content);
    }
  }

  const handleEditComment = useCallback((comment) => setEditingComment(comment), [setEditingComment]);

  const handleRequestDeleteComment = useCallback(
    (commentId) => setDeleteTargetCommentId(commentId),
    [setDeleteTargetCommentId]
  );

  const isDeletingEditingComment =
    editingComment !== null && editingComment.commentId === deleteTargetCommentId;
  const commentDeleteModalDescription = isDeletingEditingComment
    ? '현재 수정 중인 댓글입니다.'
    : '삭제한 내용은 복구할 수 없습니다.';

  const handleConfirmDeleteComment = useCallback(async () => {
    try {
      await deleteComment();
      if (isDeletingEditingComment) {
        setEditingComment(null);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setDeleteTargetCommentId(null);
    }
  }, [deleteComment, isDeletingEditingComment, setEditingComment, setDeleteTargetCommentId]);

  const onCancelDeleteComment = () => setDeleteTargetCommentId(null);

  const onCancelEditComment = () => setEditingComment(null);

  if (error) {
    return <p>게시글 정보를 불러오지 못했습니다.</p>;
  }

  if (isLoading || !post) {
    return <p>게시글을 불러오는 중입니다.</p>;
  }

  return (
    <PostDetailMain
      post={post}
      isLiked={isLiked}
      isLiking={isLiking}
      likeCount={likeCount}
      onEdit={onEdit}
      onRequestDelete={onRequestDelete}
      onToggleLike={toggleLike}
      onReport={handleReport}
      deleteModalOpen={deleteModalOpen}
      onConfirmDelete={handleConfirmDelete}
      onCancelDelete={onCancelDelete}
      comments={comments}
      commentCount={commentCount}
      editingComment={editingComment}
      onSubmitComment={handleSubmitComment}
      onCancelEditComment={onCancelEditComment}
      onEditComment={handleEditComment}
      onRequestDeleteComment={handleRequestDeleteComment}
      commentDeleteModalOpen={deleteTargetCommentId !== null}
      commentDeleteModalDescription={commentDeleteModalDescription}
      onConfirmDeleteComment={handleConfirmDeleteComment}
      onCancelDeleteComment={onCancelDeleteComment}
    />
  );
}

export default PostDetailPage;
