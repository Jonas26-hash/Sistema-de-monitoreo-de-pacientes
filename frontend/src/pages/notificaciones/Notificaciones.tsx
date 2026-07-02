import { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, Select, Tag, Space, Input as SearchInput, Typography, message } from 'antd';
import { SearchOutlined, BellOutlined, SendOutlined, UserOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import type { Notificacion, Paciente } from '../../types';
import PacienteSearchByDni from '../../components/paciente/PacienteSearchByDni';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function Notificaciones() {
  const { user } = useAuth();
  const isPaciente = user?.roles?.includes('PACIENTE');
  const [form] = Form.useForm();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedPacienteId, setSelectedPacienteId] = useState<number | null>(null);
  const [pacienteId, setPacienteId] = useState<number | null>(null);

  useEffect(() => {
    if (!isPaciente) return;
    api.get('/auth/profile').then((res) => {
      setPacienteId(res.data.pacienteId || null);
    }).catch(() => {});
  }, [isPaciente]);

  const { data: pacientes } = useQuery({
    queryKey: ['pacientes-lista'],
    queryFn: async () => { const r = await api.get('/pacientes'); return r.data as Paciente[]; },
    enabled: !isPaciente,
  });
  const pacienteMap = new Map(pacientes?.map(p => [p.id, p]) || []);

  const { data, isLoading } = useQuery({
    queryKey: isPaciente ? ['mis-notificaciones', pacienteId] : ['notificaciones', page, search],
    queryFn: async () => {
      if (isPaciente && pacienteId) {
        const res = await api.get(`/notificaciones/paciente/${pacienteId}`);
        return res.data;
      }
      const params: Record<string, unknown> = { page, size: 10 };
      if (search) params.search = search;
      const res = await api.get('/notificaciones', { params });
      return res.data;
    },
    enabled: isPaciente ? !!pacienteId : true,
  });

  const sendMutation = useMutation({
    mutationFn: (values: Partial<Notificacion>) => api.post('/notificaciones', values),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['notificaciones'] }); message.success('Notificación enviada'); setModalOpen(false); setSelectedPacienteId(null); form.resetFields(); },
    onError: () => message.error('Error al enviar notificación'),
  });

  const handlePatientSelect = (p: Paciente) => {
    setSelectedPacienteId(p.id);
    form.setFieldValue('pacienteId', p.id);
  };

  const rows = Array.isArray(data) ? data : (data?.content || []);
  const total = Array.isArray(data) ? data.length : (data?.totalElements || 0);

  const columns = [
    ...(!isPaciente ? [{
      title: 'Paciente', key: 'pacienteId', responsive: ['md'] as ('md' | 'sm' | 'lg' | 'xl' | 'xxl')[], render: (v: unknown, r: Notificacion) => { const p = pacienteMap.get(r.pacienteId); return p ? <Space><UserOutlined style={{ color: '#00D4AA' }} /><Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{p.nombres} {p.apellidoPaterno}</Text><Tag style={{ borderRadius: 4, fontSize: 11 }}>{p.dni}</Tag></Space> : <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>#{r.pacienteId}</Text>; }
    }] : []),
    { title: 'Mensaje', dataIndex: 'mensaje', key: 'mensaje', ellipsis: true, render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Tipo', dataIndex: 'tipo', key: 'tipo', render: (v: string) => <Tag color={v === 'EMAIL' ? '#3B82F6' : '#8B5CF6'} style={{ borderRadius: 4 }}>{v}</Tag> },
    {
      title: 'Estado', dataIndex: 'enviada', key: 'enviada',
      render: (v: boolean) => <Tag color={v ? '#00D4AA' : '#F59E0B'} style={{ borderRadius: 4 }}>{v ? 'Enviada' : 'Pendiente'}</Tag>,
    },
    { title: 'Fecha', dataIndex: 'fechaEnvio', key: 'fechaEnvio', responsive: ['sm'] as ('md' | 'sm' | 'lg' | 'xl' | 'xxl')[], render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '-'}</Text> },
  ];

  return (
    <div>
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(59,130,246,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#3B82F6', fontSize: 20 }}>
              <BellOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Notificaciones</Title>
              <Text style={{ color: 'var(--text-muted)' }}>{isPaciente ? 'Tus notificaciones' : 'Envío y registro de notificaciones'}</Text>
            </div>
          </Space>
        </div>
        {!isPaciente && (
          <Space>
            <SearchInput.Search placeholder="Buscar por destinatario o mensaje" allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
            <Button type="primary" icon={<SendOutlined />} onClick={() => setModalOpen(true)}>Enviar Notificación</Button>
          </Space>
        )}
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'auto' }}>
        <Table columns={columns} dataSource={rows} rowKey="id" loading={isLoading} scroll={{ x: 500 }}
          pagination={{ current: page + 1, total, onChange: (p) => setPage(p - 1), showSizeChanger: false, size: 'small' }} />
      </div>

      {!isPaciente && (
        <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Enviar Notificación</Text>}
          open={modalOpen} onCancel={() => setModalOpen(false)} onOk={() => form.submit()} okText="Enviar" width={640} destroyOnClose
          styles={{ body: { padding: '24px 28px' } }}>
          <Form form={form} layout="vertical" onFinish={(v) => sendMutation.mutate(v)} preserve={false}
            style={{ width: '100%' }}>
            <div style={{ display: 'flex', gap: 16 }}>
              <Form.Item name="pacienteId" label="Paciente" rules={[{ required: true }]} style={{ width: '50%' }}>
                <PacienteSearchByDni pacientes={pacientes || []} onSelect={handlePatientSelect} value={selectedPacienteId} onChange={(v) => setSelectedPacienteId(v)} />
              </Form.Item>
              <Form.Item name="tipo" label="Tipo" rules={[{ required: true }]} initialValue="EMAIL" style={{ width: '50%' }}>
                <Select>
                  <Select.Option value="EMAIL">Email</Select.Option>
                  <Select.Option value="SMS">SMS</Select.Option>
                  <Select.Option value="PUSH">Push</Select.Option>
                </Select>
              </Form.Item>
            </div>
            <Form.Item name="mensaje" label="Mensaje" rules={[{ required: true }]}>
              <Input.TextArea rows={4} placeholder="Contenido del mensaje" />
            </Form.Item>
            <Form.Item name="canal" label="Canal">
              <Input placeholder="WhatsApp / Telegram / etc" style={{ width: '100%' }} />
            </Form.Item>
          </Form>
        </Modal>
      )}
    </div>
  );
}
