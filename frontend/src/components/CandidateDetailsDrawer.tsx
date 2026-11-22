import { DeleteOutlined, EditOutlined } from '@ant-design/icons';
import {
  Alert,
  App,
  Button,
  Descriptions,
  Drawer,
  Popconfirm,
  Select,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import { useCallback, useEffect, useState } from 'react';
import { changeCandidateStatus, deleteCandidate, getCandidate } from '../api/candidates';
import { ApiError } from '../api/client';
import { CANDIDATE_STATUSES, type Candidate, type CandidateStatus } from '../types';
import { statusLabel } from '../utils/status';
import { StatusTag } from './StatusTag';

interface Props {
  candidateId: number | null; // null = drawer closed
  onClose: () => void;
  onEdit: (candidate: Candidate) => void;
  /** Something changed (status, delete): the list should reload. */
  onChanged: () => void;
}

const dash = (v: unknown) => (v === null || v === undefined || v === '' ? '—' : String(v));
const formatDate = (iso: string) => dayjs(iso).format('D MMM YYYY, HH:mm');

/** Read-only view of one candidate, with Edit / Status / Delete actions. */
export function CandidateDetailsDrawer({ candidateId, onClose, onEdit, onChanged }: Props) {
  const { message } = App.useApp();
  const [candidate, setCandidate] = useState<Candidate | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async (id: number) => {
    setLoading(true);
    setError(null);
    try {
      setCandidate(await getCandidate(id));
    } catch (e) {
      setCandidate(null);
      setError(e instanceof ApiError && e.status === 404 ? 'This candidate no longer exists.' : String((e as Error).message));
    } finally {
      setLoading(false);
    }
  }, []);

  // Load the candidate whenever the drawer opens for a (new) id.
  useEffect(() => {
    if (candidateId !== null) void load(candidateId);
  }, [candidateId, load]);

  const changeStatus = async (status: CandidateStatus) => {
    if (!candidate || status === candidate.candidateStatus) return;
    setBusy(true);
    try {
      // Send the version we loaded: the backend refuses (409) if someone changed it meanwhile.
      setCandidate(await changeCandidateStatus(candidate.id, status, candidate.version));
      message.success(`Status changed to ${statusLabel(status)}`);
      onChanged();
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        message.warning('Someone else changed this candidate. Showing the latest version.');
        await load(candidate.id);
      } else {
        message.error((e as Error).message);
      }
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!candidate) return;
    setBusy(true);
    try {
      await deleteCandidate(candidate.id);
      message.success('Candidate deleted');
      onChanged();
      onClose();
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Drawer
      open={candidateId !== null}
      onClose={onClose}
      size={560}
      title={
        candidate ? (
          <Space orientation="vertical" size={0}>
            <span>
              {candidate.firstName} {candidate.lastName}
            </span>
            <Typography.Text type="secondary" style={{ fontSize: 13, fontWeight: 400 }}>
              {candidate.candidateCode} · {dash(candidate.currentTitle)}
            </Typography.Text>
          </Space>
        ) : (
          'Candidate'
        )
      }
      extra={
        candidate && (
          <Space>
            <Button icon={<EditOutlined />} onClick={() => onEdit(candidate)} disabled={busy}>
              Edit
            </Button>
            <Popconfirm
              title="Delete this candidate?"
              description="This permanently removes the candidate and their skills."
              okText="Delete"
              okButtonProps={{ danger: true }}
              onConfirm={remove}
            >
              <Button danger icon={<DeleteOutlined />} disabled={busy}>
                Delete
              </Button>
            </Popconfirm>
          </Space>
        )
      }
    >
      {loading && <Skeleton active paragraph={{ rows: 10 }} />}
      {!loading && error && <Alert type="error" title={error} showIcon />}

      {!loading && candidate && (
        <Space orientation="vertical" size="large" style={{ width: '100%' }}>
          <Space wrap>
            <StatusTag status={candidate.candidateStatus} />
            {candidate.openToWork ? <Tag color="blue">Open to work</Tag> : <Tag>Not looking</Tag>}
            <span style={{ marginLeft: 8 }}>Change status:</span>
            <Select
              size="small"
              value={candidate.candidateStatus}
              onChange={changeStatus}
              loading={busy}
              disabled={busy}
              style={{ width: 130 }}
              options={CANDIDATE_STATUSES.map((s) => ({ value: s, label: statusLabel(s) }))}
            />
          </Space>

          <Descriptions title="Contact" column={1} size="small" bordered>
            <Descriptions.Item label="Email">{candidate.email}</Descriptions.Item>
            <Descriptions.Item label="Phone">{dash(candidate.phone)}</Descriptions.Item>
            <Descriptions.Item label="Location">
              {[candidate.city, candidate.country].filter(Boolean).join(', ') || '—'}
            </Descriptions.Item>
          </Descriptions>

          <Descriptions title="Professional" column={1} size="small" bordered>
            <Descriptions.Item label="Current title">{dash(candidate.currentTitle)}</Descriptions.Item>
            <Descriptions.Item label="Current company">{dash(candidate.currentCompany)}</Descriptions.Item>
            <Descriptions.Item label="Experience">
              {candidate.yearsExperience === null ? '—' : `${candidate.yearsExperience} years`}
            </Descriptions.Item>
            <Descriptions.Item label="Notice period">
              {candidate.noticePeriodDays === null ? '—' : `${candidate.noticePeriodDays} days`}
            </Descriptions.Item>
            <Descriptions.Item label="Skills">
              {candidate.skills.length === 0
                ? '—'
                : [...candidate.skills].sort().map((s) => <Tag key={s}>{s}</Tag>)}
            </Descriptions.Item>
          </Descriptions>

          <Descriptions title="Record" column={1} size="small">
            <Descriptions.Item label="Created">
              {formatDate(candidate.createdAt)}
              {candidate.createdBy ? ` by ${candidate.createdBy}` : ''}
            </Descriptions.Item>
            <Descriptions.Item label="Last updated">
              {formatDate(candidate.updatedAt)}
              {candidate.updatedBy ? ` by ${candidate.updatedBy}` : ''}
            </Descriptions.Item>
          </Descriptions>
        </Space>
      )}
    </Drawer>
  );
}
