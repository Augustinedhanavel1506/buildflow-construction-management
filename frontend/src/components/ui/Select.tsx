import { useId, type SelectHTMLAttributes } from "react";
import "./Input.css";

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
  error?: string;
}

export function Select({ label, error, id, className, children, ...rest }: SelectProps) {
  const generatedId = useId();
  const selectId = id ?? generatedId;

  return (
    <div className="bf-field">
      <label htmlFor={selectId} className="bf-field__label">
        {label}
      </label>
      <select id={selectId} className={["bf-field__input", className].filter(Boolean).join(" ")} {...rest}>
        {children}
      </select>
      {error && <span className="bf-field__error">{error}</span>}
    </div>
  );
}
