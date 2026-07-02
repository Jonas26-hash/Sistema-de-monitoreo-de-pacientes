import { useState, useMemo } from 'react';
import { Typography, Card, Row, Col, Spin, Empty, Tag, Table, Space } from 'antd';
import { DollarOutlined, RiseOutlined, FallOutlined, CalendarOutlined, WalletOutlined, FileTextOutlined } from '@ant-design/icons';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import type { Cobro } from '../../types';
import dayjs from 'dayjs';
import isoWeek from 'dayjs/plugin/isoWeek';

dayjs.extend(isoWeek);

const { Title, Text } = Typography;
const COLORS = ['#00D4AA', '#F59E0B', '#3B82F6', '#EF4444', '#8B5CF6'];

export default function Reportes() {
  const { user } = useAuth();
  const [periodo, setPeriodo] = useState<'semanal' | 'mensual'>('mensual');

  const { data: profile } = useQuery({
    queryKey: ['profile'],
    queryFn: async () => { const r = await api.get('/auth/profile'); return r.data; },
  });

  const pacienteId = profile?.pacienteId as number | undefined;

  const { data: cobrosRaw, isLoading, error } = useQuery({
    queryKey: ['reportes-cobros', pacienteId],
    queryFn: async () => {
      const r = await api.get(`/cobros/deudas/${pacienteId}`);
      const d = r.data;
      return Array.isArray(d?.cobros) ? d.cobros : Array.isArray(d) ? d : [];
    },
    enabled: !!pacienteId,
    retry: 1,
  });

  const cobros = (cobrosRaw || []) as Cobro[];
  const pagados = cobros.filter(c => c.estado === 'PAGADO');
  const pendientes = cobros.filter(c => c.estado === 'PENDIENTE');

  const hoy = dayjs();
  const inicioSemana = hoy.startOf('isoWeek');
  const finSemana = hoy.endOf('isoWeek');
  const inicioMes = hoy.startOf('month');
  const finMes = hoy.endOf('month');

  const gastosSemana = pagados.filter(c => {
    if (!c.fechaCobro) return false;
    const d = dayjs(c.fechaCobro);
    return d.isAfter(inicioSemana.subtract(1, 'day')) && d.isBefore(finSemana.add(1, 'day'));
  }).reduce((s, c) => s + Number(c.monto || 0), 0);

  const gastosMes = pagados.filter(c => {
    if (!c.fechaCobro) return false;
    const d = dayjs(c.fechaCobro);
    return d.isAfter(inicioMes.subtract(1, 'day')) && d.isBefore(finMes.add(1, 'day'));
  }).reduce((s, c) => s + Number(c.monto || 0), 0);

  const totalGastado = pagados.reduce((s, c) => s + Number(c.monto || 0), 0);
  const totalPendiente = pendientes.reduce((s, c) => s + Number(c.monto || 0), 0);

  const chartData = useMemo(() => {
    if (!pagados.length) return [];
    const agrupado: Record<string, number> = {};
    pagados.forEach(c => {
      if (!c.fechaCobro) return;
      const key = periodo === 'mensual'
        ? dayjs(c.fechaCobro).format('MMM')
        : `Sem ${dayjs(c.fechaCobro).isoWeek()}`;
      agrupado[key] = (agrupado[key] || 0) + Number(c.monto || 0);
    });
    const orden = periodo === 'mensual'
      ? ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Set', 'Oct', 'Nov', 'Dic']
      : Object.keys(agrupado).sort();
    return orden.filter(k => agrupado[k]).map(k => ({ periodo: k, gastos: agrupado[k] }));
  }, [pagados, periodo]);

  const pieData = [
    { name: 'Pagado', value: totalGastado },
    { name: 'Pendiente', value: totalPendiente },
  ].filter(d => d.value > 0);

  const columns = [
    { title: 'Fecha', dataIndex: 'fechaCobro', key: 'fechaCobro', render: (v: string) => v ? dayjs(v).format('DD/MM/YYYY') : '-' },
    { title: 'Descripción', dataIndex: 'descripcion', key: 'descripcion', render: (v: string) => v || '-' },
    { title: 'Tipo', dataIndex: 'tipo', key: 'tipo', render: (v: string) => <Tag>{v || '-'}</Tag> },
    { title: 'Monto', dataIndex: 'monto', key: 'monto', render: (v: number) => <Text strong style={{ color: 'var(--brand-primary)' }}>S/{Number(v || 0).toFixed(2)}</Text> },
    {
      title: 'Estado', dataIndex: 'estado', key: 'estado',
      render: (v: string) => <Tag color={v === 'PAGADO' ? 'green' : v === 'PENDIENTE' ? 'orange' : 'red'}>{v}</Tag>,
    },
  ];

  if (!pacienteId) return (
    <div style={{ textAlign: 'center', padding: 80 }}>
      <Spin size="large" />
    </div>
  );

  return (
    <div className="page-enter">
      <div className="crud-page-header">
        <div>
          <Title level={4} style={{ margin: 0 }}>Mis Gastos</Title>
          <Text style={{ color: 'var(--text-muted)' }}>Resumen de gastos y pagos</Text>
        </div>
      </div>

      {isLoading ? <Spin style={{ display: 'block', padding: 80 }} /> : error ? (
        <Empty description="No se pudieron cargar los datos de cobros. Contacta a administración (se requiere actualización del backend)." />
      ) : (
        <>
          <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
            <Col xs={12} sm={6}>
              <Card className="stat-card glass" size="small">
                <Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Total Gastado</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--brand-primary)' }}>S/{totalGastado.toFixed(2)}</div>
              </Card>
            </Col>
            <Col xs={12} sm={6}>
              <Card className="stat-card glass" size="small">
                <Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Este Mes</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: '#3B82F6' }}>S/{gastosMes.toFixed(2)}</div>
              </Card>
            </Col>
            <Col xs={12} sm={6}>
              <Card className="stat-card glass" size="small">
                <Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Esta Semana</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: '#8B5CF6' }}>S/{gastosSemana.toFixed(2)}</div>
              </Card>
            </Col>
            <Col xs={12} sm={6}>
              <Card className="stat-card glass" size="small">
                <Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Pendiente</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: totalPendiente > 0 ? '#EF4444' : 'var(--text-muted)' }}>S/{totalPendiente.toFixed(2)}</div>
              </Card>
            </Col>
          </Row>

          <Row gutter={[12, 12]}>
            <Col xs={24} lg={14}>
              <Card className="glass" title={
                <Space>
                  <CalendarOutlined />
                  <Text strong>Evolución de Gastos</Text>
                </Space>
              } size="small" extra={
                <Tag color={periodo === 'mensual' ? '#00D4AA' : 'default'} style={{ cursor: 'pointer' }} onClick={() => setPeriodo(periodo === 'mensual' ? 'semanal' : 'mensual')}>
                  {periodo === 'mensual' ? 'Mensual' : 'Semanal'}
                </Tag>
              }>
                {chartData.length ? (
                  <ResponsiveContainer width="100%" height={250}>
                    <BarChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" />
                      <XAxis dataKey="periodo" tick={{ fontSize: 11, fill: 'var(--text-muted)' }} />
                      <YAxis tick={{ fontSize: 11, fill: 'var(--text-muted)' }} />
                      <Tooltip contentStyle={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-color)', borderRadius: 8 }} />
                      <Bar dataKey="gastos" fill="#00D4AA" radius={[4, 4, 0, 0]} name="Gastos" />
                    </BarChart>
                  </ResponsiveContainer>
                ) : <Empty description="Sin datos de gastos" />}
              </Card>
            </Col>
            <Col xs={24} lg={10}>
              <Card className="glass" title={
                <Space>
                  <WalletOutlined />
                  <Text strong>Distribución</Text>
                </Space>
              } size="small">
                {pieData.length ? (
                  <ResponsiveContainer width="100%" height={250}>
                    <PieChart>
                      <Pie data={pieData} cx="50%" cy="50%" innerRadius={60} outerRadius={90} paddingAngle={4} dataKey="value">
                        {pieData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                      </Pie>
                      <Tooltip contentStyle={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-color)', borderRadius: 8 }} />
                    </PieChart>
                  </ResponsiveContainer>
                ) : <Empty description="Sin datos" />}
                <div style={{ display: 'flex', justifyContent: 'center', gap: 16, marginTop: 8 }}>
                  {pieData.map((d, i) => (
                    <Space key={d.name} size={4}>
                      <div style={{ width: 10, height: 10, borderRadius: '50%', background: COLORS[i] }} />
                      <Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{d.name}</Text>
                    </Space>
                  ))}
                </div>
              </Card>
            </Col>
          </Row>

          <Card className="glass" style={{ marginTop: 16 }} title={
            <Space><FileTextOutlined /><Text strong>Historial de Cobros</Text></Space>
          } size="small">
            <Table
              dataSource={cobros.slice().sort((a, b) => new Date(b.fechaCobro || '').getTime() - new Date(a.fechaCobro || '').getTime())}
              columns={columns}
              rowKey="id"
              size="small"
              pagination={{ pageSize: 10, size: 'small' }}
              style={{ marginTop: 8 }}
            />
          </Card>
        </>
      )}
    </div>
  );
}
