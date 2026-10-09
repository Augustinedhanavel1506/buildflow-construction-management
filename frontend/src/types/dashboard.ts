export interface DashboardSummary {
  projectCounts: {
    totalProjects: number;
    activeProjects: number;
    delayedProjects: number;
    completedProjects: number;
  };
  financialSummary: {
    totalContractValue: number;
    totalEstimatedCost: number;
    totalActualCost: number;
    budgetUtilizationPercent: number;
  };
  billingSummary: {
    totalBilled: number;
    totalReceived: number;
    totalOutstanding: number;
    totalRetentionHeld: number;
  };
  estimatedMaterialVarianceValue: number;
  overallProgress: number;
  costBreakdown: { category: string; amount: number }[];
  recentActivity: {
    type: "EXPENSE" | "DAILY_REPORT";
    description: string;
    projectName: string;
    occurredAt: string;
  }[];
}
