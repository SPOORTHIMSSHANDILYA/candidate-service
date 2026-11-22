import { DashboardOutlined, LogoutOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons';
import { App as AntApp, Button, ConfigProvider, Flex, Layout, Menu, Spin, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { logout, restoreSession, type CurrentUser } from './api/auth';
import { hasToken, setUnauthorizedHandler } from './api/client';
import { LoginPage } from './pages/LoginPage';
import { CandidatesPage } from './pages/CandidatesPage';
import { OverviewPage } from './pages/OverviewPage';
import type { CandidateStatus } from './types';

const { Header, Sider, Content } = Layout;

type Page = 'overview' | 'candidates';

/** The page comes from the address: /overview or / (candidates). */
const pageFromUrl = (): Page => (window.location.pathname.startsWith('/overview') ? 'overview' : 'candidates');

/** App shell: theme, sign-in, top bar, side menu and the current page. */
export default function App() {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loginNotice, setLoginNotice] = useState<string | null>(null);
  // A token stored before a page refresh is checked with the backend before showing anything.
  const [checkingSession, setCheckingSession] = useState(hasToken);

  useEffect(() => {
    if (!checkingSession) return;
    restoreSession()
      .then(setUser)
      .finally(() => setCheckingSession(false));
  }, [checkingSession]);
  const [page, setPage] = useState<Page>(pageFromUrl);
  // Bumped on every navigation so the candidates page re-reads the address (e.g. ?status=INACTIVE).
  const [visit, setVisit] = useState(0);

  // Browser back / forward buttons
  useEffect(() => {
    const onPop = () => {
      setPage(pageFromUrl());
      setVisit((v) => v + 1);
    };
    window.addEventListener('popstate', onPop);
    return () => window.removeEventListener('popstate', onPop);
  }, []);

  const go = (next: Page, query = '') => {
    window.history.pushState(null, '', (next === 'overview' ? '/overview' : '/') + query);
    setPage(next);
    setVisit((v) => v + 1);
  };

  const openStatus = (status: CandidateStatus) => go('candidates', `?status=${status}`);

  // If the backend answers 401 later (e.g. password changed), go back to the login page.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null);
      setLoginNotice('Your session has ended. Please sign in again.');
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  const signOut = () => {
    logout();
    setUser(null);
    setLoginNotice('You have signed out.');
  };

  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#1f4e79', // calm navy: a sober, "executive" look
          borderRadius: 6,
          fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
        },
      }}
    >
      {/* AntApp lets any component show toast messages via App.useApp() */}
      <AntApp>
        {checkingSession ? (
          <Flex justify="center" align="center" style={{ minHeight: '100vh' }}>
            <Spin size="large" />
          </Flex>
        ) : !user ? (
          <LoginPage
            notice={loginNotice}
            onSignedIn={(u) => {
              setUser(u);
              setLoginNotice(null);
              setVisit((v) => v + 1);
            }}
          />
        ) : (
          <Layout style={{ minHeight: '100vh' }}>
            <Header
              style={{
                display: 'flex',
                alignItems: 'center',
                background: '#1f4e79',
                paddingInline: 24,
              }}
            >
              <Typography.Title level={4} style={{ color: '#fff', margin: 0, flex: 1 }}>
                Candidate CRM
              </Typography.Title>
              <Flex align="center" gap={12}>
                <Typography.Text style={{ color: '#fff' }} title={user.username}>
                  <UserOutlined /> {user.displayName}
                </Typography.Text>
                <Button size="small" ghost icon={<LogoutOutlined />} onClick={signOut}>
                  Sign out
                </Button>
              </Flex>
            </Header>
            <Layout>
              <Sider width={200} breakpoint="lg" collapsedWidth={0} theme="light">
                <Menu
                  mode="inline"
                  selectedKeys={[page]}
                  onClick={({ key }) => go(key as Page)}
                  style={{ height: '100%', paddingTop: 8 }}
                  items={[
                    {
                      key: 'overview',
                      icon: <DashboardOutlined />,
                      label: 'Overview',
                    },
                    {
                      key: 'candidates',
                      icon: <TeamOutlined />,
                      label: 'Candidates',
                    },
                  ]}
                />
              </Sider>
              <Content style={{ padding: 24 }}>
                {page === 'overview' ? <OverviewPage onOpenStatus={openStatus} /> : <CandidatesPage key={visit} />}
              </Content>
            </Layout>
          </Layout>
        )}
      </AntApp>
    </ConfigProvider>
  );
}
