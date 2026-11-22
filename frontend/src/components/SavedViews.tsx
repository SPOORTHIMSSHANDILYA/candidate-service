import { DeleteOutlined, DownOutlined, SaveOutlined, StarOutlined } from '@ant-design/icons';
import { App, Button, Dropdown, Flex, Input, Modal, Typography } from 'antd';
import type { MenuProps } from 'antd';
import { useState } from 'react';
import type { SearchFilters } from '../types';

/** A named, saved search: filters (incl. search text) and sort order. */
export interface SavedView {
  id: string;
  name: string;
  filters: SearchFilters;
  sort: string;
  extraKeys: string[];
}

interface Props {
  views: SavedView[];
  activeViewId: string | null;
  onApply: (view: SavedView) => void;
  onSave: (name: string) => void;
  onDelete: (id: string) => void;
}

/**
 * "Views" menu: apply a saved search, save the current one, delete old ones.
 * Views are kept in this browser (see CandidatesPage). With user accounts they would move to the backend.
 */
export function SavedViews({ views, activeViewId, onApply, onSave, onDelete }: Props) {
  const { modal } = App.useApp();
  const [saving, setSaving] = useState(false);
  const [name, setName] = useState('');
  const active = views.find((v) => v.id === activeViewId);

  const items: MenuProps['items'] = [
    ...(views.length === 0
      ? [{ key: 'none', disabled: true, label: <Typography.Text type="secondary">No saved views yet</Typography.Text> }]
      : views.map((v) => ({
          key: v.id,
          icon: <StarOutlined />,
          label: (
            <Flex justify="space-between" align="center" gap={16}>
              <span>{v.name}</span>
              <DeleteOutlined
                aria-label={`Delete view ${v.name}`}
                onClick={(e) => {
                  e.stopPropagation();
                  modal.confirm({
                    title: `Delete the view "${v.name}"?`,
                    okText: 'Delete',
                    okButtonProps: { danger: true },
                    onOk: () => onDelete(v.id),
                  });
                }}
              />
            </Flex>
          ),
          onClick: () => onApply(v),
        }))),
    { type: 'divider' },
    {
      key: 'save',
      icon: <SaveOutlined />,
      label: 'Save current view…',
      onClick: () => {
        setName('');
        setSaving(true);
      },
    },
  ];

  const save = () => {
    if (!name.trim()) return;
    onSave(name.trim());
    setSaving(false);
  };

  return (
    <>
      <Dropdown menu={{ items, selectedKeys: active ? [active.id] : [] }} trigger={['click']}>
        <Button icon={<StarOutlined />}>
          {active ? active.name : 'Views'} <DownOutlined style={{ fontSize: 10 }} />
        </Button>
      </Dropdown>
      <Modal
        title="Save current view"
        open={saving}
        onOk={save}
        okText="Save"
        okButtonProps={{ disabled: !name.trim() }}
        onCancel={() => setSaving(false)}
        destroyOnHidden
      >
        <Typography.Paragraph type="secondary">
          Saves the current search text, filters and sort order so you can reapply them in one click.
        </Typography.Paragraph>
        <Input
          autoFocus
          maxLength={60}
          placeholder="e.g. Java devs in Bengaluru"
          value={name}
          onChange={(e) => setName(e.target.value)}
          onPressEnter={save}
        />
      </Modal>
    </>
  );
}
