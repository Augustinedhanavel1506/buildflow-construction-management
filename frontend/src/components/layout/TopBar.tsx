import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import { NotificationBell } from "../../features/notifications/NotificationBell";
import { OfflineStatusBadge } from "../../features/offline/OfflineStatusBadge";
import "./TopBar.css";

interface TopBarProps {
  onToggleSidebar: () => void;
  onToggleMobileSidebar: () => void;
}

export function TopBar({ onToggleSidebar, onToggleMobileSidebar }: TopBarProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  function handleLogout() {
    logout();
    navigate("/login");
  }

  const initials = user?.fullName
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <header className="bf-topbar">
      <div className="bf-topbar__left">
        <button className="bf-topbar__icon-button bf-topbar__hamburger" onClick={onToggleMobileSidebar} aria-label="Toggle navigation">
          ☰
        </button>
        <button className="bf-topbar__icon-button bf-topbar__collapse" onClick={onToggleSidebar} aria-label="Collapse sidebar">
          «
        </button>
        <span className="bf-topbar__business">{user?.businessName}</span>
      </div>

      <div className="bf-topbar__right">
        <OfflineStatusBadge />
        <NotificationBell />
        <div className="bf-topbar__menu">
          <button className="bf-topbar__user" onClick={() => setIsMenuOpen((open) => !open)}>
            <span className="bf-topbar__avatar">{initials}</span>
            <span className="bf-topbar__user-info">
              <span className="bf-topbar__user-name">{user?.fullName}</span>
              <span className="bf-topbar__user-role">{user?.role.replace("_", " ")}</span>
            </span>
          </button>

          {isMenuOpen && (
            <div className="bf-topbar__dropdown" onMouseLeave={() => setIsMenuOpen(false)}>
              <button onClick={handleLogout}>Logout</button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
