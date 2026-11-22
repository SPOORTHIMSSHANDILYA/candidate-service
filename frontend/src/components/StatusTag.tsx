import { Tag } from 'antd';
import type { CandidateStatus } from '../types';
import { statusLabel } from '../utils/status';

const COLORS: Record<CandidateStatus, string> = {
  ACTIVE: 'green',
  INACTIVE: 'gold',
  ARCHIVED: 'default',
};

/** Small coloured label for a candidate's status. */
export function StatusTag({ status }: { status: CandidateStatus }) {
  return <Tag color={COLORS[status]}>{statusLabel(status)}</Tag>;
}
