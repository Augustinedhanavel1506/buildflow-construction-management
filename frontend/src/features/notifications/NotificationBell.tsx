import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { notificationService } from "../../services/notificationService";
import { formatDate } from "../../utils/format";
import type { AppNotification } from "../../types/notification";
import { notificationIcon } from "./notificationIcon";
import "./NotificationBell.css";

const POLL_INTERVAL_MS = 60000;

export function NotificationBell() {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState<AppNotification[] | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const loadUnreadCount = useCallback(async () => {
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(count);
    } catch {
      // Silently ignore — the bell just won't update this cycle.
    }
  }, []);

  useEffect(() => {
    loadUnreadCount();
    const interval = setInterval(loadUnreadCount, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [loadUnreadCount]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  async function handleToggle() {
    const opening = !isOpen;
    setIsOpen(opening);
    if (opening) {
      const page = await notificationService.list(0, 8);
      setNotifications(page.content);
    }
  }

  async function handleNotificationClick(notification: AppNotification) {
    setIsOpen(false);
    if (!notification.read) {
      await notificationService.markAsRead(notification.id);
      setUnreadCount((count) => Math.max(0, count - 1));
    }
    if (notification.projectId) {
      navigate(`/projects/${notification.projectId}`);
    }
  }

  return (
    <div className="bf-notification-bell" ref={containerRef}>
      <button className="bf-notification-bell__trigger" onClick={handleToggle} aria-label="Notifications">
        🔔
        {unreadCount > 0 && <span className="bf-notification-bell__badge">{unreadCount > 9 ? "9+" : unreadCount}</span>}
      </button>

      {isOpen && (
        <div className="bf-notification-bell__dropdown">
          <div className="bf-notification-bell__header">
            <strong>Notifications</strong>
            <button
              className="bf-notification-bell__view-all"
              onClick={() => {
                setIsOpen(false);
                navigate("/notifications");
              }}
            >
              View all
            </button>
          </div>

          {notifications === null ? (
            <div className="bf-notification-bell__empty">Loading…</div>
          ) : notifications.length === 0 ? (
            <div className="bf-notification-bell__empty">No notifications yet.</div>
          ) : (
            <ul className="bf-notification-bell__list">
              {notifications.map((notification) => (
                <li key={notification.id}>
                  <button
                    className={`bf-notification-bell__item ${notification.read ? "" : "bf-notification-bell__item--unread"}`}
                    onClick={() => handleNotificationClick(notification)}
                  >
                    <span className="bf-notification-bell__item-icon">{notificationIcon(notification.type)}</span>
                    <span className="bf-notification-bell__item-body">
                      <span className="bf-notification-bell__item-title">{notification.title}</span>
                      <span className="bf-notification-bell__item-message">{notification.message}</span>
                      <span className="bf-notification-bell__item-date">{formatDate(notification.createdAt)}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
