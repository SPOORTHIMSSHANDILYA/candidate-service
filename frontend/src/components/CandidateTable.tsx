import { Empty, Flex, Pagination, Table, Typography } from 'antd';
import type { TableProps } from 'antd';
import { ALL_COLUMNS, SORT_FIELD } from '../table/candidateColumns';
import type { CandidateSummary, PageResponse } from '../types';

interface Props {
  data: PageResponse<CandidateSummary> | null;
  loading: boolean;
  visibleColumns: string[];
  sort: string; // e.g. "lastName,asc"
  onPageChange: (page: number, size: number) => void;
  onSortChange: (sort: string) => void;
  onRowClick: (candidate: CandidateSummary) => void;
}

/** The candidate list. Paging and sorting happen on the server. */
export function CandidateTable(props: Props) {
  const { data, loading, visibleColumns, sort, onPageChange, onSortChange, onRowClick } = props;
  const [sortField, sortDir] = sort.split(',');

  const columns = ALL_COLUMNS.filter((c) => c.key === 'name' || visibleColumns.includes(c.key)).map((c) => ({
    ...c,
    sortOrder: SORT_FIELD[c.key] === sortField ? (sortDir === 'asc' ? ('ascend' as const) : ('descend' as const)) : null,
  }));

  const handleChange: TableProps<CandidateSummary>['onChange'] = (_p, _f, sorter) => {
    const s = Array.isArray(sorter) ? sorter[0] : sorter;
    const field = s?.columnKey ? SORT_FIELD[String(s.columnKey)] : undefined;
    onSortChange(field && s.order ? `${field},${s.order === 'ascend' ? 'asc' : 'desc'}` : 'createdAt,desc');
  };

  const page = data?.page ?? 0;
  const size = data?.size ?? 20;
  const total = data?.totalElements ?? 0;
  const from = total === 0 ? 0 : page * size + 1;
  const to = Math.min(total, (page + 1) * size);

  return (
    <>
      <Table<CandidateSummary>
        rowKey="id"
        columns={columns}
        dataSource={data?.content ?? []}
        loading={loading}
        onChange={handleChange}
        onRow={(record) => ({ onClick: () => onRowClick(record) })}
        rowClassName={() => 'clickable-row'}
        pagination={false}
        scroll={{ x: 'max-content' }}
        size="middle"
        locale={{ emptyText: <Empty description="No candidates match your search" /> }}
      />
      <Flex justify="space-between" align="center" wrap gap={12} className="table-footer">
        <Typography.Text type="secondary">
          {total === 0 ? 'No results' : (
            <>
              Showing <Typography.Text strong>{from}–{to}</Typography.Text> of{' '}
              <Typography.Text strong>{total.toLocaleString()}</Typography.Text> candidates
            </>
          )}
        </Typography.Text>
        <Pagination
          size="small"
          current={page + 1}
          pageSize={size}
          total={total}
          showSizeChanger
          pageSizeOptions={[10, 20, 50, 100]}
          onChange={(p, s) => onPageChange(s !== size ? 0 : p - 1, s)}
          hideOnSinglePage={false}
        />
      </Flex>
    </>
  );
}
