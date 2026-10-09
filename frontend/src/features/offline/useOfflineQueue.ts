import { useEffect, useState } from "react";
import { offlineQueue, type QueuedMutation } from "../../services/offlineQueue";
import { syncManager } from "../../services/syncManager";

export function useOfflineQueue() {
  const [queue, setQueue] = useState<QueuedMutation[]>(() => offlineQueue.getAll());
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [isSyncing, setIsSyncing] = useState(syncManager.isSyncing());

  useEffect(() => {
    const unsubscribeQueue = offlineQueue.subscribe(setQueue);
    const unsubscribeSync = syncManager.subscribeSyncing(setIsSyncing);

    function handleOnline() {
      setIsOnline(true);
    }
    function handleOffline() {
      setIsOnline(false);
    }
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);

    return () => {
      unsubscribeQueue();
      unsubscribeSync();
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    };
  }, []);

  return {
    queue,
    isOnline,
    isSyncing,
    pendingCount: queue.filter((item) => item.status !== "FAILED").length,
    failedCount: queue.filter((item) => item.status === "FAILED").length,
    retry: () => syncManager.syncPending(),
    discard: (id: string) => offlineQueue.remove(id),
  };
}
