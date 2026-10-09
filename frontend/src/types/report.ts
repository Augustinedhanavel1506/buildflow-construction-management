export interface ProjectCostReport {
  projectId: number;
  projectName: string;
  contractValue: number;
  revisedContractValue: number;
  estimatedCost: number;
  actualCost: number;
  remainingEstimatedCost: number;
  estimatedFinalCost: number;
  estimatedMargin: number;
  overallProgress: number;
  expenseBreakdown: { category: string; amount: number }[];
  boqVariance: {
    totalEstimated: number;
    totalActual: number;
    totalVariance: number;
  };
}

export interface ProfitabilityReport {
  projects: {
    projectId: number;
    projectName: string;
    status: string;
    contractValue: number;
    revisedContractValue: number;
    estimatedFinalCost: number;
    estimatedMargin: number;
    marginPercent: number;
  }[];
  totalContractValue: number;
  totalEstimatedFinalCost: number;
  totalEstimatedMargin: number;
}
