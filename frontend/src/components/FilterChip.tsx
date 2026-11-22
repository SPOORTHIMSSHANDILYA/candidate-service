import { CloseOutlined, DownOutlined } from '@ant-design/icons';
import { Button, Checkbox, Flex, Input, InputNumber, Popover, Radio, Segmented, Typography } from 'antd';
import { useState } from 'react';
import {
  ALL_STATUSES,
  clearFilter,
  isActive,
  summary,
  type FilterDefinition,
} from '../filters/filterDefinitions';
import type { SearchFilters } from '../types';
import { statusLabel } from '../utils/status';
import { SkillSelect } from './SkillSelect';

interface Props {
  def: FilterDefinition;
  filters: SearchFilters;
  onChange: (next: SearchFilters) => void;
  /** Only for filters added from "More filters": removes the chip itself. */
  onRemove?: () => void;
}

/**
 * One filter button, Jira style: "City ▾" when unused, "City: Pune ✕" when active.
 * Clicking opens a small panel; nothing is searched until "Apply".
 */
export function FilterChip({ def, filters, onChange, onRemove }: Props) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<SearchFilters>(filters);
  const active = isActive(def, filters);

  const openChange = (next: boolean) => {
    if (next) setDraft(filters); // start editing from what is applied now
    setOpen(next);
  };

  const apply = () => {
    onChange(draft);
    setOpen(false);
  };

  const clear = () => {
    onChange(clearFilter(def, filters));
    setOpen(false);
  };

  const set = (patch: Partial<SearchFilters>) => setDraft((prev) => ({ ...prev, ...patch }));

  const content = (
    <Flex vertical gap={12} style={{ width: 280 }}>
      <Typography.Text strong>{def.label}</Typography.Text>
      <Editor def={def} draft={draft} set={set} onEnter={apply} />
      <Flex justify="space-between">
        <Button size="small" type="text" onClick={clear} disabled={!active}>
          Clear
        </Button>
        <Button size="small" type="primary" onClick={apply}>
          Apply
        </Button>
      </Flex>
    </Flex>
  );

  return (
    <Popover content={content} trigger="click" open={open} onOpenChange={openChange} placement="bottomLeft">
      <Button
        type={active ? 'primary' : 'default'}
        ghost={active}
        style={{ maxWidth: 260 }}
        icon={
          active || onRemove ? (
            <CloseOutlined
              style={{ fontSize: 10 }}
              onClick={(e) => {
                e.stopPropagation();
                if (active) onChange(clearFilter(def, filters));
                else onRemove?.();
              }}
            />
          ) : (
            <DownOutlined style={{ fontSize: 10 }} />
          )
        }
        iconPlacement="end"
      >
        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {active ? `${def.label}: ${summary(def, filters)}` : def.label}
        </span>
      </Button>
    </Popover>
  );
}

/** The input(s) inside the popover, chosen by the filter's editor kind. */
function Editor({
  def,
  draft,
  set,
  onEnter,
}: {
  def: FilterDefinition;
  draft: SearchFilters;
  set: (patch: Partial<SearchFilters>) => void;
  onEnter: () => void;
}) {
  const e = def.editor;
  switch (e.kind) {
    case 'statuses':
      return (
        <Checkbox.Group
          value={draft.status ?? []}
          onChange={(v) => set({ status: v })}
          options={ALL_STATUSES.map((s) => ({ value: s, label: statusLabel(s) }))}
          style={{ display: 'flex', flexDirection: 'column', gap: 6 }}
        />
      );
    case 'skills':
      return (
        <Flex vertical gap={8}>
          <SkillSelect value={draft.skills ?? []} onChange={(v) => set({ skills: v })} placeholder="Type to find skills" />
          <Flex align="center" gap={8}>
            <Typography.Text type="secondary">Match</Typography.Text>
            <Segmented
              size="small"
              value={draft.skillMatch ?? 'ALL'}
              onChange={(v) => set({ skillMatch: v as 'ALL' | 'ANY' })}
              options={[
                { label: 'All skills', value: 'ALL' },
                { label: 'Any skill', value: 'ANY' },
              ]}
            />
          </Flex>
        </Flex>
      );
    case 'text':
      return (
        <Input
          autoFocus
          placeholder={e.placeholder}
          value={draft[e.field] ?? ''}
          onChange={(ev) => set({ [e.field]: ev.target.value || undefined })}
          onPressEnter={onEnter}
          allowClear
        />
      );
    case 'range':
      return (
        <Flex align="center" gap={8}>
          <InputNumber
            placeholder="Min"
            min={0}
            max={e.limit}
            value={draft[e.min]}
            onChange={(v) => set({ [e.min]: v ?? undefined })}
            style={{ flex: 1 }}
          />
          <span>to</span>
          <InputNumber
            placeholder="Max"
            min={0}
            max={e.limit}
            value={draft[e.max]}
            onChange={(v) => set({ [e.max]: v ?? undefined })}
            style={{ flex: 1 }}
          />
          <Typography.Text type="secondary">{e.unit}</Typography.Text>
        </Flex>
      );
    case 'atMost':
      return (
        <Flex align="center" gap={8}>
          <span>At most</span>
          <InputNumber
            min={0}
            max={e.limit}
            value={draft[e.field]}
            onChange={(v) => set({ [e.field]: v ?? undefined })}
            style={{ flex: 1 }}
          />
          <Typography.Text type="secondary">{e.unit}</Typography.Text>
        </Flex>
      );
    case 'yesNo':
      return (
        <Radio.Group
          value={draft[e.field] === undefined ? 'any' : String(draft[e.field])}
          onChange={(ev) => set({ [e.field]: ev.target.value === 'any' ? undefined : ev.target.value === 'true' })}
          style={{ display: 'flex', flexDirection: 'column', gap: 6 }}
          options={[
            { value: 'any', label: 'Any' },
            { value: 'true', label: e.yes },
            { value: 'false', label: e.no },
          ]}
        />
      );
  }
}
