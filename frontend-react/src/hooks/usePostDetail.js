import { useCallback, useEffect, useState } from 'react';
import request from '../api/request';

async function getDetailPost(postId) {
  return request(`/posts/${postId}`, 'GET');
}

async function deletePost(postId) {
  return request(`/posts/${postId}`, 'DELETE');
}

async function likePost(postId) {
  return request(`/posts/${postId}/like`, 'POST');
}

async function unlikePost(postId) {
  return request(`/posts/${postId}/like`, 'DELETE');
}

async function reportPost(postId) {
  return request(`/posts/${postId}/declaration`, 'POST');
}

async function createComment(postId, commentData) {
  return request(`/posts/${postId}/comment`, 'POST', commentData);
}

async function editComment(postId, commentId, commentData) {
  return request(`/posts/${postId}/comment/${commentId}`, 'PUT', commentData);
}

async function deleteComment(postId, commentId) {
  return request(`/posts/${postId}/comment/${commentId}`, 'DELETE');
}

function usePostDetail(postId) {
  const [post, setPost] = useState(null);
  const [isLiked, setIsLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [commentCount, setCommentCount] = useState(0);

  const [comments, setComments] = useState([]);
  const [editingComment, setEditingComment] = useState(null);
  const [deleteTargetCommentId, setDeleteTargetCommentId] = useState(null);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);

  const [isLiking, setIsLiking] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setError(false);

      try {
        const result = await getDetailPost(postId);
        if (cancelled) return;

        setPost(result.data.post);
        setIsLiked(Boolean(result.data.is_liked));
        setLikeCount(result.data.post.like_count);
        setCommentCount(result.data.post.comment_count);
        setComments(result.data.comments);

      } catch (err) {
        if (!cancelled) {
          console.error(err);
          setError(true);
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [postId]);

  const toggleLike = useCallback(async () => {
    if(isLiking) return;
      setIsLiking(true);
    try{
      const result = isLiked ? await unlikePost(postId) : await likePost(postId);

      setIsLiked((prev) => !prev);
      setLikeCount(result.data.like_count);
    }catch(error) {
      console.error(error);
    }finally {
      setIsLiking(false);
    }
  }, [isLiked,isLiking, postId]);

  const deletePostCallback = useCallback(() => deletePost(postId), [postId]);
  const reportPostCallback = useCallback(() => reportPost(postId), [postId]);

  const addComment = useCallback(
    async (commentContent) => {
      const result = await createComment(postId, { commentContent });
      setComments((prev) => [result.data, ...prev]);
      setCommentCount(result.data.commentCount);
    },
    [postId]
  );

  const updateComment = useCallback(
    async (commentContent) => {
      const result = await editComment(postId, editingComment.commentId, { commentContent });
      setComments((prev) =>
        prev.map((comment) =>
          comment.commentId === editingComment.commentId
            ? { ...comment, commentContent: result.data.commentContent }
            : comment
        )
      );

      setEditingComment(null);
    },
    [postId, editingComment]
  );

  const removeComment = useCallback(async () => {
    const result = await deleteComment(postId, deleteTargetCommentId);
    setComments((prev) => prev.filter((comment) => comment.commentId !== deleteTargetCommentId));
    setCommentCount(result.data.commentCount);
    setDeleteTargetCommentId(null);
  }, [postId, deleteTargetCommentId]);

  return {
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
    deletePost: deletePostCallback,
    toggleLike,
    reportPost: reportPostCallback,
    createComment: addComment,
    editComment: updateComment,
    deleteComment: removeComment,
  };
}

export default usePostDetail;
