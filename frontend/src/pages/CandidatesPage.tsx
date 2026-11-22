import { PlusOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Flex, Typography } from 'antd';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { searchCandidates } from '../api/candidates';
import { CandidateDetailsDrawer } from '../components/CandidateDetailsDrawer';
import { CandidateFormDrawer } from '../components/CandidateFormDrawer';
import { CandidateTable } from '../components/CandidateTable';
import { COLUMN_OPTIONS, DEFAULT_COLUMNS } from '../table/candidateColumns';
import { ColumnSettings } from '../components/ColumnSettings';
import { FilterBar } from '../components/FilterBar';
import { SavedViews, type SavedView } from '../components/SavedViews';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import type { Candidate, CandidateSummary, PageRequest, PageResponse, SearchFilters } from '../types';
import { loadJson, saveJson } from '../utils/storage';
import { dropEmpty, readUrlState, writeUrlState } from '../utils/urlState';

const DEFAULT_PAGING: PageRequest = { page: 0, size: 20, sort: 'createdAt,desc' };
const VIEWS_KEY = 'candidate-crm.views.v1';
const COLUMNS_KEY = 'candidate-crm.columns.v1';

const sameSearch = (a: SearchFilters, b: SearchFilters) => JSON.stringify(dropEmpty(a)) === JSON.stringify(dropEmpty(b));

/**
 * The main screen.
 * Data flow:  search text (debounced) + filters + paging  ──►  GET /candidates  ──►  table
 * The current search is also written to the address bar, so it can be bookmarked or shared.
 */
