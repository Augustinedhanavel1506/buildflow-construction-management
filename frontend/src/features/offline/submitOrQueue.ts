import { offlineQueue, type QueuedMutationType } from "../../services/offlineQueue";
import { isNetworkFailure } from "./networkError";

export interface SubmitOrQueueResult<T> {
  queued: boolean;
  result?: T;
}

/**
 * Runs `submit()` when online. If the device is offline, or the request fails
 * because no response came back at all, the mutation is queued locally instead
 * of surfacing an error — it will be sent automatically once connectivity returns.
 * Genuine server-rejected requests (validation, auth) still throw normally.
 */
export async function submitOrQueue<T>(
  type: QueuedMutationType,
  label: string,
  projectId: number,
  payload: unknown,
  submit: () => Promise<T>,
): Promise<SubmitOrQueueResult<T>> {
  if (!navigator.onLine) {
    offlineQueue.enqueue(type, label, projectId, payload);
    return { queued: true };
  }

  try {
    const result = await submit();
    return { queued: false, result };
  } catch (error) {
    if (isNetworkFailure(error)) {
      offlineQueue.enqueue(type, label, projectId, payload);
      return { queued: true };
    }
    throw error;
  }
}
