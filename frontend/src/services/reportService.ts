import { apiClient } from "./apiClient";
import type { ApiResponse } from "../types/api";
import type { ProfitabilityReport, ProjectCostReport } from "../types/report";

function triggerDownload(csv: string, filename: string) {
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export const reportService = {
  async getProjectCostSummary(projectId: number): Promise<ProjectCostReport> {
    const { data } = await apiClient.get<ApiResponse<ProjectCostReport>>(
      `/api/projects/${projectId}/reports/cost-summary`,
    );
    return data.data;
  },

  async getProfitability(): Promise<ProfitabilityReport> {
    const { data } = await apiClient.get<ApiResponse<ProfitabilityReport>>("/api/reports/profitability");
    return data.data;
  },

  async downloadBoqVarianceCsv(projectId: number): Promise<void> {
    const { data } = await apiClient.get<string>(`/api/projects/${projectId}/reports/boq-variance/export`, {
      responseType: "text",
    });
    triggerDownload(data, `boq-variance-${projectId}.csv`);
  },

  async downloadExpensesCsv(projectId: number): Promise<void> {
    const { data } = await apiClient.get<string>(`/api/projects/${projectId}/reports/expenses/export`, {
      responseType: "text",
    });
    triggerDownload(data, `expenses-${projectId}.csv`);
  },

  async downloadProfitabilityCsv(): Promise<void> {
    const { data } = await apiClient.get<string>("/api/reports/profitability/export", {
      responseType: "text",
    });
    triggerDownload(data, "profitability-report.csv");
  },

  async downloadTallySalesVouchersXml(from: string, to: string): Promise<void> {
    const { data } = await apiClient.get<string>("/api/tally/export/sales-vouchers", {
      params: { from, to },
      responseType: "text",
    });
    const blob = new Blob([data], { type: "application/xml;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "tally-sales-vouchers.xml";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  },
};
