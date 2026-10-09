import type { ButtonHTMLAttributes } from "react";
import "./Button.css";

type Variant = "primary" | "secondary" | "danger" | "ghost";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  isLoading?: boolean;
}

export function Button({ variant = "primary", isLoading, disabled, children, className, ...rest }: ButtonProps) {
  const classes = ["bf-button", `bf-button--${variant}`, className].filter(Boolean).join(" ");
  return (
    <button className={classes} disabled={disabled || isLoading} {...rest}>
      {isLoading ? "Please wait…" : children}
    </button>
  );
}
