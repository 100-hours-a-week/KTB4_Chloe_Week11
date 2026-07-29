import { useNavigate} from 'react-router-dom';
import PostForm from '../../components/PostForm/PostForm';
import request from '../../api/request';
import { useState } from 'react';

async function writePost(formData) {
  return request('/posts', 'POST', formData);
}

function PostWritePage() {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(values) {
    if (submitting) return;

    setSubmitting(true);
    const formData = new FormData();
    formData.append('title', values.title);
    formData.append('content', values.content);
    if (values.image) {
      formData.append('postImage', values.image);
    }

    try {
      const response = await writePost(formData);
      navigate(`/posts/${response.data.post_id}`);
    } catch (error) {
      console.error(error);
    }finally{
      setSubmitting(false);
    }
  }

  return <PostForm mode="create" onSubmit={handleSubmit} submitting={submitting} />;
}

export default PostWritePage;
