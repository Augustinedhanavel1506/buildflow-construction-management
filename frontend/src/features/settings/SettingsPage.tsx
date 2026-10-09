import { useNavigate } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { useAuth } from "../auth/AuthContext";
import { BusinessProfileSection } from "./BusinessProfileSection";
import "./SettingsPage.css";

export function SettingsPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <div className="bf-settings-page">
      <h1>Settings</h1>

      <Card className="bf-settings-page__section">
        <h3>Profile</h3>
        <dl className="bf-settings-page__list">
          <div>
            <dt>Full name</dt>
            <dd>{user?.fullName}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>{user?.email}</dd>
          </div>
          <div>
            <dt>Role</dt>
            <dd>{user?.role.replace("_", " ")}</dd>
          </div>
          <div>
            <dt>Business</dt>
            <dd>{user?.businessName}</dd>
          </div>
        </dl>
      </Card>

      {user?.role !== "ENGINEER" && <BusinessProfileSection canEdit={user?.role === "ADMIN"} />}

      <Card className="bf-settings-page__section">
        <h3>Security</h3>
        <p>Sign out of BuildFlow on this device.</p>
        <Button variant="danger" onClick={handleLogout}>
          Logout
        </Button>
      </Card>
    </div>
  );
}
