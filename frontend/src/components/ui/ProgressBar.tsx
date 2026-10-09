import "./ProgressBar.css";

export function ProgressBar({ value }: { value: number }) {
  const clamped = Math.max(0, Math.min(100, value));
  return (
    <div className="bf-progress-bar" role="progressbar" aria-valuenow={clamped} aria-valuemin={0} aria-valuemax={100}>
      <div className="bf-progress-bar__fill" style={{ width: `${clamped}%` }} />
    </div>
  );
}
