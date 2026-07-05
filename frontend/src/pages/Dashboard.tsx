import React, { useState, useEffect } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
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
  FileTextOutlined,
  CreditCardOutlined,
} from '@ant-design/icons';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import DataLoading from '../components/common/DataLoading';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

const COLORS = ['#00D4AA', '#F59E0B', '#3B82F6', '#EF4444', '#8B5CF6', '#14D6B0'];

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({ pacientes: 0, citasHoy: 0, consultasHoy: 0, recetas: 0, ingresos: 0, pendientes: 0 });
  const [recentActivity, setRecentActivity] = useState<any[]>([]);
  const [activityLoading, setActivityLoading] = useState(true);
  const [barData, setBarData] = useState<{ mes: string; citas: number; consultas: number }[]>([]);
  const [pieData, setPieData] = useState<{ name: string; value: number }[]>([]);
  const isPaciente = user?.roles?.includes('PACIENTE');
  const canSeeFinance = user?.roles?.includes('ADMIN') || user?.roles?.includes('ATENCION_CLIENTE');

  function friendlyAction(accion: string, recurso: string): string {
    const patterns: [RegExp, string][] = [
      [/^POST \/auth\/login/, 'Iniciaste sesión'],
      [/^GET \/auth\/config/, 'Consultaste configuración'],
      [/^POST \/citas/, 'Agregaste una cita'],
      [/^POST \/consultas/, 'Registraste una consulta'],
      [/^PUT \/auth\/profile/, 'Actualizaste tu perfil'],
      [/^POST \/pacientes/, 'Registraste un paciente'],
      [/^POST \/auth\/register/, 'Creaste un usuario'],
      [/^POST \/auth\/pre-registro-personal/, 'Registraste un empleado'],
      [/^DELETE \/auth\/usuarios/, 'Deshabilitaste un usuario'],
      [/^PUT \/auth\/pendientes\/\d+\/aprobar/, 'Reactivaste un usuario'],
      [/^POST \/triajes/, 'Registraste un triaje'],
      [/^POST \/ordenes-examen/, 'Creaste una orden de examen'],
      [/^PUT \/ordenes-examen/, 'Actualizaste resultados de examen'],
      [/^POST \/recetas/, 'Recetaste un medicamento'],
      [/^POST \/dispensaciones/, 'Dispensaste un medicamento'],
      [/^POST \/cobros\/pago-unico/, 'Realizaste un pago'],
      [/^POST \/medicamentos/, 'Agregaste un medicamento'],
    ];
    const full = `${accion} ${recurso}`;
    for (const [pattern, desc] of patterns) {
      if (pattern.test(full)) return desc;
    }
    return `${accion} ${recurso}`;
  }

  useEffect(() => {
    const hoy = new Date();
    const hoyStr = hoy.toISOString().slice(0, 10);

    const extractArray = (d: any) => {
      if (Array.isArray(d)) return d;
      if (d?.value && Array.isArray(d.value)) return d.value;
      if (d?.content && Array.isArray(d.content)) return d.content;
      return [];
    };

    Promise.all([
      api.get('/pacientes').catch(() => ({ data: [] })),
      api.get('/citas').catch(() => ({ data: [] })),
      api.get('/consultas').catch(() => ({ data: [] })),
      api.get('/recetas').catch(() => ({ data: [] })),
      api.get('/cobros').catch(() => ({ data: [] })),
      api.get(`/audit?size=10&username=${user?.username || ''}`).catch(() => ({ data: { content: [] } })),
    ]).then(([p, citas, consultas, r, co, auditRes]) => {
      const allCitas = extractArray(citas.data);
      const allConsultas = extractArray(consultas.data);
      const allCobros = extractArray(co.data);

      const citasHoy = allCitas.filter((c: any) => c.fechaHora?.startsWith?.(hoyStr)).length;
      const consultasHoy = allConsultas.filter((c: any) => c.fechaConsulta?.startsWith?.(hoyStr)).length;
      const ingresos = allCobros
        .filter((c: any) => c.estado === 'PAGADO')
        .reduce((s: number, c: any) => s + (c.monto || 0), 0);
      const pendientes = allCobros
        .filter((c: any) => c.estado === 'PENDIENTE')
        .reduce((s: number, c: any) => s + (c.monto || 0), 0);

      const allRecetas = extractArray(r.data);
      setStats({
        pacientes: extractArray(p.data).length,
        citasHoy, consultasHoy,
        recetas: allRecetas.length,
        ingresos, pendientes,
      });

      const meses = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Set','Oct','Nov','Dic'];
      const monthly: Record<number, { citas: number; consultas: number }> = {};
      allCitas.forEach((c: any) => {
        if (c.fechaHora) {
          const m = new Date(c.fechaHora).getMonth();
          if (!monthly[m]) monthly[m] = { citas: 0, consultas: 0 };
          monthly[m].citas++;
        }
      });
      allConsultas.forEach((c: any) => {
        if (c.fechaConsulta) {
          const m = new Date(c.fechaConsulta).getMonth();
          if (!monthly[m]) monthly[m] = { citas: 0, consultas: 0 };
          monthly[m].consultas++;
        }
      });
      const bar = Object.entries(monthly)
        .sort(([a], [b]) => Number(a) - Number(b))
        .map(([m, v]) => ({ mes: meses[Number(m)], ...v }));
      setBarData(bar.length ? bar : []);

      const estados = ['PROGRAMADA','CONFIRMADA','EN_CURSO','COMPLETADA','CANCELADA'];
      const estadoCount = estados.map(name => ({
        name,
        value: allCitas.filter((c: any) => c.estado === name).length,
      }));
      setPieData(estadoCount.filter(e => e.value > 0));

      const auditRecords = auditRes.data?.items || auditRes.data?.content || [];
      const activity = auditRecords.map((log: any, i: number) => ({
        key: `a-${i}`,
        action: friendlyAction(log.accion || '?', log.recurso || '?'),
        user: log.username || 'Sistema',
        time: formatTimeAgo(log.createdAt),
      }));

      setRecentActivity(activity.slice(0, 5));
      setActivityLoading(false);
    });
  }, [user]);

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

  if (isPaciente) {
    return <Navigate to="/portal" replace />;
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
        {(() => {
          const cards: { title: string; value: number | string; icon: React.ReactNode; color: string; bg: string }[] = [
            { title: 'Pacientes Registrados', value: stats.pacientes, icon: <TeamOutlined />, color: '#00D4AA', bg: 'rgba(0,212,170,0.1)' },
            { title: 'Citas Hoy', value: stats.citasHoy, icon: <CalendarOutlined />, color: '#3B82F6', bg: 'rgba(59,130,246,0.1)' },
            { title: 'Consultas Hoy', value: stats.consultasHoy, icon: <FileTextOutlined />, color: '#14D6B0', bg: 'rgba(20,214,176,0.1)' },
            { title: 'Recetas Emitidas', value: stats.recetas, icon: <MedicineBoxOutlined />, color: '#F59E0B', bg: 'rgba(245,158,11,0.1)' },
          ];
          if (canSeeFinance) {
            cards.push(
              { title: 'Ingresos Cobrados', value: `S/${stats.ingresos.toFixed(2)}`, icon: <DollarOutlined />, color: '#10B981', bg: 'rgba(16,185,129,0.1)' },
              { title: 'Pendientes de Pago', value: `S/${stats.pendientes.toFixed(2)}`, icon: <CreditCardOutlined />, color: '#EF4444', bg: 'rgba(239,68,68,0.1)' },
            );
          }
          const span = cards.length === 4 ? { xs: 12, sm: 12, lg: 6 } : { xs: 12, sm: 8, lg: 4 };
          return cards.map((card, i) => (
            <Col {...span} key={i}>
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
          ));
        })()}
      </Row>

      <Row gutter={[20, 20]}>
        <Col xs={24} lg={16}>
          <Card className="glass" style={{ borderRadius: 16 }} title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Citas & Consultas Mensuales</Text>}>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={barData.length ? barData : [{ mes: 'Sin datos', citas: 0, consultas: 0 }]} barGap={8}>
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
                <Pie data={pieData.length ? pieData : [{ name: 'Sin datos', value: 1 }]} cx="50%" cy="50%" innerRadius={60} outerRadius={90} paddingAngle={4} dataKey="value">
                  {pieData.length ? pieData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />) : <Cell fill="#eee" />}
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
                  <Text style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{item.name} ({item.value})</Text>
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
                    <Tag color="green" style={{ borderRadius: 4, fontSize: 11 }}>{r.user}</Tag>
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

