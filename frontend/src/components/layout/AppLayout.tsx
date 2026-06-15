import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Layout } from 'antd';
import Sidebar from './Sidebar';
import Header from './Header';
import { LAYOUT } from '../../styles/theme';

const { Content } = Layout;

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <Layout className="app-shell">
      <Sidebar collapsed={collapsed} onCollapse={setCollapsed} />
      <Layout
        className="app-main"
        style={{ marginLeft: collapsed ? LAYOUT.collapsedSidebarWidth : LAYOUT.sidebarWidth }}
      >
        <Header collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />
        <Content className="app-content">
          <div className="page-enter">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}