export function CandidatesPage() {
  // Start from whatever is in the address bar (a shared link or a page refresh).
  const initial = useMemo(() => readUrlState(DEFAULT_PAGING), []);

  const [searchText, setSearchText] = useState(initial.filters.q ?? '');
  const debouncedSearch = useDebouncedValue(searchText, 400);
  const [filters, setFilters] = useState<SearchFilters>(initial.filters);
  const [paging, setPaging] = useState<PageRequest>(initial.paging);
  const [extraKeys, setExtraKeys] = useState<string[]>([]);

  const [views, setViews] = useState<SavedView[]>(() => loadJson(VIEWS_KEY, []));
  const [activeViewId, setActiveViewId] = useState<string | null>(null);
  const [visibleColumns, setVisibleColumns] = useState<string[]>(() => loadJson(COLUMNS_KEY, DEFAULT_COLUMNS));

  const [data, setData] = useState<PageResponse<CandidateSummary> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const [detailsId, setDetailsId] = useState<number | null>(null);
  const [form, setForm] = useState<{ open: boolean; candidate: Candidate | null }>({ open: false, candidate: null });

  // New search text -> search from page 1 (both updates land in one render = one request).
  useEffect(() => {
    setFilters((prev) => (prev.q === debouncedSearch ? prev : { ...prev, q: debouncedSearch }));
    setPaging((prev) => (prev.page === 0 ? prev : { ...prev, page: 0 }));
  }, [debouncedSearch]);

  // Load the table whenever the search changes; ignore answers that arrive out of order.
  const latestRequest = useRef(0);
  useEffect(() => {
    const requestId = ++latestRequest.current;
    writeUrlState({ filters, paging }, DEFAULT_PAGING);
    setLoading(true);
    setError(null);
    searchCandidates(filters, paging)
      .then((result) => requestId === latestRequest.current && setData(result))
      .catch((e: Error) => requestId === latestRequest.current && setError(e.message))
      .finally(() => requestId === latestRequest.current && setLoading(false));
  }, [filters, paging, reloadKey]);

  // The active view is "active" only while the search still matches it.
  const activeView = views.find((v) => v.id === activeViewId);
  const viewStillMatches = activeView ? sameSearch(activeView.filters, filters) && activeView.sort === paging.sort : false;

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  const changeFilters = (next: SearchFilters) => {
    setFilters({ ...next, q: filters.q });
    setPaging((prev) => ({ ...prev, page: 0 }));
  };

  const clearAll = () => {
    setSearchText('');
    setFilters({});
    setExtraKeys([]);
    setPaging((prev) => ({ ...prev, page: 0, sort: DEFAULT_PAGING.sort }));
    setActiveViewId(null);
  };

  // ---- saved views (kept in this browser)
  const applyView = (v: SavedView) => {
    setSearchText(v.filters.q ?? '');
    setFilters(v.filters);
    setExtraKeys(v.extraKeys);
    setPaging((prev) => ({ ...prev, page: 0, sort: v.sort }));
    setActiveViewId(v.id);
  };
  const saveView = (name: string) => {
    const view: SavedView = {
      id: crypto.randomUUID(),
      name,
      filters: dropEmpty({ ...filters, q: searchText }),
      sort: paging.sort,
      extraKeys,
    };
    const next = [...views, view];
    setViews(next);
    saveJson(VIEWS_KEY, next);
    setActiveViewId(view.id);
  };
  const deleteView = (id: string) => {
    const next = views.filter((v) => v.id !== id);
    setViews(next);
    saveJson(VIEWS_KEY, next);
    if (activeViewId === id) setActiveViewId(null);
  };

  // ---- columns (kept in this browser)
  const changeColumns = (next: string[]) => {
    setVisibleColumns(next);
    saveJson(COLUMNS_KEY, next);
  };

  const onSaved = (saved: Candidate) => {
    setForm({ open: false, candidate: null });
    reload();
    setDetailsId(saved.id);
  };

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <div>
          <Typography.Title level={3} style={{ margin: 0 }}>
            Candidates
          </Typography.Title>
          <Typography.Text type="secondary">Search, filter and manage candidate records</Typography.Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setForm({ open: true, candidate: null })}>
          Add candidate
        </Button>
      </Flex>

      <Card styles={{ body: { padding: 0 } }}>
        <Flex justify="space-between" align="flex-start" gap={12} className="toolbar">
          <FilterBar
            searchText={searchText}
            onSearchTextChange={setSearchText}
            filters={filters}
            onFiltersChange={changeFilters}
            extraKeys={extraKeys}
            onExtraKeysChange={setExtraKeys}
            onClearAll={clearAll}
          />
          <Flex gap={8} style={{ flexShrink: 0 }}>
            <SavedViews
              views={views}
              activeViewId={viewStillMatches ? activeViewId : null}
              onApply={applyView}
              onSave={saveView}
              onDelete={deleteView}
            />
            <ColumnSettings
              columns={COLUMN_OPTIONS}
              visible={visibleColumns}
              onChange={changeColumns}
              onReset={() => changeColumns(DEFAULT_COLUMNS)}
            />
          </Flex>
        </Flex>

        {error && (
          <Alert
            type="error"
            showIcon
            style={{ margin: '0 16px 12px' }}
            title="Could not load candidates"
            description={error}
            action={<Button onClick={reload}>Retry</Button>}
          />
        )}

        <CandidateTable
          data={data}
          loading={loading}
          visibleColumns={visibleColumns}
          sort={paging.sort}
          onPageChange={(page, size) => setPaging((prev) => ({ ...prev, page, size }))}
          onSortChange={(sort) => setPaging((prev) => ({ ...prev, sort, page: 0 }))}
          onRowClick={(c) => setDetailsId(c.id)}
        />
      </Card>

      <CandidateDetailsDrawer
        candidateId={detailsId}
        onClose={() => setDetailsId(null)}
        onEdit={(candidate) => {
          setDetailsId(null);
          setForm({ open: true, candidate });
        }}
        onChanged={reload}
      />

      <CandidateFormDrawer
        open={form.open}
        candidate={form.candidate}
        onClose={() => setForm({ open: false, candidate: null })}
        onSaved={onSaved}
      />
    </Flex>
  );
}
