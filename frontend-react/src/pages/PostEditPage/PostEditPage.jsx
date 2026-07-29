import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PostForm from '../../components/PostForm/PostForm';
import request from '../../api/request';

async function getPostForEdit(postId) {
  return request(`/posts/${postId}/edit`, 'GET');
}

async function editPost(postId, formData) {
  return request(`/posts/${postId}`, 'PUT', formData);
}

function PostEditPage() {
  const { postId } = useParams();
  const navigate = useNavigate();

  const [initialValues, setInitialValues] = useState(null);
  const [error, setError] = useState(false);

  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {


      try {
        const result = await getPostForEdit(postId);
        if (!cancelled) setInitialValues(result.data);
      } catch (err) {
        if (!cancelled) {
          console.error(err);
          setError(true);
        }
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [postId]);

  async function handleSubmit(values) {
    if (submitting) return;

    setSubmitting(true);

    const formData = new FormData();
    formData.append('title', values.title);
    formData.append('content', values.content);
    if (values.image) {
      formData.append('postImage', values.image);
    } else if (values.removeImage) {
      formData.append('removeImage', 'true');
    }

    try {
      await editPost(postId, formData);
      navigate(`/posts/${postId}`);
    } catch (error) {
      console.error(error);
    }finally{
      setSubmitting(false);
    }
  }

  if (error) {
    return <p>게시글 정보를 불러오지 못했습니다.</p>;
  }

  if (!initialValues) {
    return <p>게시글 정보를 불러오는 중입니다.</p>;
  }

  return <PostForm mode="edit" initialValues={initialValues} onSubmit={handleSubmit} submitting={submitting}/>;
}

export default PostEditPage;
