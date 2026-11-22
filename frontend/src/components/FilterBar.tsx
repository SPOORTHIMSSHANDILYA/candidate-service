import { PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Checkbox, Divider, Flex, Input, Popover, Typography } from 'antd';
import { useMemo, useState } from 'react';
import { FILTERS, findFilter, isActive } from '../filters/filterDefinitions';
import type { SearchFilters } from '../types';
import { FilterChip } from './FilterChip';

interface Props {
  searchText: string;
  onSearchTextChange: (text: string) => void;
  filters: SearchFilters;
  onFiltersChange: (next: SearchFilters) => void;
  /** Non-pinned filters the user has added to the bar. */
  extraKeys: string[];
  onExtraKeysChange: (keys: string[]) => void;
  onClearAll: () => void;
}

/**
 * Jira-style filter bar: search box, pinned filter buttons, "+ More filters", "Clear all".
 * Built entirely from FILTERS, so it scales to many filters without changing this file.
 */
export function FilterBar(props: Props) {
  const { searchText, onSearchTextChange, filters, onFiltersChange, extraKeys, onExtraKeysChange, onClearAll } = props;

  // Pinned filters, then the ones added from "More", then any active non-pinned one (e.g. from a saved view).
  const visible = useMemo(() => {
    const keys = new Set<string>(FILTERS.filter((f) => f.pinned).map((f) => f.key));
    extraKeys.forEach((k) => keys.add(k));
    FILTERS.forEach((f) => isActive(f, filters) && keys.add(f.key));
    return [...keys].map(findFilter).filter((f) => f !== undefined);
  }, [extraKeys, filters]);

  const anyActive = !!searchText.trim() || FILTERS.some((f) => isActive(f, filters));

  return (
    <Flex wrap gap={8} align="center">
      <Input
        prefix={<SearchOutlined />}
        placeholder="Search candidates…"
        value={searchText}
        onChange={(e) => onSearchTextChange(e.target.value)}
        allowClear
        style={{ width: 280 }}
      />
      {visible.map((def) => (
        <FilterChip
          key={def.key}
          def={def}
          filters={filters}
          onChange={onFiltersChange}
          onRemove={def.pinned ? undefined : () => onExtraKeysChange(extraKeys.filter((k) => k !== def.key))}
        />
      ))}
      <MoreFilters
        selected={extraKeys}
        onChange={onExtraKeysChange}
      />
      {anyActive && (
        <Button type="link" onClick={onClearAll}>
          Clear all
        </Button>
      )}
    </Flex>
  );
}

/** "+ More filters": a searchable checklist of the non-pinned filters. */
function MoreFilters({ selected, onChange }: { selected: string[]; onChange: (keys: string[]) => void }) {
  const [find, setFind] = useState('');
  const others = FILTERS.filter((f) => !f.pinned);
  const shown = others.filter((f) => f.label.toLowerCase().includes(find.trim().toLowerCase()));

  const content = (
    <Flex vertical style={{ width: 240 }}>
      <Input
        size="small"
        placeholder="Find a filter"
        value={find}
        onChange={(e) => setFind(e.target.value)}
        allowClear
      />
      <Divider style={{ margin: '8px 0' }} />
      <Flex vertical gap={6} style={{ maxHeight: 280, overflowY: 'auto' }}>
        {shown.length === 0 && <Typography.Text type="secondary">No filters match</Typography.Text>}
        {shown.map((f) => (
          <Checkbox
            key={f.key}
            checked={selected.includes(f.key)}
            onChange={(e) =>
              onChange(e.target.checked ? [...selected, f.key] : selected.filter((k) => k !== f.key))
            }
          >
            {f.label}
          </Checkbox>
        ))}
      </Flex>
    </Flex>
  );

  return (
    <Popover content={content} trigger="click" placement="bottomLeft">
      <Button type="dashed" icon={<PlusOutlined />}>
        More filters
      </Button>
    </Popover>
  );
}
