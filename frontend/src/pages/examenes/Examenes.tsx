import { Table, Button, Modal, Form, Input, InputNumber, Select, Tag, Space, Input as SearchInput, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, ExperimentOutlined, CheckCircleOutlined, UserOutlined } from '@ant-design/icons';
import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { showCrudSuccess } from '../../utils/notifications';
import api from '../../services/api';
import type { OrdenExamen, Paciente, Cita, Servicio } from '../../types';
import PacienteSearchByDni from '../../components/paciente/PacienteSearchByDni';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

const typeColors: Record<string, string> = { LABORATORIO: '#3B82F6', IMAGENES: '#8B5CF6', OTRO: '#F59E0B' };
const statusColors: Record<string, string> = { PENDIENTE: '#F59E0B', COMPLETADO: '#00D4AA' };

export default function Examenes() {
  const [form] = Form.useForm();
  const [resultForm] = Form.useForm();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [resultModalOpen, setResultModalOpen] = useState<{ open: boolean; examen: OrdenExamen | null }>({ open: false, examen: null });
  const [editing, setEditing] = useState<OrdenExamen | null>(null);
  const [selectedPacienteId, setSelectedPacienteId] = useState<number | null>(null);

  const { data: pacientes } = useQuery({
    queryKey: ['pacientes-lista'],
    queryFn: async () => { const r = await api.get('/pacientes'); return r.data as Paciente[]; },
  });

  const { data: servicios } = useQuery({
    queryKey: ['servicios-lista'],
    queryFn: async () => { const r = await api.get('/servicios'); return r.data as Servicio[]; },
  });

  const serviciosExamen = servicios?.filter(s => s.tipo === 'EXAMEN') || [];

  const pacienteMap = new Map(pacientes?.map(p => [p.id, p]) || []);

  const { data: citasPaciente } = useQuery({
    queryKey: ['citas-paciente', selectedPacienteId],
    queryFn: async () => { const r = await api.get(`/citas/paciente/${selectedPacienteId}`); return r.data as Cita[]; },
    enabled: !!selectedPacienteId,
  });

  useEffect(() => {
    if (editing || !selectedPacienteId) return;
    const active = citasPaciente
      ?.filter(c => ['PROGRAMADA', 'CONFIRMADA', 'EN_CURSO'].includes(c.estado))
      ?.sort((a, b) => new Date(b.fechaHora).getTime() - new Date(a.fechaHora).getTime())[0];
    if (active && !form.getFieldValue('citaId')) {
      form.setFieldValue('citaId', active.id);
    }
  }, [citasPaciente, selectedPacienteId, editing, form]);

  const handleTipoChange = (tipo: string) => {
    form.setFieldValue('tipo', tipo);
    const match = serviciosExamen.find(s =>
      (tipo === 'LABORATORIO' && s.nombre.toLowerCase().includes('hemograma')) ||
      (tipo === 'IMAGENES' && s.nombre.toLowerCase().includes('radiografía')) ||
      (tipo === 'OTRO')
    );
    if (match) {
      form.setFieldValue('costo', match.precio);
    } else if (serviciosExamen.length > 0) {
      form.setFieldValue('costo', serviciosExamen[0].precio);
    }
  };

  const { data, isLoading } = useQuery({
    queryKey: ['ordenes-examen', page, search],
    queryFn: async () => {
      const params: Record<string, unknown> = { page, size: 10 };
      if (search) params.search = search;
      const res = await api.get('/ordenes-examen', { params });
      return res.data;
    },
  });

  const createMutation = useMutation({
    mutationFn: (values: OrdenExamen) => api.post('/ordenes-examen', values),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['ordenes-examen'] }); showCrudSuccess('creada', 'Orden de Examen'); setModalOpen(false); setEditing(null); setSelectedPacienteId(null); },
    onError: () => Modal.error({ title: 'Error', content: 'Error al crear orden de examen' }),
  });

  const updateMutation = useMutation({
    mutationFn: (values: OrdenExamen) => api.put('/ordenes-examen/' + values.id, values),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['ordenes-examen'] }); showCrudSuccess('actualizada', 'Orden de Examen'); setModalOpen(false); setEditing(null); setSelectedPacienteId(null); },
    onError: () => Modal.error({ title: 'Error', content: 'Error al actualizar orden de examen' }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete('/ordenes-examen/' + id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['ordenes-examen'] }); showCrudSuccess('eliminada', 'Orden de Examen'); },
    onError: () => Modal.error({ title: 'Error', content: 'Error al eliminar orden de examen' }),
  });

  const resultMutation = useMutation({
    mutationFn: ({ id, resultado }: { id: number; resultado: string }) => api.put('/ordenes-examen/' + id + '/resultado', { resultado }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['ordenes-examen'] }); showCrudSuccess('completada', 'Orden de Examen'); setResultModalOpen({ open: false, examen: null }); },
    onError: () => Modal.error({ title: 'Error', content: 'Error al ingresar resultado' }),
  });

  const openCreate = () => { setEditing(null); setSelectedPacienteId(null); form.resetFields(); setModalOpen(true); };

  const openEdit = (r: OrdenExamen) => {
    setEditing(r);
    setSelectedPacienteId(r.pacienteId);
    form.setFieldsValue(r);
    setModalOpen(true);
  };

  const openResult = (r: OrdenExamen) => {
    resultForm.resetFields();
    setResultModalOpen({ open: true, examen: r });
  };

  const handlePatientSelect = (p: Paciente) => {
    setSelectedPacienteId(p.id);
    form.setFieldValue('pacienteId', p.id);
  };

  const columns = [
    { title: 'Nº', key: 'index', width: 60, render: (_v: unknown, _r: unknown, i: number) => <Text style={{ color: 'var(--text-muted)' }}>{i + 1}</Text> },
    { title: 'Paciente', key: 'pacienteId', render: (v: unknown, r: OrdenExamen) => { const p = pacienteMap.get(r.pacienteId); return p ? <Space><UserOutlined style={{ color: '#00D4AA' }} /><Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{p.nombres} {p.apellidoPaterno}</Text><Tag style={{ borderRadius: 4, fontSize: 11 }}>{p.dni}</Tag></Space> : <Tag style={{ borderRadius: 4 }}>#{r.pacienteId}</Tag>; } },
    { title: 'Tipo', dataIndex: 'tipo', key: 'tipo', render: (v: string) => <Tag color={typeColors[v]} style={{ borderRadius: 4 }}>{v}</Tag> },
    { title: 'Descripción', dataIndex: 'descripcion', key: 'descripcion', ellipsis: true, render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Resultado', dataIndex: 'resultado', key: 'resultado', ellipsis: true, render: (v: string) => v ? <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> : <Text style={{ color: 'var(--text-muted)' }}>Pendiente</Text> },
    { title: 'Estado', dataIndex: 'estado', key: 'estado', render: (v: string) => <Tag color={statusColors[v]} style={{ borderRadius: 4 }}>{v}</Tag> },
    { title: 'Pagado', dataIndex: 'pagado', key: 'pagado', render: (v: boolean) => v ? <Tag color="success" style={{ borderRadius: 4 }}>Sí</Tag> : <Tag style={{ borderRadius: 4 }}>No</Tag> },
    { title: 'Fecha', dataIndex: 'fechaOrden', key: 'fechaOrden', render: (v: string) => <Text style={{ color: 'var(--text-muted)' }}>{v ? dayjs(v).format('DD/MM/YYYY') : '-'}</Text> },
    {
      title: '', key: 'acciones', width: 140,
      render: (_: unknown, r: OrdenExamen) => (
        <Space>
          {r.estado === 'PENDIENTE' && (
            <Button type="text" icon={<CheckCircleOutlined />} style={{ color: '#00D4AA' }} onClick={() => openResult(r)} title="Ingresar Resultado" />
          )}
          <Button type="text" icon={<EditOutlined />} style={{ color: '#3B82F6' }} onClick={() => openEdit(r)} />
          <Button type="text" icon={<DeleteOutlined />} style={{ color: '#EF4444' }} onClick={() => deleteMutation.mutate(r.id!)} />
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(139,92,246,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#8B5CF6', fontSize: 20 }}>
              <ExperimentOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Órdenes de Exámenes</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Gestión de exámenes de laboratorio e imágenes</Text>
            </div>
          </Space>
        </div>
        <Space>
          <SearchInput.Search placeholder="Buscar..." allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nueva Orden</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'hidden' }}>
        <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={isLoading}
          pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false }} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{editing ? 'Editar Orden' : 'Nueva Orden de Examen'}</Text>}
        open={modalOpen} onCancel={() => { setModalOpen(false); setEditing(null); }} onOk={() => form.submit()} okText={editing ? 'Actualizar' : 'Crear'} width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={(v) => editing ? updateMutation.mutate({ ...v, id: editing.id }) : createMutation.mutate(v)} preserve={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="pacienteId" label="Paciente" rules={[{ required: true }]} style={{ width: '50%' }}>
              <PacienteSearchByDni pacientes={pacientes || []} onSelect={handlePatientSelect} value={selectedPacienteId} onChange={(v) => setSelectedPacienteId(v)} />
            </Form.Item>
            <Form.Item name="citaId" label="ID de Cita" style={{ width: '50%' }}>
              <InputNumber min={1} style={{ width: '100%' }} placeholder="Auto" disabled />
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="tipo" label="Tipo" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Select placeholder="Seleccionar tipo" onChange={handleTipoChange}>
                <Select.Option value="LABORATORIO">Laboratorio</Select.Option>
                <Select.Option value="IMAGENES">Imágenes</Select.Option>
                <Select.Option value="OTRO">Otro</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item name="costo" label="Costo (S/.)" style={{ width: '50%' }}>
              <InputNumber min={0} step={0.01} prefix="S/. " style={{ width: '100%' }} disabled />
            </Form.Item>
          </div>
          <Form.Item name="descripcion" label="Descripción" rules={[{ required: true }]}>
            <Input.TextArea rows={3} placeholder="Describa el examen solicitado (ej: Hemograma completo, Rayos X de tórax)" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Ingresar Resultado</Text>}
        open={resultModalOpen.open} onCancel={() => setResultModalOpen({ open: false, examen: null })} onOk={() => resultForm.submit()} okText="Guardar Resultado" width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={resultForm} layout="vertical" onFinish={(v) => resultMutation.mutate({ id: resultModalOpen.examen!.id!, resultado: v.resultado })} preserve={false}
          style={{ width: '100%' }}>
          {resultModalOpen.examen && (
            <div style={{ marginBottom: 16, padding: '12px 16px', background: 'rgba(59,130,246,0.06)', borderRadius: 8, border: '1px solid rgba(59,130,246,0.15)' }}>
              <Text strong>{resultModalOpen.examen.tipo}</Text>
              <Text style={{ display: 'block', color: 'var(--text-secondary)', marginTop: 4 }}>{resultModalOpen.examen.descripcion}</Text>
            </div>
          )}
          <Form.Item name="resultado" label="Resultado del Examen" rules={[{ required: true }]}>
            <Input.TextArea rows={5} placeholder="Ingrese los resultados obtenidos..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}