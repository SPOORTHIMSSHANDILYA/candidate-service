// Keeps the current search in the page address, so a view can be bookmarked or shared:
//   http://localhost:5173/?q=java&city=Pune&skills=java,aws&sort=lastName,asc&page=1
// Parameter names are the same as the backend's.

import { CANDIDATE_STATUSES, type CandidateStatus, type PageRequest, type SearchFilters } from '../types';

export interface UrlState {
  filters: SearchFilters;
  paging: PageRequest;
}

const num = (v: string | null) => (v === null || v === '' || Number.isNaN(Number(v)) ? undefined : Number(v));
const list = (v: string | null) => (v ? v.split(',').map((s) => s.trim()).filter(Boolean) : undefined);

export function readUrlState(defaults: PageRequest): UrlState {
  const p = new URLSearchParams(window.location.search);
  const statuses = list(p.get('status'))?.filter((s): s is CandidateStatus =>
    (CANDIDATE_STATUSES as string[]).includes(s),
  );
  const skillMatch = p.get('skillMatch');
  const openToWork = p.get('openToWork');

  const filters: SearchFilters = {
    q: p.get('q') ?? undefined,
    skills: list(p.get('skills')),
    skillMatch: skillMatch === 'ANY' || skillMatch === 'ALL' ? skillMatch : undefined,
    city: p.get('city') ?? undefined,
    country: p.get('country') ?? undefined,
    minExperience: num(p.get('minExperience')),
    maxExperience: num(p.get('maxExperience')),
    maxNoticeDays: num(p.get('maxNoticeDays')),
    status: statuses?.length ? statuses : undefined,
    openToWork: openToWork === 'true' ? true : openToWork === 'false' ? false : undefined,
  };

  return {
    filters: dropEmpty(filters),
    paging: {
      page: Math.max(0, (num(p.get('page')) ?? 1) - 1), // the address shows pages from 1, like people count
      size: num(p.get('size')) ?? defaults.size,
      sort: p.get('sort') ?? defaults.sort,
    },
  };
}

export function writeUrlState({ filters, paging }: UrlState, defaults: PageRequest): void {
  const p = new URLSearchParams();
  const put = (k: string, v: unknown) => {
    if (v === undefined || v === null || v === '') return;
    if (Array.isArray(v)) {
      if (v.length) p.set(k, v.join(','));
    } else {
      p.set(k, String(v));
    }
  };
  put('q', filters.q?.trim());
  put('skills', filters.skills);
  if ((filters.skills?.length ?? 0) > 1 && filters.skillMatch === 'ANY') put('skillMatch', 'ANY');
  put('city', filters.city?.trim());
  put('country', filters.country?.trim());
  put('minExperience', filters.minExperience);
  put('maxExperience', filters.maxExperience);
  put('maxNoticeDays', filters.maxNoticeDays);
  put('status', filters.status);
  put('openToWork', filters.openToWork);
  if (paging.sort !== defaults.sort) put('sort', paging.sort);
  if (paging.size !== defaults.size) put('size', paging.size);
  if (paging.page > 0) put('page', paging.page + 1);

  const query = p.toString();
  const url = window.location.pathname + (query ? `?${query}` : '');
  window.history.replaceState(null, '', url); // update the address without reloading
}

/** Removes undefined / empty values so two equal searches compare equal. */
export function dropEmpty(f: SearchFilters): SearchFilters {
  const out: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(f)) {
    if (v === undefined || v === null || v === '') continue;
    if (Array.isArray(v) && v.length === 0) continue;
    out[k] = v;
  }
  return out as SearchFilters;
}
