import { useId, type InputHTMLAttributes } from "react";
import "./Input.css";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export function Input({ label, error, id, className, ...rest }: InputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;

  return (
    <div className="bf-field">
      <label htmlFor={inputId} className="bf-field__label">
        {label}
      </label>
      <input id={inputId} className={["bf-field__input", className].filter(Boolean).join(" ")} {...rest} />
      {error && <span className="bf-field__error">{error}</span>}
    </div>
  );
}
