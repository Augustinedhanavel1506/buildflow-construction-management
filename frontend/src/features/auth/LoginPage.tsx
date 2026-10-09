import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import { useAuth } from "./AuthContext";
import "./AuthLayout.css";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await login({ email, password });
      const redirectTo = (location.state as { from?: Location })?.from?.pathname ?? "/";
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="bf-auth-layout">
      <div className="bf-auth-card">
        <div className="bf-auth-card__brand">
          <span className="bf-auth-card__brand-mark">BF</span>
          <span className="bf-auth-card__brand-name">BuildFlow</span>
        </div>

        <div className="bf-auth-card__header">
          <h1>Welcome back</h1>
          <p>Sign in to your account</p>
        </div>

        <form className="bf-auth-form" onSubmit={handleSubmit}>
          {error && <div className="bf-auth-form__error">{error}</div>}

          <Input
            label="Email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <Input
            label="Password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <Button type="submit" isLoading={isSubmitting}>
            Sign in
          </Button>
        </form>

        <div className="bf-auth-card__footer">
          Don't have an account? <Link to="/register">Sign up</Link>
        </div>
      </div>
    </div>
  );
}
