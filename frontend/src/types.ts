// Shapes of the data the backend sends and receives.
// They mirror the Java DTOs (CandidateSummary, CandidateResponse, CandidateRequest, PageResponse).

export type CandidateStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';

export const CANDIDATE_STATUSES: CandidateStatus[] = ['ACTIVE', 'INACTIVE', 'ARCHIVED'];

/** One row in the candidate list (GET /candidates). */
export interface CandidateSummary {
  id: number;
  candidateCode: string;
  firstName: string;
  lastName: string;
  email: string;
  currentTitle: string | null;
  city: string | null;
  yearsExperience: number | null;
  openToWork: boolean;
  candidateStatus: CandidateStatus;
  // Optional extra columns: shown as "—" until the backend's CandidateSummary includes them.
  currentCompany?: string | null;
  country?: string | null;
  noticePeriodDays?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

/** Full candidate (GET /candidates/{id}, and the response of POST/PUT/PATCH). */
export interface Candidate {
  id: number;
  candidateCode: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  currentTitle: string | null;
  currentCompany: string | null;
  city: string | null;
  country: string | null;
  yearsExperience: number | null;
  noticePeriodDays: number | null;
  openToWork: boolean;
  candidateStatus: CandidateStatus;
  skills: string[];
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy?: string | null;
  updatedBy?: string | null;
}

/** Body of POST (create) and PUT (update). version is required for PUT only. */
export interface CandidateRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  currentTitle?: string | null;
  currentCompany?: string | null;
  city?: string | null;
  country?: string | null;
  yearsExperience?: number | null;
  noticePeriodDays?: number | null;
  openToWork?: boolean;
  skills?: string[];
  version?: number;
}

/** Our page JSON from the backend. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

/** Filters for the search endpoint. Every field is optional. */
export interface SearchFilters {
  q?: string;
  skills?: string[];
  skillMatch?: 'ALL' | 'ANY';
  city?: string;
  country?: string;
  minExperience?: number;
  maxExperience?: number;
  maxNoticeDays?: number;
  status?: CandidateStatus[];
  openToWork?: boolean;
}

/** Paging + sorting for the search endpoint. */
export interface PageRequest {
  page: number; // 0-based, like the backend
  size: number;
  sort: string; // e.g. "createdAt,desc"
}

/** Overview numbers (GET /candidates/stats). */
export interface CandidateStats {
  total: number;
  countsByStatus: Record<CandidateStatus, number>;
  addedInLast7Days: number;
  openToWork: number;
}
