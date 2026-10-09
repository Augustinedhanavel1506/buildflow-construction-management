import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import "./Toast.css";

type ToastTone = "success" | "error" | "warning";

interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
}

interface ToastContextValue {
  showToast: (tone: ToastTone, message: string) => void;
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

// Several quick actions used to pile up a tall column of messages over the page. Keep the newest
// few, drop repeats, and let a click dismiss one.
const MAX_VISIBLE = 3;
const DISMISS_MS = 4000;

let nextId = 1;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const showToast = useCallback(
    (tone: ToastTone, message: string) => {
      const id = nextId++;
      setToasts((current) => {
        const withoutRepeat = current.filter((toast) => !(toast.tone === tone && toast.message === message));
        return [...withoutRepeat, { id, tone, message }].slice(-MAX_VISIBLE);
      });
      setTimeout(() => dismiss(id), DISMISS_MS);
    },
    [dismiss],
  );

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="bf-toast-stack" aria-live="polite">
        {toasts.map((toast) => (
          <button
            key={toast.id}
            type="button"
            className={`bf-toast bf-toast--${toast.tone}`}
            onClick={() => dismiss(toast.id)}
            title="Click to dismiss"
          >
            {toast.message}
          </button>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}
