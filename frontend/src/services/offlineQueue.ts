export type QueuedMutationType = "DAILY_REPORT" | "ATTENDANCE" | "MATERIAL_USAGE";
export type QueuedMutationStatus = "PENDING" | "SYNCING" | "FAILED";

export interface QueuedMutation {
  id: string;
  type: QueuedMutationType;
  label: string;
  projectId: number;
  payload: unknown;
  createdAt: string;
  status: QueuedMutationStatus;
  errorMessage?: string;
}

const STORAGE_KEY = "buildflow.offlineQueue";

type Listener = (queue: QueuedMutation[]) => void;
const listeners = new Set<Listener>();

function readQueue(): QueuedMutation[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as QueuedMutation[]) : [];
  } catch {
    return [];
  }
}

function writeQueue(queue: QueuedMutation[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
  } catch {
    // If storage is full or unavailable, the mutation stays in memory for this
    // session only — best effort, since there is no more durable fallback here.
  }
  listeners.forEach((listener) => listener(queue));
}

export const offlineQueue = {
  getAll(): QueuedMutation[] {
    return readQueue();
  },

  enqueue(type: QueuedMutationType, label: string, projectId: number, payload: unknown): QueuedMutation {
    const mutation: QueuedMutation = {
      id: crypto.randomUUID(),
      type,
      label,
      projectId,
      payload,
      createdAt: new Date().toISOString(),
      status: "PENDING",
    };
    writeQueue([...readQueue(), mutation]);
    return mutation;
  },

  remove(id: string) {
    writeQueue(readQueue().filter((item) => item.id !== id));
  },

  update(id: string, updates: Partial<QueuedMutation>) {
    writeQueue(readQueue().map((item) => (item.id === id ? { ...item, ...updates } : item)));
  },

  subscribe(listener: Listener): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};
