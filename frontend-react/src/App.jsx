import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import AppLayout from './components/layout/AppLayout/AppLayout';
import LoginPage from './pages/LoginPage/LoginPage';
import SignupPage from './pages/SignupPage/SignupPage';
import BoardPage from './pages/BoardPage/BoardPage';
import PostWritePage from './pages/PostWritePage/PostWritePage';
import PostEditPage from './pages/PostEditPage/PostEditPage';
import PostDetailPage from './pages/PostDetailPage/PostDetailPage';
import ProfileEditPage from './pages/ProfileEditPage/ProfileEditPage';
import PasswordEditPage from './pages/PasswordEditPage/PasswordEditPage';
import NotificationPage from './pages/NotificationPage/NotificationPage';

import { useAuth } from './context/AuthContext/useAuth';

function UnauthorizedRedirect() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  useEffect(() => {
    function handleUnauthorized() {
      logout();
      navigate('/login');

    }

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, [navigate]);

  return null;
}

function App() {
  return (
    <BrowserRouter>
      <UnauthorizedRedirect />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />

        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/board" replace />} />
          <Route path="/board" element={<BoardPage />} />
          <Route path="/notifications" element={<NotificationPage />} />
          <Route path="/posts/write" element={<PostWritePage />} />
          <Route path="/posts/:postId/edit" element={<PostEditPage />} />
          <Route path="/posts/:postId" element={<PostDetailPage />} />
          <Route path="/profile-edit" element={<ProfileEditPage />} />
          <Route path="/password-edit" element={<PasswordEditPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
