import { SettingOutlined } from '@ant-design/icons';
import { Button, Checkbox, Divider, Flex, Popover, Typography } from 'antd';

export interface ColumnOption {
  key: string;
  title: string;
  /** Columns that can't be hidden (e.g. the candidate's name). */
  locked?: boolean;
}

interface Props {
  columns: ColumnOption[];
  visible: string[];
  onChange: (visible: string[]) => void;
  onReset: () => void;
}

/** "Columns" button: choose which table columns to show. */
export function ColumnSettings({ columns, visible, onChange, onReset }: Props) {
  const toggle = (key: string, checked: boolean) =>
    onChange(checked ? columns.map((c) => c.key).filter((k) => k === key || visible.includes(k)) : visible.filter((k) => k !== key));

  const content = (
    <Flex vertical style={{ width: 220 }}>
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        Show columns
      </Typography.Text>
      <Flex vertical gap={6} style={{ marginTop: 8 }}>
        {columns.map((c) => (
          <Checkbox
            key={c.key}
            checked={c.locked || visible.includes(c.key)}
            disabled={c.locked}
            onChange={(e) => toggle(c.key, e.target.checked)}
          >
            {c.title}
          </Checkbox>
        ))}
      </Flex>
      <Divider style={{ margin: '10px 0' }} />
      <Button size="small" type="link" onClick={onReset} style={{ alignSelf: 'flex-start', padding: 0 }}>
        Reset to default
      </Button>
    </Flex>
  );

  return (
    <Popover content={content} trigger="click" placement="bottomRight">
      <Button icon={<SettingOutlined />}>Columns</Button>
    </Popover>
  );
}
