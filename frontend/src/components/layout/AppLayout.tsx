import { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { Layout } from 'antd';
import Sidebar from './Sidebar';
import Header from './Header';
import { LAYOUT } from '../../styles/theme';

const { Content } = Layout;

function isMobile(): boolean {
  return window.innerWidth < 768;
}

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(isMobile);

  useEffect(() => {
    const onResize = () => {
      const mobile = window.innerWidth < 768;
      setCollapsed(mobile);
    };
    window.addEventListener('resize', onResize);
    onResize();
    return () => window.removeEventListener('resize', onResize);
  }, []);

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

