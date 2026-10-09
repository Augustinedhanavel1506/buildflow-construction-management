import { NavLink } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import "./Sidebar.css";

interface NavItem {
  label: string;
  to: string;
  icon: string;
  // Engineers only see items marked engineer; adminOnly items are hidden from other staff.
  engineer?: boolean;
  adminOnly?: boolean;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

const NAV_SECTIONS: NavSection[] = [
  {
    title: "Main",
    items: [
      { label: "Dashboard", to: "/dashboard", icon: "📊" },
      { label: "Projects", to: "/projects", icon: "🏗️" },
    ],
  },
  {
    title: "Engineering",
    items: [
      { label: "Reviews", to: "/reviews", icon: "📝", engineer: true },
      { label: "Calculators", to: "/calculators", icon: "📐", engineer: true },
    ],
  },
  {
    title: "Planning",
    items: [
      { label: "Home Estimator", to: "/estimation", icon: "🏠" },
      { label: "Estimation Rules", to: "/estimation/rules", icon: "🧮" },
    ],
  },
  {
    title: "Procurement",
    items: [
      { label: "Dealers", to: "/dealers", icon: "🏪" },
      { label: "Quote Requests", to: "/quotes", icon: "💬" },
    ],
  },
  {
    title: "Analytics",
    items: [{ label: "Reports", to: "/reports", icon: "📈" }],
  },
  {
    title: "Reference Data",
    items: [
      { label: "Rate Master", to: "/rate-master", icon: "📋" },
      { label: "Subcontractors", to: "/subcontractors", icon: "👷" },
    ],
  },
  {
    title: "System",
    items: [
      { label: "Team", to: "/team", icon: "👥", adminOnly: true },
      { label: "Notifications", to: "/notifications", icon: "🔔", engineer: true },
      { label: "Settings", to: "/settings", icon: "⚙️", engineer: true },
    ],
  },
];

interface SidebarProps {
  isCollapsed: boolean;
  isMobileOpen: boolean;
  onCloseMobile: () => void;
}

export function Sidebar({ isCollapsed, isMobileOpen, onCloseMobile }: SidebarProps) {
  const { user } = useAuth();
  const isEngineer = user?.role === "ENGINEER";
  const sections = NAV_SECTIONS.map((section) => ({
    ...section,
    items: section.items.filter((item) => {
      if (isEngineer) return item.engineer;
      return !item.adminOnly || user?.role === "ADMIN";
    }),
  })).filter((section) => section.items.length > 0);

  return (
    <>
      {isMobileOpen && <div className="bf-sidebar-scrim" onClick={onCloseMobile} />}
      <aside className={`bf-sidebar ${isCollapsed ? "bf-sidebar--collapsed" : ""} ${isMobileOpen ? "bf-sidebar--mobile-open" : ""}`}>
        <div className="bf-sidebar__brand">
          <span className="bf-sidebar__brand-mark">BF</span>
          {!isCollapsed && <span className="bf-sidebar__brand-name">BuildFlow</span>}
        </div>

        <nav className="bf-sidebar__nav">
          {sections.map((section) => (
            <div key={section.title} className="bf-sidebar__section">
              {!isCollapsed && <div className="bf-sidebar__section-title">{section.title}</div>}
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === "/estimation"}
                  onClick={onCloseMobile}
                  className={({ isActive }) =>
                    `bf-sidebar__link ${isActive ? "bf-sidebar__link--active" : ""}`
                  }
                  title={isCollapsed ? item.label : undefined}
                >
                  <span className="bf-sidebar__link-icon">{item.icon}</span>
                  {!isCollapsed && <span>{item.label}</span>}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
      </aside>
    </>
  );
}
