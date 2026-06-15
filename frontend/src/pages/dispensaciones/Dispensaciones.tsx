import { Table, Button, Modal, Form, Input, InputNumber, DatePicker, Select, Tag, Space, Input as SearchInput, Typography, message } from 'antd';
import { PlusOutlined, SearchOutlined, MedicineBoxOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { showCrudSuccess } from '../../utils/notifications';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import type { Dispensacion, Receta, Paciente, Medicamento, User } from '../../types';
import PacienteSearchByDni from '../../components/paciente/PacienteSearchByDni';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function Dispensaciones() {
  const [form] = Form.useForm();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedRecetaId, setSelectedRecetaId] = useState<number | null>(null);
  const [selectedPacienteId, setSelectedPacienteId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['dispensaciones', page, search],
    queryFn: async () => {
      const params: Record<string, unknown> = { page, size: 10 };
      if (search) params.search = search;
      const res = await api.get('/dispensaciones', { params });
      const arr = Array.isArray(res.data) ? res.data : res.data?.content ?? [];
      return { content: arr, totalElements: arr.length };
    },
  });

  const { data: pacientes } = useQuery({
    queryKey: ['pacientes-lista'],
    queryFn: async () => { const r = await api.get('/pacientes'); return r.data as Paciente[]; },
  });
  const pacienteMap = new Map(pacientes?.map(p => [p.id, p]) || []);

  const { data: medicamentos } = useQuery({
    queryKey: ['medicamentos-lista'],
    queryFn: async () => { const r = await api.get('/medicamentos'); return r.data as Medicamento[]; },
  });
  const medicamentoMap = new Map(medicamentos?.map(m => [m.id, m]) || []);

  const { data: usuarios } = useQuery({
    queryKey: ['usuarios-todos'],
    queryFn: async () => { const r = await api.get('/auth/usuarios', { params: { size: 100 } }); return r.data.content as User[]; },
  });
  const usuarioMap = new Map(usuarios?.map(u => [u.id, u]) || []);
  const currentUserId = usuarios?.find(u => u.username === user?.username)?.id;

  const { data: allRecetas } = useQuery({
    queryKey: ['recetas-pendientes'],
    queryFn: async () => { const r = await api.get('/recetas/pendientes'); return r.data as Receta[]; },
  });

  const recetasFiltered = selectedPacienteId
    ? allRecetas?.filter(r => r.pacienteId === selectedPacienteId) ?? []
    : allRecetas ?? [];

  const selectedReceta = recetasFiltered.find(r => r.id === selectedRecetaId);

  const handlePatientSelect = (p: Paciente) => {
    setSelectedPacienteId(p.id);
    setSelectedRecetaId(null);
    form.setFieldValue('pacienteId', p.id);
    form.setFieldValue('recetaId', undefined);
    form.setFieldValue('medicamentoId', undefined);
    form.setFieldValue('cantidad', undefined);
    if (currentUserId) form.setFieldValue('farmaceuticoId', currentUserId);
  };

  const createMutation = useMutation({
    mutationFn: async (values: Dispensacion) => {
      const r = recetasFiltered.find(re => re.id === values.recetaId);
      if (r && !r.pagado) {
        throw new Error('La receta no ha sido pagada. El paciente debe pagar antes de la dispensación.');
      }
      const dispRes = await api.post('/dispensaciones', values);
      await api.put(`/recetas/${values.recetaId}/dispensar`);
      const paciente = pacienteMap.get(selectedPacienteId ?? 0);
      if (paciente?.email) {
        await api.post('/notificaciones/enviar-correo-personalizado', {
          to: paciente.email,
          asunto: 'Medicamentos entregados - Clínica',
          mensaje: `Estimado(a) ${paciente.nombres}, sus medicamentos han sido entregados exitosamente. Gracias por su preferencia.`,
        }).catch(() => {});
      }
      return dispRes.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dispensaciones'] });
      queryClient.invalidateQueries({ queryKey: ['recetas-pendientes'] });
      showCrudSuccess('creado', 'Dispensación');
      setModalOpen(false);
      setSelectedRecetaId(null);
      setSelectedPacienteId(null);
    },
    onError: (err: Error) => {
      const msg = err.message || 'Error al registrar dispensación';
      if (msg.includes('no ha sido pagada')) {
        Modal.error({ title: 'Pago Pendiente', content: msg });
      } else {
        message.error(msg);
      }
    },
  });

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60, render: (v: number) => <Text style={{ color: 'var(--text-muted)' }}>{v}</Text> },
    { title: 'Receta ID', dataIndex: 'recetaId', key: 'recetaId', render: (v: number) => <Tag style={{ borderRadius: 4 }}>#{v}</Tag> },
    { title: 'Medicamento', key: 'medicamentoId', render: (v: unknown, r: Dispensacion) => { const m = medicamentoMap.get(r.medicamentoId); return m ? <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{m.nombre}</Text> : <Text style={{ color: 'var(--text-secondary)' }}>#{r.medicamentoId}</Text>; } },
    { title: 'Cantidad', dataIndex: 'cantidad', key: 'cantidad', render: (v: number) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Farmacéutico', key: 'farmaceuticoId', render: (v: unknown, r: Dispensacion) => { const u = usuarioMap.get(r.farmaceuticoId); return u ? <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{u.nombres} {u.apellidos}</Text> : <Text style={{ color: 'var(--text-secondary)' }}>#{r.farmaceuticoId}</Text>; } },
    { title: 'Fecha', dataIndex: 'fechaDispensacion', key: 'fechaDispensacion', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{dayjs(v).format('DD/MM/YYYY HH:mm')}</Text> },
  ];

  const handleRecetaSelect = (recetaId: number) => {
    setSelectedRecetaId(recetaId);
    const r = recetasFiltered.find(re => re.id === recetaId);
    if (r) {
      form.setFieldValue('recetaId', recetaId);
    }
  };

  return (
    <div>
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(139,92,246,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#8B5CF6', fontSize: 20 }}>
              <MedicineBoxOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Dispensaciones</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Registro de dispensación de medicamentos</Text>
            </div>
          </Space>
        </div>
        <Space>
          <SearchInput.Search placeholder="Buscar..." allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setModalOpen(true); setSelectedRecetaId(null); setSelectedPacienteId(null); }}>Nueva Dispensación</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'hidden' }}>
        <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={isLoading}
          pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false }} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Nueva Dispensación</Text>}
        open={modalOpen} onCancel={() => { setModalOpen(false); setSelectedRecetaId(null); setSelectedPacienteId(null); }} onOk={() => form.submit()} okText="Dispensar" width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={(v) => createMutation.mutate(v)} preserve={false}
          style={{ width: '100%' }}>
          <Form.Item label="Paciente" required>
            <PacienteSearchByDni pacientes={pacientes || []} onSelect={handlePatientSelect} value={selectedPacienteId} onChange={(v) => setSelectedPacienteId(v)} />
          </Form.Item>
          {(selectedPacienteId) && (
            <Form.Item label="Receta Pendiente" required>
              <Select placeholder="Seleccionar receta..." value={selectedRecetaId} onChange={handleRecetaSelect} style={{ width: '100%' }}>
                {recetasFiltered.map(r => {
                  const p = pacienteMap.get(r.pacienteId);
                  return (
                    <Select.Option key={r.id} value={r.id}>
                      #{r.id} — {p ? `${p.nombres} ${p.apellidoPaterno}` : `Paciente #${r.pacienteId}`} — {r.medicamentos.substring(0, 40)}...
                    </Select.Option>
                  );
                })}
                {recetasFiltered.length === 0 && (
                  <Select.Option value={-1} disabled>No hay recetas pendientes para este paciente</Select.Option>
                )}
              </Select>
            </Form.Item>
          )}
          {selectedReceta && (
            <div style={{ background: 'var(--bg-secondary)', padding: '12px 16px', borderRadius: 8, marginBottom: 16 }}>
              <Text style={{ fontSize: 13, color: 'var(--text-secondary)' }}><strong>Medicamentos:</strong> {selectedReceta.medicamentos}</Text>
              {selectedReceta.indicaciones && <br />}
              {selectedReceta.indicaciones && <Text style={{ fontSize: 13, color: 'var(--text-secondary)' }}><strong>Indicaciones:</strong> {selectedReceta.indicaciones}</Text>}
              <div style={{ marginTop: 8 }}>
                {selectedReceta.pagado
                  ? <Tag color="success" style={{ borderRadius: 4 }}>Pagado</Tag>
                  : <Tag color="error" style={{ borderRadius: 4 }}>No Pagado</Tag>}
              </div>
            </div>
          )}
          <Form.Item name="recetaId" label="ID de Receta" rules={[{ required: true, message: 'Seleccione una receta' }]}>
            <InputNumber min={1} style={{ width: '100%' }} placeholder="Auto" disabled />
          </Form.Item>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="medicamentoId" label="Medicamento" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Select showSearch placeholder="Buscar medicamento..." filterOption={(input, option) => (option?.label ?? '').toLowerCase().includes(input.toLowerCase())}
                options={medicamentos?.map(m => ({ label: `${m.nombre} (${m.codigo})${m.presentacion ? ` — ${m.presentacion}` : ''}`, value: m.id })) || []} />
            </Form.Item>
            <Form.Item name="cantidad" label="Cantidad" rules={[{ required: true }]} style={{ width: '50%' }}>
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
          </div>
          <Form.Item name="fechaDispensacion" label="Fecha de Dispensación" rules={[{ required: true }]}>
            <DatePicker showTime style={{ width: '100%' }} onChange={(d) => { if (d) form.setFieldValue('fechaDispensacion', d.toISOString()); }} />
          </Form.Item>
          <Form.Item name="farmaceuticoId" hidden><Input /></Form.Item>
          <Form.Item name="observaciones" label="Observaciones">
            <Input.TextArea rows={2} placeholder="Notas adicionales" />
          </Form.Item>
          {selectedReceta && pacienteMap.get(selectedReceta.pacienteId)?.email && (
            <div style={{ background: '#f0fdf4', padding: '8px 12px', borderRadius: 6, marginTop: 8 }}>
              <CheckCircleOutlined style={{ color: '#00D4AA', marginRight: 8 }} />
              <Text style={{ fontSize: 12, color: '#059669' }}>Se enviará email de agradecimiento a {pacienteMap.get(selectedReceta.pacienteId)?.email}</Text>
            </div>
          )}
        </Form>
      </Modal>
    </div>
  );
}
