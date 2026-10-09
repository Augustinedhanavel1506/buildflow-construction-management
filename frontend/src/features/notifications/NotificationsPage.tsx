import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { notificationService } from "../../services/notificationService";
import { getErrorMessage } from "../../utils/apiError";
import { formatDate } from "../../utils/format";
import type { AppNotification } from "../../types/notification";
import type { Page } from "../../types/project";
import { notificationIcon } from "./notificationIcon";
import "./NotificationsPage.css";

export function NotificationsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState<Page<AppNotification> | null>(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [isMarkingAll, setIsMarkingAll] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await notificationService.list(pageNumber, 20);
      setPage(data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [pageNumber]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleMarkAllRead() {
    setIsMarkingAll(true);
    try {
      await notificationService.markAllAsRead();
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsMarkingAll(false);
    }
  }

  async function handleNotificationClick(notification: AppNotification) {
    if (!notification.read) {
      await notificationService.markAsRead(notification.id);
      await load();
    }
    if (notification.projectId) {
      navigate(`/projects/${notification.projectId}`);
    }
  }

  const hasUnread = page?.content.some((n) => !n.read) ?? false;

  return (
    <div className="bf-notifications-page">
      <div className="bf-notifications-page__header">
        <h1>Notifications</h1>
        {hasUnread && (
          <Button variant="secondary" onClick={handleMarkAllRead} isLoading={isMarkingAll}>
            Mark all as read
          </Button>
        )}
      </div>

      {error && <div className="bf-notifications-page__error">{error}</div>}

      <Card>
        {page === null ? (
          <Skeleton height={280} />
        ) : page.content.length === 0 ? (
          <EmptyState
            title="No notifications yet."
            description="You'll see updates here for material requests, daily reports, budget alerts, and more."
          />
        ) : (
          <ul className="bf-notifications-page__list">
            {page.content.map((notification) => (
              <li key={notification.id}>
                <button
                  className={`bf-notifications-page__item ${notification.read ? "" : "bf-notifications-page__item--unread"}`}
                  onClick={() => handleNotificationClick(notification)}
                >
                  <span className="bf-notifications-page__icon">{notificationIcon(notification.type)}</span>
                  <span className="bf-notifications-page__body">
                    <span className="bf-notifications-page__title">{notification.title}</span>
                    <span className="bf-notifications-page__message">{notification.message}</span>
                    <span className="bf-notifications-page__date">{formatDate(notification.createdAt)}</span>
                  </span>
                  {!notification.read && <span className="bf-notifications-page__dot" />}
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {page && page.totalPages > 1 && (
        <div className="bf-notifications-page__pagination">
          <Button
            variant="secondary"
            onClick={() => setPageNumber((p) => Math.max(0, p - 1))}
            disabled={pageNumber === 0}
          >
            Previous
          </Button>
          <span>
            Page {pageNumber + 1} of {page.totalPages}
          </span>
          <Button
            variant="secondary"
            onClick={() => setPageNumber((p) => Math.min(page.totalPages - 1, p + 1))}
            disabled={pageNumber >= page.totalPages - 1}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  );
}
