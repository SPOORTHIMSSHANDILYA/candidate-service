// Every filter the screen offers is described here, in ONE list.
// To add a filter: add one entry below (and the matching query parameter in the backend).
// The filter bar, the "More filters" menu and the chip labels are all built from this list.

import { CANDIDATE_STATUSES, type CandidateStatus, type SearchFilters } from '../types';
import { statusLabel } from '../utils/status';

/** How a filter is edited in its popover. */
export type FilterEditor =
  | { kind: 'statuses' } //                     checkboxes of statuses
  | { kind: 'skills' } //                       skill picker + All/Any
  | { kind: 'text'; field: 'city' | 'country'; placeholder: string }
  | { kind: 'range'; min: 'minExperience'; max: 'maxExperience'; unit: string; limit: number }
  | { kind: 'atMost'; field: 'maxNoticeDays'; unit: string; limit: number }
  | { kind: 'yesNo'; field: 'openToWork'; yes: string; no: string };

export interface FilterDefinition {
  key: string;
  label: string;
  /** Pinned filters always show in the bar; the rest live under "+ More filters". */
  pinned: boolean;
  editor: FilterEditor;
}

export const FILTERS: FilterDefinition[] = [
  { key: 'status', label: 'Status', pinned: true, editor: { kind: 'statuses' } },
  { key: 'skills', label: 'Skills', pinned: true, editor: { kind: 'skills' } },
  { key: 'city', label: 'City', pinned: true, editor: { kind: 'text', field: 'city', placeholder: 'e.g. Bengaluru' } },
  {
    key: 'experience',
    label: 'Experience',
    pinned: true,
    editor: { kind: 'range', min: 'minExperience', max: 'maxExperience', unit: 'yrs', limit: 60 },
  },
  { key: 'country', label: 'Country', pinned: false, editor: { kind: 'text', field: 'country', placeholder: 'e.g. India' } },
  {
    key: 'notice',
    label: 'Notice period',
    pinned: false,
    editor: { kind: 'atMost', field: 'maxNoticeDays', unit: 'days', limit: 365 },
  },
  {
    key: 'availability',
    label: 'Availability',
    pinned: false,
    editor: { kind: 'yesNo', field: 'openToWork', yes: 'Open to work', no: 'Not looking' },
  },
];

export function findFilter(key: string): FilterDefinition | undefined {
  return FILTERS.find((f) => f.key === key);
}

/** Is this filter currently narrowing the results? */
export function isActive(def: FilterDefinition, f: SearchFilters): boolean {
  const e = def.editor;
  switch (e.kind) {
    case 'statuses':
      return (f.status?.length ?? 0) > 0;
    case 'skills':
      return (f.skills?.length ?? 0) > 0;
    case 'text':
      return !!f[e.field]?.trim();
    case 'range':
      return f[e.min] !== undefined || f[e.max] !== undefined;
    case 'atMost':
      return f[e.field] !== undefined;
    case 'yesNo':
      return f[e.field] !== undefined;
  }
}

/** Short text shown on an active chip, e.g. "Pune" or "3–7 yrs". */
export function summary(def: FilterDefinition, f: SearchFilters): string {
  const e = def.editor;
  switch (e.kind) {
    case 'statuses':
      return listSummary((f.status ?? []).map((s: CandidateStatus) => statusLabel(s)));
    case 'skills': {
      const skills = f.skills ?? [];
      const joiner = f.skillMatch === 'ANY' && skills.length > 1 ? ' or ' : ' + ';
      return skills.length <= 2 ? skills.join(joiner) : `${skills[0]}${joiner}${skills.length - 1} more`;
    }
    case 'text':
      return f[e.field] ?? '';
    case 'range': {
      const min = f[e.min];
      const max = f[e.max];
      if (min !== undefined && max !== undefined) return `${min}–${max} ${e.unit}`;
      if (min !== undefined) return `≥ ${min} ${e.unit}`;
      return `≤ ${max} ${e.unit}`;
    }
    case 'atMost':
      return `≤ ${f[e.field]} ${e.unit}`;
    case 'yesNo':
      return f[e.field] ? e.yes : e.no;
  }
}

/** The filters with this one removed. */
export function clearFilter(def: FilterDefinition, f: SearchFilters): SearchFilters {
  const next = { ...f };
  const e = def.editor;
  switch (e.kind) {
    case 'statuses':
      delete next.status;
      break;
    case 'skills':
      delete next.skills;
      delete next.skillMatch;
      break;
    case 'text':
      delete next[e.field];
      break;
    case 'range':
      delete next[e.min];
      delete next[e.max];
      break;
    case 'atMost':
    case 'yesNo':
      delete next[e.field];
      break;
  }
  return next;
}

function listSummary(items: string[]): string {
  return items.length <= 2 ? items.join(', ') : `${items[0]} +${items.length - 1}`;
}

export const ALL_STATUSES = CANDIDATE_STATUSES;
