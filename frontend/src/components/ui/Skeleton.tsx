import "./Skeleton.css";

export function Skeleton({ height = 16, width = "100%" }: { height?: number | string; width?: number | string }) {
  return <div className="bf-skeleton" style={{ height, width }} />;
}
