import { Table, Tag, Space, Input, DatePicker, Select, Row, Col, Typography } from 'antd';
import { SearchOutlined, AuditOutlined, FilterOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import type { AuditLog } from '../../types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

export default function Auditoria() {
  const [page, setPage] = useState(0);
  const [username, setUsername] = useState('');
  const [accion, setAccion] = useState('');
  const [dateRange, setDateRange] = useState<[string, string] | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['auditoria', page, username, accion, dateRange],
    queryFn: async () => {
      const params: Record<string, unknown> = { page, size: 15 };
      if (username) params.username = username;
      if (accion) params.accion = accion;
      if (dateRange) {
        params.fechaInicio = dateRange[0];
        params.fechaFin = dateRange[1];
      }
      const res = await api.get('/audit', { params });
      return res.data;
    },
  });

  const columns = [
    { title: 'Nº', key: 'index', width: 60, render: (_v: unknown, _r: unknown, i: number) => <Text style={{ color: 'var(--text-muted)' }}>{i + 1}</Text> },
    { title: 'Usuario', dataIndex: 'username', key: 'username', render: (v: string) => <Tag color="#3B82F6" style={{ borderRadius: 4 }}>{v}</Tag> },
    { title: 'Acción', dataIndex: 'accion', key: 'accion', render: (v: string) => <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{v}</Text> },
    { title: 'Recurso', dataIndex: 'recurso', key: 'recurso', ellipsis: true, render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v || '-'}</Text> },
    { title: 'IP', dataIndex: 'ip', key: 'ip', render: (v: string) => <Text style={{ color: 'var(--text-muted)', fontFamily: 'monospace' }}>{v || '-'}</Text> },
    { title: 'Fecha', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{dayjs(v).format('DD/MM/YYYY HH:mm:ss')}</Text> },
  ];

  return (
    <div>
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(139,92,246,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#8B5CF6', fontSize: 20 }}>
              <AuditOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Auditoría</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Registro detallado de actividades del sistema</Text>
            </div>
          </Space>
        </div>
      </div>

      <div className="glass" style={{ borderRadius: 16, padding: 20, marginBottom: 20 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={8}>
            <Input placeholder="Filtrar por usuario..." prefix={<SearchOutlined style={{ color: 'var(--text-muted)' }} />}
              value={username} onChange={(e) => { setUsername(e.target.value); setPage(0); }}
              style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-color)' }} />
          </Col>
          <Col xs={24} sm={8}>
            <Select placeholder="Filtrar por acción..." allowClear style={{ width: '100%' }}
              value={accion || undefined} onChange={(v) => { setAccion(v || ''); setPage(0); }}
              suffixIcon={<FilterOutlined style={{ color: 'var(--text-muted)' }} />}>
              <Select.Option value="LOGIN">LOGIN</Select.Option>
              <Select.Option value="LOGOUT">LOGOUT</Select.Option>
              <Select.Option value="CREATE">CREATE</Select.Option>
              <Select.Option value="UPDATE">UPDATE</Select.Option>
              <Select.Option value="DELETE">DELETE</Select.Option>
              <Select.Option value="REGISTER">REGISTER</Select.Option>
            </Select>
          </Col>
          <Col xs={24} sm={8}>
            <RangePicker
              style={{ width: '100%', background: 'var(--bg-surface)', borderColor: 'var(--border-color)' }}
              onChange={(dates) => {
                if (dates && dates[0] && dates[1]) {
                  setDateRange([dates[0].toISOString(), dates[1].toISOString()]);
                } else {
                  setDateRange(null);
                }
                setPage(0);
              }}
            />
          </Col>
        </Row>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'hidden' }}>
        <Table
          columns={columns}
          dataSource={data?.content || []}
          rowKey="id"
          loading={isLoading}
          pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false }}
        />
      </div>
    </div>
  );
}