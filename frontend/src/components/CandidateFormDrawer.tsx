import { Alert, App, Button, Col, Drawer, Form, Input, InputNumber, Row, Space, Switch } from 'antd';
import { useEffect, useState } from 'react';
import { createCandidate, getCandidate, updateCandidate } from '../api/candidates';
import { ApiError } from '../api/client';
import type { Candidate, CandidateRequest } from '../types';
import { SkillSelect } from './SkillSelect';

interface Props {
  open: boolean;
  /** null = "Add candidate"; a candidate = "Edit candidate" */
  candidate: Candidate | null;
  onClose: () => void;
  onSaved: (saved: Candidate) => void;
}

type FormValues = Omit<CandidateRequest, 'version'>;

/** Turns a loaded candidate into form values. */
function toFormValues(c: Candidate | null): FormValues {
  if (!c) return { firstName: '', lastName: '', email: '', openToWork: true, skills: [] };
  return {
    firstName: c.firstName,
    lastName: c.lastName,
    email: c.email,
    phone: c.phone,
    currentTitle: c.currentTitle,
    currentCompany: c.currentCompany,
    city: c.city,
    country: c.country,
    yearsExperience: c.yearsExperience,
    noticePeriodDays: c.noticePeriodDays,
    openToWork: c.openToWork,
    skills: c.skills,
  };
}

/** Empty text boxes become null, so optional fields are stored as "not set". */
function clean(values: FormValues): FormValues {
  const out: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(values)) {
    out[key] = typeof value === 'string' && value.trim() === '' ? null : value;
  }
  return out as unknown as FormValues;
}

/** One form for both "Add" and "Edit". */
export function CandidateFormDrawer({ open, candidate, onClose, onSaved }: Props) {
  const { message } = App.useApp();
  const [form] = Form.useForm<FormValues>();
  const [saving, setSaving] = useState(false);
  const [conflict, setConflict] = useState(false); // someone else saved first (409 version)
  // The version we are editing. Kept separately so "reload latest" can refresh it.
  const [version, setVersion] = useState<number | undefined>(candidate?.version);

  const isEdit = candidate !== null;

  // Fill the form every time the drawer opens.
  useEffect(() => {
    if (open) {
      form.resetFields(); // clear anything left from the last time the form was used
      form.setFieldsValue(toFormValues(candidate));
      setVersion(candidate?.version);
      setConflict(false);
    }
  }, [open, candidate, form]);

  const submit = async (values: FormValues) => {
    setSaving(true);
    setConflict(false);
    try {
      const body: CandidateRequest = { ...clean(values), skills: values.skills ?? [] };
      const saved = isEdit
        ? await updateCandidate(candidate.id, { ...body, version })
        : await createCandidate(body);
      message.success(isEdit ? 'Candidate updated' : `Candidate ${saved.candidateCode} created`);
      onSaved(saved);
    } catch (e) {
      if (!(e instanceof ApiError)) {
        message.error('Something went wrong');
      } else if (e.status === 400 && Object.keys(e.fields).length > 0) {
        // Show each backend validation message under its field.
        form.setFields(
          Object.entries(e.fields).map(([name, msg]) => ({ name: name as keyof FormValues, errors: [msg] })),
        );
      } else if (e.status === 409 && e.message.toLowerCase().includes('email')) {
        form.setFields([{ name: 'email', errors: [e.message] }]);
      } else if (e.status === 409) {
        setConflict(true);
      } else {
        message.error(e.message);
      }
    } finally {
      setSaving(false);
    }
  };

  /** After a version conflict: load the latest data. The user's unsaved edits are replaced. */
  const reloadLatest = async () => {
    if (!candidate) return;
    try {
      const latest = await getCandidate(candidate.id);
      form.setFieldsValue(toFormValues(latest));
      setVersion(latest.version);
      setConflict(false);
      message.info('Loaded the latest version. Re-apply your changes and save.');
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  return (
    <Drawer
      open={open}
      onClose={onClose}
      size={640}
      destroyOnHidden
      title={isEdit ? `Edit ${candidate.firstName} ${candidate.lastName}` : 'Add candidate'}
      extra={
        <Space>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="primary" loading={saving} onClick={() => form.submit()}>
            {isEdit ? 'Save changes' : 'Create candidate'}
          </Button>
        </Space>
      }
    >
      {conflict && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          title="Someone else edited this candidate after you opened it."
          description="Load the latest version, then re-apply your changes."
          action={
            <Button size="small" onClick={reloadLatest}>
              Load latest
            </Button>
          }
        />
      )}

      <Form<FormValues> form={form} layout="vertical" onFinish={submit}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="firstName"
              label="First name"
              rules={[{ required: true, whitespace: true, message: 'First name is required' }, { max: 100 }]}
            >
              <Input />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="lastName"
              label="Last name"
              rules={[{ required: true, whitespace: true, message: 'Last name is required' }, { max: 100 }]}
            >
              <Input />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="email"
              label="Email"
              rules={[
                { required: true, message: 'Email is required' },
                { type: 'email', message: 'Enter a valid email' },
                { max: 254 },
              ]}
            >
              <Input />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="phone"
              label="Phone"
              rules={[
                { max: 20 },
                { pattern: /^\+?[0-9 ()-]{7,20}$/, message: 'Digits, spaces, +, -, ( ) only' },
              ]}
            >
              <Input placeholder="+91 98765 43210" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="currentTitle" label="Current title" rules={[{ max: 150 }]}>
              <Input />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="currentCompany" label="Current company" rules={[{ max: 150 }]}>
              <Input />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="city" label="City" rules={[{ max: 100 }]}>
              <Input />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="country" label="Country" rules={[{ max: 100 }]}>
              <Input />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="yearsExperience" label="Experience (yrs)">
              <InputNumber min={0} max={60} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="noticePeriodDays" label="Notice (days)">
              <InputNumber min={0} max={365} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="openToWork" label="Open to work" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item
              name="skills"
              label="Skills"
              extra="Pick existing skills or type new ones (press Enter or comma)."
              rules={[{ type: 'array', max: 30, message: 'At most 30 skills' }]}
            >
              <SkillSelect allowNew placeholder="e.g. java, spring, mysql" />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Drawer>
  );
}
