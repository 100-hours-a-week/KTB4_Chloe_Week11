import { NavLink } from 'react-router-dom';
import Header from '../Header/Header';
import { useNotification } from '../../../context/NotificationContext/useNotification';
import './Sidebar.css';

function Sidebar({ collapsed, onCollapse }) {
  const { unreadCount } = useNotification();
  return (
    <aside className={`sidebar${collapsed ? ' collapsed' : ''}`}>
      <div className="sidebar-top">
        <span className="sidebar-logo">FitMate</span>
        <button
          type="button"
          className="btn-sidebar-toggle"
          aria-label="사이드바 접기"
          onClick={onCollapse}
        >
          <span className="icon-chevron-left">‹</span>
        </button>
      </div>

      <nav className="sidebar-menu">
        <NavLink
          to="/board"
          className={({ isActive }) => `sidebar-menu-item${isActive ? ' active' : ''}`}
        >
          <svg className="menu-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M4 6h16M4 12h16M4 18h10" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
          전체 게시글
        </NavLink>

        <button className="sidebar-menu-item" type="button">
          <svg className="menu-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2s4 4.5 4 8.5a4 4 0 0 1-8 0C8 6.5 12 2 12 2z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
            <path d="M8 14c0 4 2 8 4 8s4-4 4-8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
          인기글
        </button>

        <button className="sidebar-menu-item" type="button">
          <svg className="menu-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="8" r="4" stroke="currentColor" strokeWidth="1.8" />
            <path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
          내 활동
        </button>

        <NavLink
          to="/notifications"
          className={({ isActive }) => `sidebar-menu-item${isActive ? ' active' : ''}`}
        >
          <svg className="menu-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M6 10a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
            <path d="M10 20a2 2 0 0 0 4 0" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
          알림
          <span className="sidebar-menu-badge">{unreadCount}</span>
        </NavLink>
      </nav>

      <Header />
    </aside>
  );
}

export default Sidebar;
