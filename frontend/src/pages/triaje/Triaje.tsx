import { Table, Button, Modal, Form, Input, InputNumber, DatePicker, Tag, Space, Input as SearchInput, Typography, message } from 'antd';
import { PlusOutlined, EditOutlined, SearchOutlined, HeartOutlined, UserOutlined } from '@ant-design/icons';
import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { showCrudSuccess } from '../../utils/notifications';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import type { Triaje, Paciente, Cita, User } from '../../types';
import PacienteSearchByDni from '../../components/paciente/PacienteSearchByDni';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function Triajes() {
  const [form] = Form.useForm();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Triaje | null>(null);
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

  const { data: citasPaciente } = useQuery({
    queryKey: ['citas-paciente', selectedPacienteId],
    queryFn: async () => { const r = await api.get(`/citas/paciente/${selectedPacienteId}`); return r.data as Cita[]; },
    enabled: !!selectedPacienteId,
  });

  const currentUserId = usuarios?.find(u => u.username === user?.username)?.id;

  useEffect(() => {
    if (editing || !selectedPacienteId) return;
    const active = citasPaciente
      ?.filter(c => ['PROGRAMADA', 'CONFIRMADA', 'EN_CURSO'].includes(c.estado))
      ?.sort((a, b) => new Date(b.fechaHora).getTime() - new Date(a.fechaHora).getTime())[0];
    if (active && !form.getFieldValue('citaId')) {
      form.setFieldValue('citaId', active.id);
    }
  }, [citasPaciente, selectedPacienteId, editing, form]);

  const { data, isLoading } = useQuery({
    queryKey: ['triajes', page, search],
    queryFn: async () => {
      const params: Record<string, unknown> = { page, size: 10 };
      if (search) params.search = search;
      const res = await api.get('/triajes', { params });
      return res.data;
    },
  });

  const createMutation = useMutation({
    mutationFn: (values: Triaje) => api.post('/triajes', values),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['triajes'] }); showCrudSuccess('creado', 'Triaje'); setModalOpen(false); setEditing(null); setSelectedPacienteId(null); },
    onError: () => Modal.error({ title: 'Error', content: 'Error al registrar triaje' }),
  });

  const updateMutation = useMutation({
    mutationFn: (values: Triaje) => api.put('/triajes/' + values.id, values),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['triajes'] }); showCrudSuccess('actualizado', 'Triaje'); setModalOpen(false); setEditing(null); setSelectedPacienteId(null); },
    onError: () => Modal.error({ title: 'Error', content: 'Error al actualizar triaje' }),
  });

  const openCreate = () => { setEditing(null); setSelectedPacienteId(null); form.resetFields(); setModalOpen(true); };

  const openEdit = (r: Triaje) => {
    setEditing(r);
    setSelectedPacienteId(r.pacienteId);
    form.setFieldsValue({ ...r, fechaTriaje: r.fechaTriaje ? dayjs(r.fechaTriaje) : undefined });
    setModalOpen(true);
  };

  const handlePatientSelect = (p: Paciente) => {
    setSelectedPacienteId(p.id);
    form.setFieldValue('pacienteId', p.id);
    if (currentUserId) form.setFieldValue('enfermeroId', currentUserId);
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60, render: (v: number) => <Text style={{ color: 'var(--text-muted)' }}>{v}</Text> },
    { title: 'Paciente', key: 'pacienteId', render: (v: unknown, r: Triaje) => { const p = pacienteMap.get(r.pacienteId); return p ? <Space><UserOutlined style={{ color: '#00D4AA' }} /><Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{p.nombres} {p.apellidoPaterno}</Text><Tag style={{ borderRadius: 4, fontSize: 11 }}>{p.dni}</Tag></Space> : <Tag style={{ borderRadius: 4 }}>#{r.pacienteId}</Tag>; } },
    { title: 'Peso', dataIndex: 'peso', key: 'peso', render: (v: number) => v ? <Text style={{ color: 'var(--text-secondary)' }}>{v} kg</Text> : '-' },
    { title: 'Talla', dataIndex: 'talla', key: 'talla', render: (v: number) => v ? <Text style={{ color: 'var(--text-secondary)' }}>{v} m</Text> : '-' },
    { title: 'Presión', key: 'presion', render: (_: unknown, r: Triaje) =>
      r.presionSistolica && r.presionDiastolica
        ? <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{r.presionSistolica}/{r.presionDiastolica}</Text>
        : '-' },
    { title: 'T °', dataIndex: 'temperatura', key: 'temperatura', render: (v: number) => v ? <Text style={{ color: 'var(--text-secondary)' }}>{v} °C</Text> : '-' },
    { title: 'FC', dataIndex: 'frecuenciaCardiaca', key: 'frecuenciaCardiaca', render: (v: number) => v ? <Text style={{ color: 'var(--text-secondary)' }}>{v} lpm</Text> : '-' },
    { title: 'SpO2', dataIndex: 'spo2', key: 'spo2', render: (v: number) => v ? <Text style={{ color: 'var(--text-secondary)' }}>{v}%</Text> : '-' },
    { title: 'Fecha', dataIndex: 'fechaTriaje', key: 'fechaTriaje', render: (v: string) => <Text style={{ color: 'var(--text-muted)' }}>{v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '-'}</Text> },
    {
      title: '', key: 'acciones', width: 60,
      render: (_: unknown, r: Triaje) => (
        <Button type="text" icon={<EditOutlined />} style={{ color: '#3B82F6' }} onClick={() => openEdit(r)} />
      ),
    },
  ];

  return (
    <div>
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(239,68,68,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#EF4444', fontSize: 20 }}>
              <HeartOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Triaje</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Registro de signos vitales por enfermería</Text>
            </div>
          </Space>
        </div>
        <Space>
          <SearchInput.Search placeholder="Buscar..." allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nuevo Triaje</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'hidden' }}>
        <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={isLoading}
          pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false }} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{editing ? 'Editar Triaje' : 'Nuevo Triaje'}</Text>}
        open={modalOpen} onCancel={() => { setModalOpen(false); setEditing(null); }} onOk={() => form.submit()} okText={editing ? 'Actualizar' : 'Registrar'} width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={(v) => editing ? updateMutation.mutate({ ...v, id: editing.id }) : createMutation.mutate(v)} preserve={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="pacienteId" label="Paciente" rules={[{ required: true }]} style={{ width: '33%' }}>
              <PacienteSearchByDni pacientes={pacientes || []} onSelect={handlePatientSelect} value={selectedPacienteId} onChange={(v) => setSelectedPacienteId(v)} />
            </Form.Item>
            <Form.Item name="citaId" label="ID de Cita" style={{ width: '33%' }}>
              <InputNumber min={1} style={{ width: '100%' }} placeholder="Auto" disabled />
            </Form.Item>
            <Form.Item name="fechaTriaje" label="Fecha" style={{ width: '34%' }}>
              <DatePicker showTime style={{ width: '100%' }} onChange={(d) => { if (d) form.setFieldValue('fechaTriaje', d.toISOString()); }} />
            </Form.Item>
          </div>
          <Form.Item name="enfermeroId" hidden><Input /></Form.Item>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="peso" label="Peso (kg)" style={{ width: '25%' }}>
              <InputNumber min={0} step={0.1} style={{ width: '100%' }} placeholder="70.5" />
            </Form.Item>
            <Form.Item name="talla" label="Talla (m)" style={{ width: '25%' }}>
              <InputNumber min={0} step={0.01} style={{ width: '100%' }} placeholder="1.75" />
            </Form.Item>
            <Form.Item name="temperatura" label="Temperatura (°C)" style={{ width: '25%' }}>
              <InputNumber min={0} step={0.1} style={{ width: '100%' }} placeholder="36.5" />
            </Form.Item>
            <Form.Item name="spo2" label="SpO2 (%)" style={{ width: '25%' }}>
              <InputNumber min={0} max={100} step={1} style={{ width: '100%' }} placeholder="98" />
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="presionSistolica" label="Presión Sistólica" style={{ width: '25%' }}>
              <InputNumber min={0} style={{ width: '100%' }} placeholder="120" />
            </Form.Item>
            <Form.Item name="presionDiastolica" label="Presión Diastólica" style={{ width: '25%' }}>
              <InputNumber min={0} style={{ width: '100%' }} placeholder="80" />
            </Form.Item>
            <Form.Item name="frecuenciaCardiaca" label="FC (lpm)" style={{ width: '25%' }}>
              <InputNumber min={0} style={{ width: '100%' }} placeholder="72" />
            </Form.Item>
            <Form.Item name="frecuenciaRespiratoria" label="FR (rpm)" style={{ width: '25%' }}>
              <InputNumber min={0} style={{ width: '100%' }} placeholder="16" />
            </Form.Item>
          </div>
          <Form.Item name="motivoConsulta" label="Motivo de Consulta">
            <Input.TextArea rows={2} placeholder="Motivo por el que el paciente acude a consulta" />
          </Form.Item>
          <Form.Item name="observaciones" label="Observaciones">
            <Input.TextArea rows={2} placeholder="Observaciones adicionales de enfermería" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
