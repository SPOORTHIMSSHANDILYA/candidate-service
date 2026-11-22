import { ReloadOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Col, Flex, Row, Skeleton, Statistic, Tooltip, Typography } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { getCandidateStats } from '../api/candidates';
import { CANDIDATE_STATUSES, type CandidateStats, type CandidateStatus } from '../types';
import { statusLabel } from '../utils/status';

const STATUS_COLORS: Record<CandidateStatus, string> = {
  ACTIVE: '#52c41a',
  INACTIVE: '#faad14',
  ARCHIVED: '#bfbfbf',
};

const percent = (part: number, whole: number) => (whole === 0 ? 0 : Math.round((part / whole) * 100));

interface Props {
  /** Open the candidate list filtered to one status. */
  onOpenStatus: (status: CandidateStatus) => void;
}

/** Overview: four headline numbers and the split by status. */
export function OverviewPage({ onOpenStatus }: Props) {
  const [stats, setStats] = useState<CandidateStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setStats(await getCandidateStats());
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const total = stats?.total ?? 0;
  const active = stats?.countsByStatus.ACTIVE ?? 0;

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <div>
          <Typography.Title level={3} style={{ margin: 0 }}>
            Overview
          </Typography.Title>
          <Typography.Text type="secondary">Candidate pipeline at a glance</Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>
          Refresh
        </Button>
      </Flex>

      {error && (
        <Alert
          type="error"
          showIcon
          title="Could not load the overview"
          description={error}
          action={<Button onClick={load}>Retry</Button>}
        />
      )}

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="Total candidates" value={total} loading={loading} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="Active" value={active} loading={loading} />
            {!loading && stats && (
              <Typography.Text type="success" style={{ fontSize: 12 }}>
                {percent(active, total)}% of total
              </Typography.Text>
            )}
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="Added last 7 days" value={stats?.addedInLast7Days ?? 0} loading={loading} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Open to work"
              value={percent(stats?.openToWork ?? 0, total)}
              suffix="%"
              loading={loading}
            />
            {!loading && stats && (
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {stats.openToWork.toLocaleString()} candidates
              </Typography.Text>
            )}
          </Card>
        </Col>
      </Row>

      <Card title="By status">
        {loading || !stats ? (
          <Skeleton active paragraph={{ rows: 1 }} title={false} />
        ) : (
          <Flex vertical gap={12}>
            {/* One bar split by status; each part is as wide as its share. */}
            <div className="status-bar" role="img" aria-label="Candidates by status">
              {CANDIDATE_STATUSES.map((s) => {
                const count = stats.countsByStatus[s] ?? 0;
                return count === 0 ? null : (
                  <Tooltip key={s} title={`${statusLabel(s)}: ${count.toLocaleString()} (${percent(count, total)}%)`}>
                    <div
                      className="status-bar-part"
                      style={{ width: `${(count / total) * 100}%`, background: STATUS_COLORS[s] }}
                      onClick={() => onOpenStatus(s)}
                    />
                  </Tooltip>
                );
              })}
            </div>
            <Flex gap={24} wrap>
              {CANDIDATE_STATUSES.map((s) => (
                <Button key={s} type="link" size="small" style={{ padding: 0 }} onClick={() => onOpenStatus(s)}>
                  <span className="status-dot" style={{ background: STATUS_COLORS[s] }} />
                  <Typography.Text>
                    {statusLabel(s)}{' '}
                    <Typography.Text strong>{(stats.countsByStatus[s] ?? 0).toLocaleString()}</Typography.Text>
                  </Typography.Text>
                </Button>
              ))}
            </Flex>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              Click a status to see those candidates.
            </Typography.Text>
          </Flex>
        )}
      </Card>
    </Flex>
  );
}
