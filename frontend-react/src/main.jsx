import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { AuthProvider } from './context/AuthContext/AuthProvider.jsx'
import { ToastProvider } from './context/ToastContext/ToastProvider.jsx'
import { NotificationProvider } from './context/NotificationContext/NotificationProvider.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AuthProvider>
      <NotificationProvider>
        <ToastProvider>
          <App />
        </ToastProvider>
      </NotificationProvider>
    </AuthProvider>
  </StrictMode>,
)
