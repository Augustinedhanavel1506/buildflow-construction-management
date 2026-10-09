export interface Business {
  id: number;
  name: string;
  phone: string | null;
  address: string | null;
  gstin: string | null;
  stateName: string | null;
  materialRegion: string | null;
  defaultGstRate: number;
}

export interface BusinessPayload {
  name: string;
  phone?: string;
  address?: string;
  gstin?: string;
  stateName?: string;
  materialRegion?: string;
  defaultGstRate?: number;
}
