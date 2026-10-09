import { dailyReportService } from "./dailyReportService";
import { labourService } from "./labourService";
import { materialService } from "./materialService";
import { offlineQueue, type QueuedMutation } from "./offlineQueue";
import { getErrorMessage } from "../utils/apiError";
import { isNetworkFailure } from "../features/offline/networkError";
import type { AttendanceSubmitPayload } from "../types/labour";
import type { DailyReportPayload } from "../types/dailyReport";
import type { MaterialUsagePayload } from "../types/material";

export interface MaterialUsageQueuePayload {
  materialId: number;
  usage: MaterialUsagePayload;
}

type Listener = (isSyncing: boolean) => void;
const listeners = new Set<Listener>();
let isSyncing = false;

function setSyncing(value: boolean) {
  isSyncing = value;
  listeners.forEach((listener) => listener(value));
}

async function replay(mutation: QueuedMutation): Promise<void> {
  switch (mutation.type) {
    case "DAILY_REPORT":
      await dailyReportService.create(mutation.projectId, mutation.payload as DailyReportPayload);
      return;
    case "ATTENDANCE":
      await labourService.submitAttendance(mutation.projectId, mutation.payload as AttendanceSubmitPayload);
      return;
    case "MATERIAL_USAGE": {
      const { materialId, usage } = mutation.payload as MaterialUsageQueuePayload;
      await materialService.createUsage(materialId, usage);
      return;
    }
  }
}

export const syncManager = {
  isSyncing: () => isSyncing,

  isOnline: () => navigator.onLine,

  subscribeSyncing(listener: Listener): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },

  async syncPending(): Promise<void> {
    if (isSyncing || !navigator.onLine) return;

    const pending = offlineQueue.getAll().filter((item) => item.status !== "SYNCING");
    if (pending.length === 0) return;

    setSyncing(true);
    try {
      for (const mutation of pending) {
        offlineQueue.update(mutation.id, { status: "SYNCING" });
        try {
          await replay(mutation);
          offlineQueue.remove(mutation.id);
        } catch (error) {
          if (isNetworkFailure(error)) {
            // Still offline (or connection dropped mid-sync) — leave it queued, try again later.
            offlineQueue.update(mutation.id, { status: "PENDING" });
            break;
          }
          offlineQueue.update(mutation.id, { status: "FAILED", errorMessage: getErrorMessage(error) });
        }
      }
    } finally {
      setSyncing(false);
    }
  },
};

window.addEventListener("online", () => {
  syncManager.syncPending();
});

if (navigator.onLine) {
  syncManager.syncPending();
}
