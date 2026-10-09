import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { getErrorMessage } from "../../utils/apiError";
import { useAuth } from "./AuthContext";
import "./AuthLayout.css";

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [businessName, setBusinessName] = useState("");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await register({ businessName, fullName, email, password });
      navigate("/dashboard", { replace: true });
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
          <h1>Create your account</h1>
          <p>Start tracking your construction projects</p>
        </div>

        <form className="bf-auth-form" onSubmit={handleSubmit}>
          {error && <div className="bf-auth-form__error">{error}</div>}

          <Input
            label="Business name"
            required
            value={businessName}
            onChange={(e) => setBusinessName(e.target.value)}
          />

          <Input label="Full name" required value={fullName} onChange={(e) => setFullName(e.target.value)} />

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
            autoComplete="new-password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <Button type="submit" isLoading={isSubmitting}>
            Create account
          </Button>
        </form>

        <div className="bf-auth-card__footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  );
}
