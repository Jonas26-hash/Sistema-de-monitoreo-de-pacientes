import { Table, Button, Modal, Form, Input, InputNumber, DatePicker, Space, Input as SearchInput, Typography, Tag } from 'antd';
import { PlusOutlined, SearchOutlined, FileTextOutlined, UserOutlined, MedicineBoxOutlined } from '@ant-design/icons';
import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { showCrudSuccess } from '../../utils/notifications';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import { normalizeResponse } from '../../hooks/useCrud';
import type { Consulta, Paciente, Cita, User } from '../../types';
import PacienteSearchByDni from '../../components/paciente/PacienteSearchByDni';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function Consultas() {
  const [form] = Form.useForm();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedPacienteId, setSelectedPacienteId] = useState<number | null>(null);

  const { data: pacientes } = useQuery({
    queryKey: ['pacientes-lista'],
    queryFn: async () => { const r = await api.get('/pacientes'); return r.data as Paciente[]; },
  });
  const pacienteMap = new Map(pacientes?.map(p => [p.id, p]) || []);

  const { data: usuarios } = useQuery({
    queryKey: ['usuarios-todos'],
    queryFn: async () => { const r = await api.get('/auth/usuarios', { params: { size: 100 } }); return r.data.content as User[]; },
  });
  const currentUserId = usuarios?.find(u => u.username === user?.username)?.id;

  const { data: citasPaciente } = useQuery({
    queryKey: ['citas-paciente', selectedPacienteId],
    queryFn: async () => { const r = await api.get(`/citas/paciente/${selectedPacienteId}`); return r.data as Cita[]; },
    enabled: !!selectedPacienteId,
  });

  useEffect(() => {
    if (!selectedPacienteId) return;
    const active = citasPaciente
      ?.filter(c => ['PROGRAMADA', 'CONFIRMADA', 'EN_CURSO'].includes(c.estado))
      ?.sort((a, b) => new Date(b.fechaHora).getTime() - new Date(a.fechaHora).getTime())[0];
    if (active && !form.getFieldValue('citaId')) {
      form.setFieldValue('citaId', active.id);
    }
  }, [citasPaciente, selectedPacienteId, form]);

  const { data, isLoading } = useQuery({
    queryKey: ['consultas', page, search],
    queryFn: async () => {
      const params: Record<string, unknown> = { page, size: 10 };
      if (search) params.search = search;
      const res = await api.get('/consultas', { params });
      return normalizeResponse<Consulta>(res.data);
    },
  });

  const createMutation = useMutation({
    mutationFn: (values: Consulta) => api.post('/consultas', values),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['consultas'] }); showCrudSuccess('creado', 'Consulta'); setModalOpen(false); setSelectedPacienteId(null); },
    onError: () => Modal.error({ title: 'Error', content: 'Error al registrar consulta' }),
  });

  const handlePatientSelect = (p: Paciente) => {
    setSelectedPacienteId(p.id);
    form.setFieldValue('pacienteId', p.id);
    if (currentUserId) form.setFieldValue('doctorId', currentUserId);
  };

  const columns = [
    { title: 'Nº', key: 'index', width: 60, render: (_v: unknown, _r: unknown, i: number) => <Text style={{ color: 'var(--text-muted)' }}>{i + 1}</Text> },
    { title: 'Paciente', key: 'pacienteId', render: (v: unknown, r: Consulta) => { const p = pacienteMap.get(r.pacienteId); return p ? <Space><UserOutlined style={{ color: '#00D4AA' }} /><Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{p.nombres} {p.apellidoPaterno}</Text><Tag style={{ borderRadius: 4, fontSize: 11 }}>{p.dni}</Tag></Space> : <Text style={{ color: 'var(--text-secondary)' }}>#{r.pacienteId}</Text>; } },
    { title: 'Doctor', key: 'doctorId', render: (v: unknown, r: Consulta) => { const d = usuarios?.find(u => u.id === r.doctorId); return d ? <Space><MedicineBoxOutlined style={{ color: '#3B82F6' }} /><Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>Dr. {d.nombres} {d.apellidos}</Text></Space> : <Text style={{ color: 'var(--text-secondary)' }}>#{r.doctorId}</Text>; } },
    { title: 'Fecha', dataIndex: 'fechaConsulta', key: 'fechaConsulta', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{dayjs(v).format('DD/MM/YYYY HH:mm')}</Text> },
    { title: 'Diagnóstico', dataIndex: 'diagnostico', key: 'diagnostico', ellipsis: true, render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Tratamiento', dataIndex: 'tratamiento', key: 'tratamiento', ellipsis: true, render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
  ];

  return (
    <div>
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(139,92,246,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#8B5CF6', fontSize: 20 }}>
              <FileTextOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Consultas</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Registro de consultas médicas</Text>
            </div>
          </Space>
        </div>
        <Space>
          <SearchInput.Search placeholder="Buscar por DNI o nombre del paciente" allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>Nueva Consulta</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'auto' }}>
        <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={isLoading} scroll={{ x: 650 }}
          pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false }} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Nueva Consulta</Text>}
        open={modalOpen} onCancel={() => setModalOpen(false)} onOk={() => form.submit()} okText="Registrar" width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={(v) => createMutation.mutate({ ...v, fechaConsulta: v.fechaConsulta?.format?.('YYYY-MM-DDTHH:mm:ss') ?? v.fechaConsulta })} preserve={false}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="pacienteId" label="Paciente" rules={[{ required: true }]} style={{ width: '50%' }}>
              <PacienteSearchByDni pacientes={pacientes || []} onSelect={handlePatientSelect} value={selectedPacienteId} onChange={(v) => setSelectedPacienteId(v)} />
            </Form.Item>
            <Form.Item name="citaId" label="ID de Cita" rules={[{ required: true }]} style={{ width: '50%' }}>
              <InputNumber min={1} style={{ width: '100%' }} placeholder="Auto" disabled />
            </Form.Item>
          </div>
          <Form.Item name="doctorId" hidden><Input /></Form.Item>
          <Form.Item name="fechaConsulta" label="Fecha de Consulta" rules={[{ required: true }]}>
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="sintomas" label="Síntomas">
            <Input.TextArea rows={2} placeholder="Síntomas del paciente" />
          </Form.Item>
          <Form.Item name="diagnostico" label="Diagnóstico" rules={[{ required: true }]}>
            <Input.TextArea rows={3} placeholder="Diagnóstico médico" />
          </Form.Item>
          <Form.Item name="tratamiento" label="Tratamiento" rules={[{ required: true }]}>
            <Input.TextArea rows={3} placeholder="Tratamiento indicado" />
          </Form.Item>
          <Form.Item name="observaciones" label="Observaciones">
            <Input.TextArea rows={2} placeholder="Observaciones adicionales" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
