import { Navigate, Route, BrowserRouter, Routes } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import { ProtectedRoute } from "./components/layout/ProtectedRoute";
import { ToastProvider } from "./components/ui/ToastProvider";
import { AuthProvider, useAuth } from "./features/auth/AuthContext";
import { LoginPage } from "./features/auth/LoginPage";
import { RegisterPage } from "./features/auth/RegisterPage";
import { CalculatorsPage } from "./features/calculators/CalculatorsPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { ReviewDetailPage } from "./features/reviews/ReviewDetailPage";
import { ReviewsPage } from "./features/reviews/ReviewsPage";
import { TeamPage } from "./features/team/TeamPage";
import { DealersPage } from "./features/dealers/DealersPage";
import { QuoteRequestDetailPage } from "./features/dealers/QuoteRequestDetailPage";
import { QuoteRequestsPage } from "./features/dealers/QuoteRequestsPage";
import { EstimationDetailPage } from "./features/estimation/EstimationDetailPage";
import { EstimationListPage } from "./features/estimation/EstimationListPage";
import { EstimationRulesPage } from "./features/estimation/EstimationRulesPage";
import { PlanEditorPage } from "./features/estimation/plan/PlanEditorPage";
import { PlanSheetPage } from "./features/estimation/plan/PlanSheetPage";
import { HomeView3DPage } from "./features/estimation/view3d/HomeView3DPage";
import { ReportPage } from "./features/estimation/ReportPage";
import { WhatIfPage } from "./features/estimation/WhatIfPage";
import { NotificationsPage } from "./features/notifications/NotificationsPage";
import { ProjectDetailPage } from "./features/projects/ProjectDetailPage";
import { ProjectsListPage } from "./features/projects/ProjectsListPage";
import { RateMasterPage } from "./features/rateMaster/RateMasterPage";
import { ReportsPage } from "./features/reports/ReportsPage";
import { SettingsPage } from "./features/settings/SettingsPage";
import { SubcontractorsPage } from "./features/subcontractors/SubcontractorsPage";
import "./services/syncManager";

function RootRedirect() {
  const { isAuthenticated, user } = useAuth();
  const home = user?.role === "ENGINEER" ? "/reviews" : "/dashboard";
  return <Navigate to={isAuthenticated ? home : "/login"} replace />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/projects" element={<ProjectsListPage />} />
        <Route path="/projects/:id" element={<ProjectDetailPage />} />
        <Route path="/estimation" element={<EstimationListPage />} />
        <Route path="/estimation/rules" element={<EstimationRulesPage />} />
        <Route path="/estimation/:id" element={<EstimationDetailPage />} />
        <Route path="/estimation/:id/plan" element={<PlanEditorPage />} />
        <Route path="/estimation/:id/plan-sheet" element={<PlanSheetPage />} />
        <Route path="/estimation/:id/3d" element={<HomeView3DPage />} />
        <Route path="/estimation/:id/what-if" element={<WhatIfPage />} />
        <Route path="/estimation/:id/report" element={<ReportPage />} />
        <Route path="/dealers" element={<DealersPage />} />
        <Route path="/quotes" element={<QuoteRequestsPage />} />
        <Route path="/quotes/:id" element={<QuoteRequestDetailPage />} />
        <Route path="/reviews" element={<ReviewsPage />} />
        <Route path="/reviews/:id" element={<ReviewDetailPage />} />
        <Route path="/calculators" element={<CalculatorsPage />} />
        <Route path="/team" element={<TeamPage />} />
        <Route path="/reports" element={<ReportsPage />} />
        <Route path="/rate-master" element={<RateMasterPage />} />
        <Route path="/subcontractors" element={<SubcontractorsPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>

      <Route path="/" element={<RootRedirect />} />
      <Route path="*" element={<RootRedirect />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <AppRoutes />
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
