import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Row, Col, Card, Typography, Table, Tag, Space } from 'antd';
import {
  TeamOutlined,
  CalendarOutlined,
  MedicineBoxOutlined,
  DollarOutlined,
  RiseOutlined,
  UserOutlined,
  DashboardOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import DataLoading from '../components/common/DataLoading';

const { Title, Text } = Typography;

const COLORS = ['#00D4AA', '#F59E0B', '#3B82F6', '#EF4444', '#8B5CF6'];

const barData = [
  { mes: 'Ene', citas: 45, consultas: 38 },
  { mes: 'Feb', citas: 52, consultas: 42 },
  { mes: 'Mar', citas: 48, consultas: 40 },
  { mes: 'Abr', citas: 61, consultas: 53 },
  { mes: 'May', citas: 55, consultas: 47 },
  { mes: 'Jun', citas: 67, consultas: 58 },
];

const pieData = [
  { name: 'Programadas', value: 35 },
  { name: 'Confirmadas', value: 25 },
  { name: 'En Curso', value: 15 },
  { name: 'Completadas', value: 20 },
  { name: 'Canceladas', value: 5 },
];

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({ pacientes: 0, citasHoy: 0, recetas: 0, ingresos: 0 });
  const [recentActivity, setRecentActivity] = useState<any[]>([]);
  const [activityLoading, setActivityLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get('/pacientes?size=1').catch(() => ({ data: { totalElements: 0 } })),
      api.get('/citas?size=1').catch(() => ({ data: { totalElements: 0 } })),
      api.get('/recetas?size=1').catch(() => ({ data: { totalElements: 0 } })),
      api.get('/cobros?size=1').catch(() => ({ data: { totalElements: 0 } })),
      api.get('/audit?size=10').catch(() => ({ data: { content: [] } })),
    ]).then(([p, c, r, co, auditRes]) => {
      setStats({
        pacientes: p.data.totalElements || 0,
        citasHoy: c.data.totalElements || 0,
        recetas: r.data.totalElements || 0,
        ingresos: co.data.totalElements || 0,
      });

      const auditRecords = auditRes.data?.items || auditRes.data?.content || [];
      const activity = auditRecords.map((log: any, i: number) => {
        const methodColors: Record<string, string> = {
          GET: 'blue', POST: 'green', PUT: 'orange', DELETE: 'red',
        };
        return {
          key: `a-${i}`,
          action: `${log.accion || '?'} ${log.recurso || '?'} — ${log.statusCode || ''}`,
          user: log.username || 'Sistema',
          time: formatTimeAgo(log.createdAt),
          type: methodColors[log.accion] || 'default',
        };
      });

      setRecentActivity(activity.slice(0, 5));
      setActivityLoading(false);
    });
  }, []);

  function formatTimeAgo(dateStr: string): string {
    if (!dateStr) return 'Recientemente';
    const now = new Date();
    const date = new Date(dateStr);
    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return 'Ahora';
    if (diffMin < 60) return `Hace ${diffMin} min`;
    const diffHrs = Math.floor(diffMin / 60);
    if (diffHrs < 24) return `Hace ${diffHrs}h`;
    return `Hace ${Math.floor(diffHrs / 24)}d`;
  }

  return (
    <div>
      <div style={{ marginBottom: 28, display: 'flex', alignItems: 'center', gap: 16 }}>
        <div style={{ width: 48, height: 48, borderRadius: 12, background: 'rgba(20,214,176,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24, color: '#14D6B0' }}>
          <DashboardOutlined />
        </div>
        <div>
          <Title level={3} style={{ color: 'var(--text-primary)', margin: 0, fontWeight: 700 }}>
            Dashboard {user?.username ? `— ${user.username}` : ''}
          </Title>
          <Text style={{ color: 'var(--text-muted)', fontSize: 14 }}>Resumen del sistema de monitoreo pacientes</Text>
        </div>
      </div>

      <Row gutter={[20, 20]} style={{ marginBottom: 24 }}>
        {[
          { title: 'Pacientes Registrados', value: stats.pacientes, icon: <TeamOutlined />, color: '#00D4AA', bg: 'rgba(0,212,170,0.1)' },
          { title: 'Citas Hoy', value: stats.citasHoy, icon: <CalendarOutlined />, color: '#3B82F6', bg: 'rgba(59,130,246,0.1)' },
          { title: 'Recetas Activas', value: stats.recetas, icon: <MedicineBoxOutlined />, color: '#F59E0B', bg: 'rgba(245,158,11,0.1)' },
          { title: 'Ingresos del Mes', value: `$${stats.ingresos}`, icon: <DollarOutlined />, color: '#8B5CF6', bg: 'rgba(139,92,246,0.1)' },
        ].map((card, i) => (
          <Col xs={24} sm={12} lg={6} key={i}>
            <Card className="stat-card glass" style={{ borderRadius: 16, animationDelay: `${i * 0.1}s`, opacity: 0, animation: `slideUp 0.5s ease-out ${i * 0.1}s forwards` }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <div style={{
                  width: 48, height: 48, borderRadius: 12, background: card.bg,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 22, color: card.color,
                }}>
                  {card.icon}
                </div>
                <div>
                  <Text style={{ color: 'var(--text-secondary)', fontSize: 13, display: 'block' }}>{card.title}</Text>
                  <Text style={{ color: 'var(--text-primary)', fontSize: 28, fontWeight: 700, lineHeight: 1.2 }}>{card.value}</Text>
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[20, 20]}>
        <Col xs={24} lg={16}>
          <Card className="glass" style={{ borderRadius: 16 }} title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Citas & Consultas Mensuales</Text>}>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={barData} barGap={8}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                <XAxis dataKey="mes" stroke="var(--text-muted)" tick={{ fontSize: 12 }} />
                <YAxis stroke="var(--text-muted)" tick={{ fontSize: 12 }} />
                <Tooltip
                  contentStyle={{ background: 'var(--bg-elevated)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: 'var(--text-primary)' }}
                  labelStyle={{ color: 'var(--text-secondary)' }}
                />
                <Bar dataKey="citas" fill="#00D4AA" radius={[4, 4, 0, 0]} name="Citas" />
                <Bar dataKey="consultas" fill="#3B82F6" radius={[4, 4, 0, 0]} name="Consultas" />
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card className="glass" style={{ borderRadius: 16 }} title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Estado de Citas</Text>}>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={pieData} cx="50%" cy="50%" innerRadius={60} outerRadius={90} paddingAngle={4} dataKey="value">
                  {pieData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip
                  contentStyle={{ background: 'var(--bg-elevated)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: 'var(--text-primary)' }}
                />
              </PieChart>
            </ResponsiveContainer>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px 16px', justifyContent: 'center', marginTop: 8 }}>
              {pieData.map((item, i) => (
                <Space key={i} size={6}>
                  <div style={{ width: 10, height: 10, borderRadius: '50%', background: COLORS[i] }} />
                  <Text style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{item.name}</Text>
                </Space>
              ))}
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[20, 20]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={16}>
          <Card className="glass" style={{ borderRadius: 16 }}
            title={
              <Space align="center">
                <span className="live-pulse-dot" style={{ margin: '0 4px 0 2px' }} />
                <Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Actividad Reciente</Text>
              </Space>
            }
          >
            <Table
              dataSource={activityLoading ? [] : recentActivity}
              pagination={false}
              showHeader={false}
              locale={{ emptyText: activityLoading ? <DataLoading height={120} /> : 'Sin actividad reciente' }}
              columns={[
                { dataIndex: 'action', key: 'action', render: (v, r) => (
                  <Space>
                    <Tag color={r.type} style={{ borderRadius: 4, fontSize: 11 }}>{r.user}</Tag>
                    <Text style={{ color: 'var(--text-primary)', fontSize: 13 }}>{v}</Text>
                  </Space>
                )},
                { dataIndex: 'time', key: 'time', width: 80, render: (v) => <Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{v}</Text> },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card className="glass" style={{ borderRadius: 16 }}
            title={
              <Space>
                <span style={{ color: '#F59E0B', fontSize: 16 }}><ThunderboltOutlined /></span>
                <Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Acceso Rápido</Text>
              </Space>
            }
          >
            <Space direction="vertical" style={{ width: '100%' }} size={12}>
              {[
                { label: 'Registrar Paciente', icon: <UserOutlined />, link: '/pacientes' },
                { label: 'Nueva Cita', icon: <CalendarOutlined />, link: '/citas' },
                { label: 'Ver Reportes', icon: <RiseOutlined />, link: '/auditoria' },
              ].map((item) => (
                <Card
                  key={item.label}
                  hoverable
                  style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-soft)', borderRadius: 10 }}
                  onClick={() => navigate(item.link)}
                  styles={{ body: { padding: '14px 18px' } }}
                >
                  <Space>
                    <div style={{ width: 36, height: 36, borderRadius: 8, background: 'rgba(0,212,170,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#00D4AA' }}>
                      {item.icon}
                    </div>
                    <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{item.label}</Text>
                  </Space>
                </Card>
              ))}
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

