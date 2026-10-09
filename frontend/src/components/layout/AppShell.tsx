import { useState } from "react";
import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";
import "./AppShell.css";

export function AppShell() {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  return (
    <div className="bf-app-shell">
      <Sidebar
        isCollapsed={isCollapsed}
        isMobileOpen={isMobileOpen}
        onCloseMobile={() => setIsMobileOpen(false)}
      />
      <div className="bf-app-shell__content">
        <TopBar
          onToggleSidebar={() => setIsCollapsed((value) => !value)}
          onToggleMobileSidebar={() => setIsMobileOpen((value) => !value)}
        />
        <main className="bf-app-shell__main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
