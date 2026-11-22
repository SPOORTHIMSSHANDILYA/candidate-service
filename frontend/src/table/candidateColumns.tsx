import { Tag, Typography } from 'antd';
import type { TableProps } from 'antd';
import dayjs from 'dayjs';
import type { ColumnOption } from '../components/ColumnSettings';
import { StatusTag } from '../components/StatusTag';
import type { CandidateSummary } from '../types';

// Every column the candidate table can show, in ONE list (like the filters).
// To add a column: add one entry to ALL_COLUMNS (and the field to the backend's CandidateSummary).

export type Column = NonNullable<TableProps<CandidateSummary>['columns']>[number] & { key: string };

const dash = (v: unknown) => (v === null || v === undefined || v === '' ? '—' : String(v));
const years = (v: number | null | undefined) => (v === null || v === undefined ? '—' : `${v} yr${v === 1 ? '' : 's'}`);
const date = (v: string | undefined) => (v ? dayjs(v).format('D MMM YYYY') : '—');

/** Which backend sort field each sortable column uses (must be in the backend's whitelist). */
export const SORT_FIELD: Record<string, string> = {
  name: 'lastName',
  experience: 'yearsExperience',
  created: 'createdAt',
  updated: 'updatedAt',
};

/** Every column the table can show, in display order. */
export const ALL_COLUMNS: Column[] = [
  {
    key: 'code',
    title: 'Code',
    dataIndex: 'candidateCode',
    width: 110,
    render: (v: string) => <Typography.Text type="secondary">{v}</Typography.Text>,
  },
  {
    key: 'name',
    title: 'Candidate',
    sorter: true,
    render: (_: unknown, c: CandidateSummary) => (
      <div>
        <div style={{ fontWeight: 500 }}>
          {c.firstName} {c.lastName}
        </div>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {c.email}
        </Typography.Text>
      </div>
    ),
  },
  { key: 'title', title: 'Current title', dataIndex: 'currentTitle', ellipsis: true, render: dash },
  { key: 'company', title: 'Company', dataIndex: 'currentCompany', ellipsis: true, render: dash },
  { key: 'experience', title: 'Experience', dataIndex: 'yearsExperience', width: 120, sorter: true, render: years },
  {
    key: 'notice',
    title: 'Notice',
    dataIndex: 'noticePeriodDays',
    width: 100,
    render: (v: number | null | undefined) => (v === null || v === undefined ? '—' : `${v} days`),
  },
  { key: 'city', title: 'City', dataIndex: 'city', width: 130, render: dash },
  { key: 'country', title: 'Country', dataIndex: 'country', width: 120, render: dash },
  {
    key: 'status',
    title: 'Status',
    dataIndex: 'candidateStatus',
    width: 110,
    render: (s: CandidateSummary['candidateStatus']) => <StatusTag status={s} />,
  },
  {
    key: 'availability',
    title: 'Availability',
    dataIndex: 'openToWork',
    width: 130,
    render: (open: boolean) => (open ? <Tag color="blue">Open to work</Tag> : <Tag>Not looking</Tag>),
  },
  { key: 'created', title: 'Added', dataIndex: 'createdAt', width: 130, sorter: true, render: date },
  { key: 'updated', title: 'Updated', dataIndex: 'updatedAt', width: 130, sorter: true, render: date },
];

export const COLUMN_OPTIONS: ColumnOption[] = ALL_COLUMNS.map((c) => ({
  key: c.key,
  title: String(c.title),
  locked: c.key === 'name',
}));

export const DEFAULT_COLUMNS = ['code', 'name', 'title', 'experience', 'city', 'status', 'availability'];
