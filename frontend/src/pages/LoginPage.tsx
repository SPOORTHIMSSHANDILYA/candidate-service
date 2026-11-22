import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Flex, Form, Input, Typography } from 'antd';
import { useState } from 'react';
import { login, type CurrentUser } from '../api/auth';
import { ApiError } from '../api/client';

interface Props {
  onSignedIn: (user: CurrentUser) => void;
  /** Shown above the form, e.g. after the session ended. */
  notice?: string | null;
}

interface LoginForm {
  username: string;
  password: string;
}

/** Simple sign-in page: username + password, checked against GET /auth/me. */
export function LoginPage({ onSignedIn, notice }: Props) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async ({ username, password }: LoginForm) => {
    setSubmitting(true);
    setError(null);
    try {
      onSignedIn(await login(username.trim(), password));
    } catch (e) {
      // Same message for a wrong username or a wrong password: don't reveal which usernames exist.
      setError(e instanceof ApiError && e.status === 401 ? 'Invalid username or password.' : (e as Error).message);
      setSubmitting(false);
    }
  };

  return (
    <Flex justify="center" align="center" style={{ minHeight: '100vh', background: '#f4f6f9', padding: 16 }}>
      <Card style={{ width: 360 }}>
        <Flex vertical gap={20}>
          <div>
            <Typography.Title level={3} style={{ margin: 0, color: '#1f4e79' }}>
              Candidate CRM
            </Typography.Title>
            <Typography.Text type="secondary">Sign in to continue</Typography.Text>
          </div>

          {notice && !error && <Alert type="info" showIcon title={notice} />}
          {error && <Alert type="error" showIcon title={error} />}

          <Form<LoginForm> layout="vertical" onFinish={submit} requiredMark={false} disabled={submitting}>
            <Form.Item name="username" label="Username" rules={[{ required: true, message: 'Enter your username' }]}>
              <Input prefix={<UserOutlined />} autoComplete="username" autoFocus />
            </Form.Item>
            <Form.Item name="password" label="Password" rules={[{ required: true, message: 'Enter your password' }]}>
              <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={submitting}>
              Sign in
            </Button>
          </Form>
        </Flex>
      </Card>
    </Flex>
  );
}
