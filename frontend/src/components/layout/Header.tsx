import { useState, useEffect } from 'react';
import { Layout, Button, Avatar, Dropdown, Typography, Space, Badge } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined,
  MoonOutlined,
  SunOutlined,
  BellOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../../context/AuthContext';
import { useThemeMode } from '../../context/ThemeContext';
import { LAYOUT } from '../../styles/theme';
import api from '../../services/api';
import SuccessModal from '../common/SuccessModal';
import NotificationDrawer from '../notifications/NotificationDrawer';

const { Header: AntHeader } = Layout;
const { Text } = Typography;

export default function Header({ collapsed, onToggle }: { collapsed: boolean; onToggle: () => void }) {
  const { user, logout, isAuthenticated } = useAuth();
  const { mode, toggleTheme } = useThemeMode();
  const navigate = useNavigate();
  const [goodbyeOpen, setGoodbyeOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const [pacienteId, setPacienteId] = useState<number | undefined>(undefined);

  const isPaciente = user?.roles?.includes('PACIENTE');
  const isStaff = user?.roles?.some(r => ['ADMIN', 'ATENCION_CLIENTE'].includes(r));

  useEffect(() => {
    if (isPaciente) {
      api.get('/auth/profile').then(r => setPacienteId(r.data.pacienteId)).catch(() => {});
    }
  }, [isPaciente]);

  const { data: notifBadge } = useQuery({
    queryKey: ['notif-badge', pacienteId],
    queryFn: async () => {
      if (pacienteId) {
        const r = await api.get(`/notificaciones/paciente/${pacienteId}`);
        return (r.data as any[])?.filter((n: any) => !n.leida).length || 0;
      }
      return 0;
    },
    enabled: !!pacienteId && notifOpen === false,
    refetchInterval: 60000,
  });

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
  const profileAvatar = user?.avatar || localStorage.getItem('profile_avatar') || '';

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

      <Space size="small" className="header-actions">
        <Button
          type="text"
          aria-label={mode === 'dark' ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}
          icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
          onClick={toggleTheme}
          className="theme-icon-button"
          style={{ fontSize: 16, width: 40, height: 40 }}
        />

        {isPaciente && (
          <Button
            type="text"
            icon={<Badge count={notifBadge || 0} showZero={false} size="small" offset={[2, -2]}><BellOutlined style={{ fontSize: 16 }} /></Badge>}
            onClick={() => setNotifOpen(true)}
            className="theme-icon-button"
            style={{ width: 40, height: 40 }}
          />
        )}

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
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, padding: '2px 0' }}>
              <Avatar
                src={profileAvatar || undefined}
                style={{
                  background: profileAvatar ? 'transparent' : 'linear-gradient(135deg, #00D4AA, #059669)',
                  color: '#fff',
                  fontWeight: 700,
                  cursor: 'pointer',
                  boxShadow: '0 10px 22px rgba(14,165,164,0.24)',
                }}
                size={32}
              >
                {profileAvatar ? null : initials}
              </Avatar>
              <div className="header-user-info">
                <Text style={{ color: 'var(--text-primary)', fontSize: 13, fontWeight: 600, lineHeight: 1.2, maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'inline-block' }}>{displayName}</Text>
                <Text style={{ color: 'var(--text-muted)', fontSize: 11, lineHeight: 1, display: 'block' }}>{user.roles?.[0] || 'Usuario'}</Text>
              </div>
            </div>
          </Dropdown>
        )}
      </Space>

      {isPaciente && (
        <NotificationDrawer
          open={notifOpen}
          onClose={() => setNotifOpen(false)}
          pacienteId={pacienteId}
        />
      )}
    </AntHeader>
  );
}
