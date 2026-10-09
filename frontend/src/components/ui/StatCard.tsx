import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Card } from "./Card";
import "./StatCard.css";

export function StatCard({
  label,
  value,
  note,
  to,
}: {
  label: string;
  value: ReactNode;
  note?: ReactNode;
  to?: string;
}) {
  const content = (
    <>
      <span className="bf-stat-card__label">{label}</span>
      <span className="bf-stat-card__value">{value}</span>
      {note && <span className="bf-stat-card__note">{note}</span>}
    </>
  );

  if (to) {
    return (
      <Link to={to} className="bf-card bf-stat-card bf-stat-card--clickable">
        {content}
      </Link>
    );
  }

  return <Card className="bf-stat-card">{content}</Card>;
}
