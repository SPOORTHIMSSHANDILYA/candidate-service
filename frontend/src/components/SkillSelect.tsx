import { Select } from 'antd';
import { useEffect, useState } from 'react';
import { findSkills } from '../api/candidates';
import { useDebouncedValue } from '../hooks/useDebouncedValue';

interface Props {
  value?: string[];
  onChange?: (value: string[]) => void;
  /** true in the add/edit form: the user may type new skills that don't exist yet */
  allowNew?: boolean;
  placeholder?: string;
}

/**
 * Multi-select for skills. Suggestions come from GET /skills?prefix=...
 * as the user types. Works inside an antd <Form.Item> (value/onChange).
 */
export function SkillSelect({ value, onChange, allowNew = false, placeholder }: Props) {
  const [typed, setTyped] = useState('');
  const [options, setOptions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const debouncedTyped = useDebouncedValue(typed, 250);

  useEffect(() => {
    let cancelled = false; // ignore answers that arrive after a newer request
    setLoading(true);
    findSkills(debouncedTyped)
      .then((skills) => {
        if (!cancelled) setOptions(skills);
      })
      .catch(() => {
        if (!cancelled) setOptions([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [debouncedTyped]);

  return (
    <Select
      mode={allowNew ? 'tags' : 'multiple'}
      value={value}
      onChange={onChange}
      placeholder={placeholder ?? 'Skills'}
      options={options.map((s) => ({ value: s, label: s }))}
      onSearch={setTyped}
      filterOption={false} // the server already filters by prefix
      loading={loading}
      tokenSeparators={[',']}
      allowClear
      style={{ width: '100%' }}
      maxTagCount="responsive"
    />
  );
}
