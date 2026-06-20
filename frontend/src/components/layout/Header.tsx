import { useState } from 'react';
import { Layout, Button, Avatar, Dropdown, Typography, Space } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined,
  MoonOutlined,
  SunOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useThemeMode } from '../../context/ThemeContext';
import { LAYOUT } from '../../styles/theme';
import SuccessModal from '../common/SuccessModal';

const { Header: AntHeader } = Layout;
const { Text } = Typography;

export default function Header({ collapsed, onToggle }: { collapsed: boolean; onToggle: () => void }) {
  const { user, logout, isAuthenticated } = useAuth();
  const { mode, toggleTheme } = useThemeMode();
  const navigate = useNavigate();
  const [goodbyeOpen, setGoodbyeOpen] = useState(false);

  const handleLogout = () => {
    setGoodbyeOpen(true);
    setTimeout(() => {
      setGoodbyeOpen(false);
      logout();
      navigate('/login');
    }, 2000);
  };

  const displayName = localStorage.getItem('user_display_name') || (user?.nombres && user?.apellidos ? `${user.nombres} ${user.apellidos}`.trim() : '') || user?.username || '';
  const initials = (user?.nombres?.charAt(0) || user?.username?.charAt(0) || '?').toUpperCase();

  return (
    <AntHeader className="app-header" style={{ height: LAYOUT.headerHeight }}>
      <SuccessModal
        open={goodbyeOpen}
        title="¡Hasta pronto!"
        subtitle={`${displayName || 'Usuario'}, has cerrado sesión correctamente`}
        lottieSrc="/lottie/Despedida.svg"
      />
      <Space>
        <Button
          type="text"
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={onToggle}
          className="theme-icon-button"
          style={{ fontSize: 18, width: 40, height: 40 }}
        />
      </Space>

      <Space size="middle">
        <Button
          type="text"
          aria-label={mode === 'dark' ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}
          icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
          onClick={toggleTheme}
          className="theme-icon-button"
          style={{ fontSize: 16, width: 40, height: 40 }}
        />

        {isAuthenticated && user && (
          <Dropdown
            menu={{
              items: [
                { key: 'profile', icon: <UserOutlined />, label: displayName, disabled: true },
                { key: 'mi-perfil', icon: <UserOutlined />, label: 'Mi Perfil', onClick: () => navigate('/perfil') },
                { type: 'divider' },
                { key: 'logout', icon: <LogoutOutlined />, label: 'Cerrar Sesión', danger: true, onClick: handleLogout },
              ],
            }}
            placement="bottomRight"
          >
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Avatar
                style={{
                  background: 'linear-gradient(135deg, #00D4AA, #059669)',
                  color: '#fff',
                  fontWeight: 700,
                  cursor: 'pointer',
                  boxShadow: '0 10px 22px rgba(14,165,164,0.24)',
                }}
                size={36}
              >
                {initials}
              </Avatar>
              <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.2 }}>
                <Text style={{ color: 'var(--text-primary)', fontSize: 13, fontWeight: 600 }}>{displayName}</Text>
                <Text style={{ color: 'var(--text-muted)', fontSize: 11 }}>{user.roles?.[0] || 'Usuario'}</Text>
              </div>
            </div>
          </Dropdown>
        )}
      </Space>
    </AntHeader>
  );
}
