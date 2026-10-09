import { useEffect, useRef, useState } from "react";
import { useOfflineQueue } from "./useOfflineQueue";
import "./OfflineStatusBadge.css";

const TYPE_LABEL: Record<string, string> = {
  DAILY_REPORT: "Daily report",
  ATTENDANCE: "Attendance",
  MATERIAL_USAGE: "Material usage",
};

export function OfflineStatusBadge() {
  const { queue, isOnline, isSyncing, pendingCount, failedCount, retry, discard } = useOfflineQueue();
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  if (isOnline && queue.length === 0) {
    return null;
  }

  return (
    <div className="bf-offline-badge" ref={containerRef}>
      <button
        className={`bf-offline-badge__trigger ${!isOnline ? "bf-offline-badge__trigger--offline" : ""}`}
        onClick={() => setIsOpen((open) => !open)}
      >
        {!isOnline ? "🔴 Offline" : isSyncing ? "🔄 Syncing…" : `📥 ${pendingCount + failedCount} queued`}
      </button>

      {isOpen && (
        <div className="bf-offline-badge__dropdown">
          <div className="bf-offline-badge__header">
            <strong>{isOnline ? "Sync status" : "You're offline"}</strong>
            {isOnline && queue.length > 0 && (
              <button className="bf-offline-badge__retry" onClick={() => retry()} disabled={isSyncing}>
                Retry now
              </button>
            )}
          </div>

          {!isOnline && (
            <p className="bf-offline-badge__note">
              Daily reports, attendance, and material usage you submit now will be saved on this device and sent
              automatically once you're back online.
            </p>
          )}

          {queue.length === 0 ? (
            <div className="bf-offline-badge__empty">Nothing queued.</div>
          ) : (
            <ul className="bf-offline-badge__list">
              {queue.map((item) => (
                <li key={item.id} className={item.status === "FAILED" ? "bf-offline-badge__item--failed" : ""}>
                  <div className="bf-offline-badge__item-main">
                    <span className="bf-offline-badge__item-type">{TYPE_LABEL[item.type]}</span>
                    <span className="bf-offline-badge__item-label">{item.label}</span>
                  </div>
                  {item.status === "FAILED" ? (
                    <div className="bf-offline-badge__item-error">
                      <span>{item.errorMessage}</span>
                      <button onClick={() => discard(item.id)}>Discard</button>
                    </div>
                  ) : (
                    <span className="bf-offline-badge__item-status">
                      {item.status === "SYNCING" ? "Sending…" : "Waiting to send"}
                    </span>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
