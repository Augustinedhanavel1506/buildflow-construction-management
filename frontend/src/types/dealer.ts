export interface DealerRate {
  id: number;
  itemName: string;
  unit: string;
  rate: number;
  updatedAt: string;
}

export interface Dealer {
  id: number;
  name: string;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  district: string | null;
  deliveryRadiusKm: number | null;
  gstin: string | null;
  notes: string | null;
  active: boolean;
  rates: DealerRate[];
}

export interface DealerRatePayload {
  itemName: string;
  unit: string;
  rate: number;
}

export interface DealerPayload {
  name: string;
  contactPerson?: string;
  phone?: string;
  email?: string;
  address?: string;
  district?: string;
  deliveryRadiusKm?: number;
  gstin?: string;
  notes?: string;
  active?: boolean;
  rates?: DealerRatePayload[];
}

export type QuoteRequestStatus = "OPEN" | "AWARDED" | "CLOSED";
export type DealerQuoteStatus = "PENDING" | "RECEIVED" | "DECLINED";

export interface QuoteRequestSummary {
  id: number;
  projectId: number;
  projectName: string;
  title: string;
  status: QuoteRequestStatus;
  dealerCount: number;
  receivedCount: number;
  itemCount: number;
  createdAt: string;
}

export interface QuoteItem {
  itemName: string;
  unit: string;
  quantity: number;
}

export interface QuoteLine {
  itemName: string;
  unit: string;
  quantity: number;
  unitRate: number | null;
  amount: number | null;
  source: "INDICATIVE" | "QUOTED";
}

export interface DealerQuote {
  id: number;
  dealerId: number;
  dealerName: string;
  dealerPhone: string | null;
  status: DealerQuoteStatus;
  deliveryCharge: number;
  loadingCharge: number;
  notes: string | null;
  receivedAt: string | null;
  lines: QuoteLine[];
  pricedItemCount: number;
  itemCount: number;
  complete: boolean;
  materialTotal: number;
  effectiveTotal: number;
}

export interface QuoteRequest {
  id: number;
  projectId: number;
  projectName: string;
  title: string;
  status: QuoteRequestStatus;
  awardedQuoteId: number | null;
  lowestCompleteQuoteId: number | null;
  createdAt: string;
  items: QuoteItem[];
  quotes: DealerQuote[];
}

export interface QuoteUpdatePayload {
  status: DealerQuoteStatus;
  deliveryCharge?: number;
  loadingCharge?: number;
  notes?: string;
  lines: { itemName: string; unitRate?: number }[];
}
