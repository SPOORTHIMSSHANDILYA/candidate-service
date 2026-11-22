import type { CandidateStatus } from '../types';

const LABELS: Record<CandidateStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  ARCHIVED: 'Archived',
};

/** "ACTIVE" -> "Active" */
export function statusLabel(status: CandidateStatus): string {
  return LABELS[status];
}
