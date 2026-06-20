import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Typography } from 'antd';
import { useThemeMode } from '../../context/ThemeContext';
import {
  DashboardOutlined,
  UserOutlined,
  TeamOutlined,
  CalendarOutlined,
  FileTextOutlined,
  MedicineBoxOutlined,
  ShoppingCartOutlined,
  DollarOutlined,
  BellOutlined,
  AuditOutlined,
  SettingOutlined,
  SafetyOutlined,
  HeartOutlined,
  ExperimentOutlined,
  TagsOutlined,
  GiftOutlined,
} from '@ant-design/icons';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import { LAYOUT } from '../../styles/theme';

const { Sider } = Layout;
const { Text } = Typography;

const menuItems = [
  { key: '/', icon: <DashboardOutlined />, label: 'Dashboard', roles: ['ADMIN', 'DOCTOR', 'ATENCION_CLIENTE', 'FARMACEUTICO', 'ENFERMERO', 'PACIENTE'] },
  { key: '/usuarios', icon: <UserOutlined />, label: 'Usuarios', roles: ['ADMIN'] },
  { key: '/pacientes', icon: <TeamOutlined />, label: 'Pacientes', roles: ['ADMIN', 'DOCTOR', 'ATENCION_CLIENTE'] },
  { key: '/citas', icon: <CalendarOutlined />, label: 'Citas', roles: ['ADMIN', 'DOCTOR', 'ATENCION_CLIENTE', 'PACIENTE'] },
  { key: '/triaje', icon: <HeartOutlined />, label: 'Triaje', roles: ['ADMIN', 'ENFERMERO'] },
  { key: '/consultas', icon: <FileTextOutlined />, label: 'Consultas', roles: ['ADMIN', 'DOCTOR'] },
  { key: '/examenes', icon: <ExperimentOutlined />, label: 'Exámenes', roles: ['ADMIN', 'DOCTOR', 'ATENCION_CLIENTE'] },
  { key: '/recetas', icon: <MedicineBoxOutlined />, label: 'Recetas', roles: ['ADMIN', 'DOCTOR'] },
  { key: '/medicamentos', icon: <ShoppingCartOutlined />, label: 'Medicamentos', roles: ['ADMIN', 'FARMACEUTICO'] },
  { key: '/dispensaciones', icon: <MedicineBoxOutlined />, label: 'Dispensaciones', roles: ['ADMIN', 'FARMACEUTICO'] },
  { key: '/cobros', icon: <DollarOutlined />, label: 'Cobros', roles: ['ADMIN', 'ATENCION_CLIENTE'] },
  { key: '/notificaciones', icon: <BellOutlined />, label: 'Notificaciones', roles: ['ADMIN', 'ATENCION_CLIENTE', 'PACIENTE'] },
  { key: '/auditoria', icon: <AuditOutlined />, label: 'Auditoría', roles: ['ADMIN'] },
  { key: '/tarifario', icon: <TagsOutlined />, label: 'Tarifario', roles: ['ADMIN', 'ATENCION_CLIENTE'] },
  { key: '/campanias', icon: <GiftOutlined />, label: 'Campañas', roles: ['ADMIN', 'ATENCION_CLIENTE'] },
  { key: '/configuracion', icon: <SettingOutlined />, label: 'Configuración', roles: ['ADMIN'] },
];

export default function Sidebar({ collapsed, onCollapse }: { collapsed: boolean; onCollapse: (v: boolean) => void }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { hasRole } = useAuth();
  const { mode } = useThemeMode();
  const [hospitalName, setHospitalName] = useState('');

  useEffect(() => {
    api.get('/auth/config')
      .then((res) => setHospitalName(res.data.hospitalName || ''))
      .catch(() => {});
  }, []);

  const visibleItems = menuItems.filter((item) => item.roles.some((r) => hasRole(r)));

  return (
    <Sider
      width={LAYOUT.sidebarWidth}
      collapsedWidth={LAYOUT.collapsedSidebarWidth}
      collapsed={collapsed}
      onCollapse={onCollapse}
      className="app-sidebar"
    >
      <div className="app-logo" style={{ justifyContent: collapsed ? 'center' : 'flex-start', padding: collapsed ? 0 : '0 20px' }}>
        <img src="/Logo-1.png" alt="MT" className="app-logo-mark" style={{
          width: 36, height: 36, objectFit: 'contain',
          background: mode === 'dark' ? 'rgba(255,255,255,0.12)' : 'transparent',
          borderRadius: 6, padding: 2,
        }} />
        {!collapsed && (
          <div>
            <Text strong style={{ color: 'var(--text-primary)', fontSize: 16, lineHeight: 1.2, display: 'block' }}>{hospitalName || 'MedTrack'}</Text>
            <Text style={{ color: 'var(--text-muted)', fontSize: 11, lineHeight: 1, display: 'block' }}>Clínica System</Text>
          </div>
        )}
      </div>

      <Menu
        mode="inline"
        selectedKeys={[location.pathname]}
        items={visibleItems}
        onClick={({ key }) => navigate(key)}
        style={{
          background: 'transparent',
          borderRight: 0,
          padding: '0 8px',
        }}
      />
    </Sider>
  );
}

