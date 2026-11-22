// One function per backend endpoint. Components call these, never fetch() directly.

import { request } from './client';
import type {
  Candidate,
  CandidateRequest,
  CandidateStats,
  CandidateStatus,
  CandidateSummary,
  PageRequest,
  PageResponse,
  SearchFilters,
} from '../types';

/** GET /candidates?q=...&skills=...&page=...  (search + filter + page + sort) */
export function searchCandidates(
  filters: SearchFilters,
  paging: PageRequest,
): Promise<PageResponse<CandidateSummary>> {
  const params = new URLSearchParams();

  // Only send filters that have a value. Lists are sent comma-separated: skills=java,aws
  const add = (key: string, value: unknown) => {
    if (value === undefined || value === null || value === '') return;
    if (Array.isArray(value)) {
      if (value.length > 0) params.set(key, value.join(','));
      return;
    }
    params.set(key, String(value));
  };

  add('q', filters.q?.trim());
  add('skills', filters.skills);
  if (filters.skills && filters.skills.length > 1) add('skillMatch', filters.skillMatch);
  add('city', filters.city?.trim());
  add('country', filters.country?.trim());
  add('minExperience', filters.minExperience);
  add('maxExperience', filters.maxExperience);
  add('maxNoticeDays', filters.maxNoticeDays);
  add('status', filters.status);
  add('openToWork', filters.openToWork);
  add('page', paging.page);
  add('size', paging.size);
  add('sort', paging.sort);

  return request('GET', `/candidates?${params.toString()}`);
}

/** GET /candidates/{id} */
export function getCandidate(id: number): Promise<Candidate> {
  return request('GET', `/candidates/${id}`);
}

/** POST /candidates */
export function createCandidate(body: CandidateRequest): Promise<Candidate> {
  return request('POST', '/candidates', body);
}

/** PUT /candidates/{id}  (body must include the version you last read) */
export function updateCandidate(id: number, body: CandidateRequest): Promise<Candidate> {
  return request('PUT', `/candidates/${id}`, body);
}

/** PATCH /candidates/{id}/status */
export function changeCandidateStatus(
  id: number,
  status: CandidateStatus,
  version: number,
): Promise<Candidate> {
  return request('PATCH', `/candidates/${id}/status`, { status, version });
}

/** DELETE /candidates/{id} */
export function deleteCandidate(id: number): Promise<void> {
  return request('DELETE', `/candidates/${id}`);
}

/** GET /skills?prefix=ja&limit=20  (for the skills dropdowns) */
export function findSkills(prefix: string, limit = 20): Promise<string[]> {
  const params = new URLSearchParams({ limit: String(limit) });
  if (prefix.trim()) params.set('prefix', prefix.trim());
  return request('GET', `/skills?${params.toString()}`);
}

/** GET /candidates/stats  (overview numbers) */
export function getCandidateStats(): Promise<CandidateStats> {
  return request('GET', '/candidates/stats');
}
