import type { ReactNode } from "react";
import "./Modal.css";

interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

export function Modal({ title, onClose, children }: ModalProps) {
  return (
    <div className="bf-modal-overlay" onClick={onClose}>
      <div className="bf-modal" onClick={(e) => e.stopPropagation()}>
        <div className="bf-modal__header">
          <h3>{title}</h3>
          <button className="bf-modal__close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
        <div className="bf-modal__body">{children}</div>
      </div>
    </div>
  );
}
