import { Button, Col, Input, InputNumber, Row, Segmented, Select, Space } from 'antd';
import { useState } from 'react';
import { CANDIDATE_STATUSES, type SearchFilters } from '../types';
import { SkillSelect } from './SkillSelect';
import { statusLabel } from '../utils/status';

type FilterValues = Omit<SearchFilters, 'q'>;

interface Props {
  /** Called once with all chosen filters when the user presses Apply. */
  onApply: (filters: FilterValues) => void;
  onClear: () => void;
}

const EMPTY: FilterValues = { skills: [], skillMatch: 'ALL', status: [] };

/**
 * Filter bar. Choosing a filter does NOT call the API on its own:
 * the user picks several filters, then presses Apply (one request).
 */
export function CandidateFilters({ onApply, onClear }: Props) {
  const [draft, setDraft] = useState<FilterValues>(EMPTY);

  // Update one field of the draft filters.
  const set = <K extends keyof FilterValues>(key: K, value: FilterValues[K]) =>
    setDraft((prev) => ({ ...prev, [key]: value }));

  const clear = () => {
    setDraft(EMPTY);
    onClear();
  };

  return (
    <div>
      <Row gutter={[12, 12]}>
        <Col xs={24} md={8} lg={6}>
          <Input
            placeholder="City"
            value={draft.city}
            onChange={(e) => set('city', e.target.value)}
            allowClear
          />
        </Col>
        <Col xs={24} md={8} lg={6}>
          <Space.Compact style={{ width: '100%' }}>
            <InputNumber
              placeholder="Min exp (yrs)"
              min={0}
              max={60}
              value={draft.minExperience}
              onChange={(v) => set('minExperience', v ?? undefined)}
              style={{ width: '50%' }}
            />
            <InputNumber
              placeholder="Max exp (yrs)"
              min={0}
              max={60}
              value={draft.maxExperience}
              onChange={(v) => set('maxExperience', v ?? undefined)}
              style={{ width: '50%' }}
            />
          </Space.Compact>
        </Col>
        <Col xs={24} md={8} lg={6}>
          <Select
            mode="multiple"
            placeholder="Status (default: hides Archived)"
            value={draft.status}
            onChange={(v) => set('status', v)}
            options={CANDIDATE_STATUSES.map((s) => ({ value: s, label: statusLabel(s) }))}
            allowClear
            style={{ width: '100%' }}
          />
        </Col>
        <Col xs={24} md={8} lg={6}>
          <InputNumber
            placeholder="Max notice period (days)"
            min={0}
            max={365}
            value={draft.maxNoticeDays}
            onChange={(v) => set('maxNoticeDays', v ?? undefined)}
            style={{ width: '100%' }}
          />
        </Col>
        <Col xs={24} md={16} lg={12}>
          <Space.Compact style={{ width: '100%' }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <SkillSelect
                value={draft.skills}
                onChange={(v) => set('skills', v)}
                placeholder="Skills"
              />
            </div>
            <Segmented
              options={[
                { label: 'All', value: 'ALL' },
                { label: 'Any', value: 'ANY' },
              ]}
              value={draft.skillMatch}
              onChange={(v) => set('skillMatch', v as 'ALL' | 'ANY')}
              title="All = has every skill, Any = has at least one"
            />
          </Space.Compact>
        </Col>
        <Col xs={24} md={8} lg={6}>
          <Select
            placeholder="Open to work"
            value={draft.openToWork === undefined ? undefined : String(draft.openToWork)}
            onChange={(v) => set('openToWork', v === undefined ? undefined : v === 'true')}
            options={[
              { value: 'true', label: 'Open to work' },
              { value: 'false', label: 'Not looking' },
            ]}
            allowClear
            style={{ width: '100%' }}
          />
        </Col>
        <Col xs={24} lg={6} style={{ textAlign: 'right' }}>
          <Space>
            <Button onClick={clear}>Clear</Button>
            <Button type="primary" onClick={() => onApply(draft)}>
              Apply filters
            </Button>
          </Space>
        </Col>
      </Row>
    </div>
  );
}
