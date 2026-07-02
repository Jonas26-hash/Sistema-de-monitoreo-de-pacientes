import { useState, useCallback, useMemo } from 'react';
import { Table, Button, Modal, Form, Input, DatePicker, Select, Tag, Space, Typography, message, Descriptions, Divider, Spin } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, CalendarOutlined, UserOutlined, MedicineBoxOutlined, EnvironmentOutlined, CloseCircleOutlined, EyeOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useCrud } from '../../hooks/useCrud';
import { useAuth } from '../../context/AuthContext';
import { showCrudSuccess } from '../../utils/notifications';
import api from '../../services/api';
import type { Cita, Paciente, User } from '../../types';
import PacienteSearchByDni from '../../components/paciente/PacienteSearchByDni';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

const statusColors: Record<string, string> = {
  PROGRAMADA: '#3B82F6', CONFIRMADA: '#F59E0B', EN_CURSO: '#8B5CF6', COMPLETADA: '#00D4AA', CANCELADA: '#EF4444',
};

export default function Citas() {
  const { user } = useAuth();
  const isPaciente = user?.roles?.includes('PACIENTE');
  const queryClient = useQueryClient();
  const [form] = Form.useForm();
  const [createForm] = Form.useForm();
  const [createOpen, setCreateOpen] = useState(false);

  const crud = useCrud<Cita>({
    key: 'citas',
    endpoint: '/citas',
    dateTimeFields: ['fechaHora'],
  });
  const { search, setSearch, editing, modalOpen, openEdit, closeModal, handleSave, handleDelete } = crud;
  const { data: crudData, loading: crudLoading, page, setPage } = crud;

  const { data: profile } = useQuery({
    queryKey: ['profile-citas'],
    queryFn: async () => { const r = await api.get('/auth/profile'); return r.data; },
    enabled: isPaciente,
  });

  const { data: pacienteCitas, isLoading: pacienteCitasLoading } = useQuery({
    queryKey: ['citas-paciente', profile?.pacienteId],
    queryFn: async () => { const r = await api.get(`/citas/paciente/${profile!.pacienteId}`); return r.data as Cita[]; },
    enabled: isPaciente && !!profile?.pacienteId,
  });

  const data = isPaciente ? { content: pacienteCitas || [], totalElements: pacienteCitas?.length || 0 } : crudData;
  const loading = isPaciente ? pacienteCitasLoading : crudLoading;

  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedCita, setSelectedCita] = useState<Cita | null>(null);

  const cancelarMutation = useMutation({
    mutationFn: (cita: Cita) => api.put(`/citas/${cita.id}`, { ...cita, estado: 'CANCELADA' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['citas'] });
      queryClient.invalidateQueries({ queryKey: ['citas-paciente'] });
      showCrudSuccess('cancelada', 'Cita');
      setDetailOpen(false);
      setSelectedCita(null);
    },
    onError: () => Modal.error({ title: 'Error', content: 'Error al cancelar la cita' }),
  });

  const { data: pacientes } = useQuery({
    queryKey: ['pacientes-lista'],
    queryFn: async () => { const r = await api.get('/pacientes'); return r.data as Paciente[]; },
  });
  const pacienteMap = new Map(pacientes?.map(p => [p.id, p]) || []);

  const { data: doctores } = useQuery({
    queryKey: ['doctores'],
    queryFn: async () => {
      const res = await api.get('/auth/usuarios/rol/DOCTOR');
      return res.data as User[];
    },
  });

  const [selectedPaciente, setSelectedPaciente] = useState<Paciente | null>(null);
  const [fechaHora, setFechaHora] = useState<string | null>(null);
  const [doctoresOcupados, setDoctoresOcupados] = useState<number[]>([]);
  const [completingProfile, setCompletingProfile] = useState(false);
  const [profileForm] = Form.useForm();
  const [savingProfile, setSavingProfile] = useState(false);

  const { data: doctoresDisponibles } = useQuery({
    queryKey: ['doctores-disponibles', fechaHora],
    queryFn: async () => {
      if (!fechaHora) return doctores || [];
      try {
        const res = await api.get('/citas/doctores-ocupados', { params: { fechaHora } });
        const ocupados: number[] = res.data;
        setDoctoresOcupados(ocupados);
        return (doctores || []).filter((d) => !ocupados.includes(d.id));
      } catch {
        setDoctoresOcupados([]);
        return doctores || [];
      }
    },
    enabled: !!fechaHora,
  });

  const crearMutation = useMutation({
    mutationFn: (data: { dni: string; doctorId: number; fechaHora: string; motivo: string; observaciones?: string }) =>
      api.post('/citas/por-dni', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['citas'] });
      queryClient.invalidateQueries({ queryKey: ['citas-paciente'] });
      showCrudSuccess('creado');
      setCreateOpen(false);
      createForm.resetFields();
      setSelectedPaciente(null);
      setFechaHora(null);
      setDoctoresOcupados([]);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { error?: string } } }).response?.data?.error || 'Error al crear la cita';
      Modal.error({ title: 'Error', content: msg, centered: true });
    },
  });

  const cambiarEstadoMutation = useMutation({
    mutationFn: ({ id, estado }: { id: number; estado: string }) => {
      const cita = data?.content?.find(c => c.id === id);
      if (!cita) throw new Error('Cita no encontrada');
      return api.put(`/citas/${id}`, { ...cita, estado });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['citas'] });
      queryClient.invalidateQueries({ queryKey: ['citas-paciente'] });
      showCrudSuccess('actualizado', 'Estado de cita');
    },
    onError: () => Modal.error({ title: 'Error', content: 'Error al cambiar estado de la cita' }),
  });

  const citasOrdenadas = useMemo(() => {
    if (!data?.content) return [];
    return [...data.content].sort((a, b) => new Date(b.fechaHora).getTime() - new Date(a.fechaHora).getTime());
  }, [data?.content]);

  const isProfileComplete = (p: Paciente) => p.fechaNacimiento && p.genero && p.direccion;

  const handlePatientSelect = useCallback((p: Paciente) => {
    setSelectedPaciente(p);
    createForm.setFieldValue('dni', p.dni);
    if (!isProfileComplete(p)) {
      profileForm.setFieldsValue({ fechaNacimiento: p.fechaNacimiento ? dayjs(p.fechaNacimiento) : null, genero: p.genero || undefined, direccion: p.direccion || '' });
      setCompletingProfile(true);
    }
  }, [createForm, profileForm]);

  const handleProfileComplete = useCallback(async () => {
    if (!selectedPaciente) return;
    try {
      const values = await profileForm.validateFields();
      setSavingProfile(true);
      await api.put(`/pacientes/${selectedPaciente.id}`, {
        ...selectedPaciente,
        fechaNacimiento: values.fechaNacimiento ? dayjs(values.fechaNacimiento).format('YYYY-MM-DD') : null,
        genero: values.genero || null,
        direccion: values.direccion || null,
      });
      setSelectedPaciente({ ...selectedPaciente, fechaNacimiento: values.fechaNacimiento, genero: values.genero, direccion: values.direccion });
      setCompletingProfile(false);
      showCrudSuccess('actualizado', 'Perfil del paciente');
    } catch {
      if (!(await profileForm.validateFields().catch(() => false))) return;
    } finally {
      setSavingProfile(false);
    }
  }, [selectedPaciente, profileForm]);

  const handleFechaHoraChange = useCallback((date: dayjs.Dayjs | null) => {
    if (date) {
      setFechaHora(date.format('YYYY-MM-DDTHH:mm:ss'));
    } else {
      setFechaHora(null);
    }
  }, []);

  const handleCreateSubmit = useCallback(async () => {
    try {
      const values = await createForm.validateFields();
      crearMutation.mutate({ ...values, fechaHora: values.fechaHora?.format?.('YYYY-MM-DDTHH:mm:ss') ?? values.fechaHora });
    } catch { }
  }, [createForm, crearMutation]);

  const closeCreateModal = useCallback(() => {
    setCreateOpen(false);
    setSelectedPaciente(null);
    setFechaHora(null);
    setDoctoresOcupados([]);
    createForm.resetFields();
  }, [createForm]);

  const columns = [
    { title: 'Nº', key: 'index', width: 50, render: (_v: unknown, _r: unknown, i: number) => <Text style={{ color: 'var(--text-muted)' }}>{i + 1}</Text> },
    ...(!isPaciente ? [{
      title: 'Paciente', key: 'pacienteId', render: (v: unknown, r: Cita) => { const p = pacienteMap.get(r.pacienteId); return p ? <Space size={4} wrap><UserOutlined style={{ color: '#00D4AA', fontSize: 14 }} /><Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{p.nombres} {p.apellidoPaterno}</Text><Tag style={{ borderRadius: 4, fontSize: 11, marginRight: 0 }}>{p.dni}</Tag></Space> : <Text style={{ color: 'var(--text-secondary)' }}>#{r.pacienteId}</Text>; }
    }] : []),
    { title: 'Doctor', key: 'doctorId', responsive: ['md'] as ('md' | 'sm' | 'lg' | 'xl' | 'xxl')[], render: (v: unknown, r: Cita) => { const d = (doctores || []).find(d => d.id === r.doctorId); return d ? <Space><MedicineBoxOutlined style={{ color: '#3B82F6' }} /><Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>Dr. {d.nombres} {d.apellidos}{d.especialidad ? ` (${d.especialidad})` : ''}</Text></Space> : <Text style={{ color: 'var(--text-secondary)' }}>#{r.doctorId}</Text>; } },
    { title: 'Fecha', dataIndex: 'fechaHora', key: 'fechaHora', responsive: ['sm'] as ('md' | 'sm' | 'lg' | 'xl' | 'xxl')[], render: (v: string) => { const esHoy = dayjs(v).isSame(dayjs(), 'day'); return (<Space><Text style={{ color: 'var(--text-secondary)' }}>{dayjs(v).format('DD/MM/YYYY HH:mm')}</Text>{esHoy && <Tag color="#00D4AA" style={{ borderRadius: 4, fontSize: 11, lineHeight: '18px', marginRight: 0 }}>Hoy</Tag>}</Space>); } },
    { title: 'Motivo', dataIndex: 'motivo', key: 'motivo', ellipsis: true, responsive: ['md'] as ('md' | 'sm' | 'lg' | 'xl' | 'xxl')[], render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    {
      title: 'Estado', dataIndex: 'estado', key: 'estado',
      render: (v: string, r: Cita) => (
        <Space>
          <Tag color={statusColors[v] || 'var(--text-muted)'} style={{ borderRadius: 4 }}>{v}</Tag>
          {!isPaciente && v === 'PROGRAMADA' && (
            <Button type="primary" size="small" style={{ background: '#00D4AA', borderColor: '#00D4AA', borderRadius: 6, height: 24, fontSize: 12 }}
              onClick={() => cambiarEstadoMutation.mutate({ id: r.id!, estado: 'EN_CURSO' })}>
              Atender
            </Button>
          )}
          {!isPaciente && v === 'EN_CURSO' && (
            <Button type="primary" size="small" style={{ background: '#3B82F6', borderColor: '#3B82F6', borderRadius: 6, height: 24, fontSize: 12 }}
              onClick={() => cambiarEstadoMutation.mutate({ id: r.id!, estado: 'COMPLETADA' })}>
              Completar
            </Button>
          )}
        </Space>
      ),
    },
    ...(!isPaciente ? [{
      title: '', key: 'acciones', width: 90,
      render: (_: unknown, r: Cita) => (
        <Space size="small">
          <Button type="text" size="small" icon={<EditOutlined />} style={{ color: '#3B82F6' }} onClick={() => openEdit(r)} />
          <Button type="text" size="small" icon={<DeleteOutlined />} style={{ color: '#EF4444' }} onClick={() => handleDelete(r.id!)} />
        </Space>
      ),
    }] : []),
  ];

  return (
    <div>
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(59,130,246,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#3B82F6', fontSize: 20 }}>
              <CalendarOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Citas</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Gestión de citas médicas</Text>
            </div>
          </Space>
        </div>
        <Space>
          <Input.Search placeholder="Buscar por DNI o nombre del paciente" allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setCreateOpen(true); setSelectedPaciente(null); setFechaHora(null); setDoctoresOcupados([]); createForm.resetFields(); }}>Nueva Cita</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'auto' }}>
        <Table columns={columns} dataSource={citasOrdenadas} rowKey="id" loading={loading} scroll={{ x: 650 }}
          onRow={(r) => ({ onClick: () => { setSelectedCita(r); setDetailOpen(true); }, style: { cursor: 'pointer' } })}
          pagination={{ current: page + 1, total: citasOrdenadas.length, onChange: (p) => setPage(p - 1), showSizeChanger: false, size: 'small' }} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Editar Cita</Text>}
        open={modalOpen} onCancel={closeModal} onOk={() => form.submit()} okText="Actualizar" width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={handleSave} initialValues={editing || {}} preserve={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="pacienteId" label="Paciente" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Select showSearch disabled placeholder="Paciente" filterOption={(input, option) => (option?.label ?? '').toLowerCase().includes(input.toLowerCase())}
                options={pacientes?.map(p => ({ label: `${p.nombres} ${p.apellidoPaterno} — DNI: ${p.dni}`, value: p.id })) || []} />
            </Form.Item>
            <Form.Item name="doctorId" label="Doctor" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Select showSearch disabled placeholder="Doctor" filterOption={(input, option) => (option?.label ?? '').toLowerCase().includes(input.toLowerCase())}
                options={doctores?.map(d => ({ label: `Dr. ${d.nombres} ${d.apellidos}${d.especialidad ? ` (${d.especialidad})` : ''}`, value: d.id })) || []} />
            </Form.Item>
          </div>
          <Form.Item name="fechaHora" label="Fecha y Hora" rules={[{ required: true }]}>
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="motivo" label="Motivo" rules={[{ required: true }]}>
            <Input.TextArea rows={2} placeholder="Motivo de la cita" />
          </Form.Item>
          <Form.Item name="estado" label="Estado">
            <Select>
              <Select.Option value="PROGRAMADA">Programada</Select.Option>
              <Select.Option value="CONFIRMADA">Confirmada</Select.Option>
              <Select.Option value="EN_CURSO">En Curso</Select.Option>
              <Select.Option value="COMPLETADA">Completada</Select.Option>
              <Select.Option value="CANCELADA">Cancelada</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="observaciones" label="Observaciones">
            <Input.TextArea rows={2} placeholder="Observaciones adicionales" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Nueva Cita</Text>}
        open={createOpen} onCancel={closeCreateModal}
        onOk={handleCreateSubmit} okText="Crear Cita" okButtonProps={{ loading: crearMutation.isPending, disabled: !selectedPaciente }} width={560} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={createForm} layout="vertical" preserve={false} style={{ width: '100%' }}>
          <Form.Item label="Paciente" required>
            <PacienteSearchByDni pacientes={pacientes || []} onSelect={handlePatientSelect} autoFocus />
          </Form.Item>

          {selectedPaciente && (
            <div style={{ background: 'var(--bg-card)', borderRadius: 12, padding: '12px 16px', marginBottom: 16, border: '1px solid var(--border-color)' }}>
              <Space>
                <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'rgba(0,212,170,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#00D4AA', fontSize: 22 }}>
                  <UserOutlined />
                </div>
                <div>
                  <Text strong style={{ color: 'var(--text-primary)', fontSize: 15, display: 'block' }}>
                    {selectedPaciente.nombres} {selectedPaciente.apellidoPaterno} {selectedPaciente.apellidoMaterno || ''}
                  </Text>
                  <Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>DNI: {selectedPaciente.dni} · ID: #{selectedPaciente.id}</Text>
                </div>
              </Space>
            </div>
          )}

          <Form.Item name="dni" hidden><Input /></Form.Item>

          <Form.Item name="fechaHora" label="Fecha y Hora" rules={[{ required: true, message: 'Seleccione fecha y hora' }]}>
            <DatePicker showTime style={{ width: '100%' }} onChange={handleFechaHoraChange} />
          </Form.Item>

          <Form.Item name="doctorId" label="Doctor" rules={[{ required: true, message: 'Seleccione un doctor' }]}>
            <Select placeholder="Seleccione un doctor disponible" loading={!doctores} notFoundContent={fechaHora ? 'No hay doctores disponibles en este horario' : 'Seleccione fecha/hora primero'}>
              {(doctoresDisponibles || doctores || []).map((d) => (
                <Select.Option key={d.id} value={d.id}>
                  <Space>
                    <MedicineBoxOutlined style={{ color: '#3B82F6' }} />
                    <Text>Dr. {d.nombres} {d.apellidos}{d.especialidad ? ` (${d.especialidad})` : ''}</Text>
                  </Space>
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="motivo" label="Motivo" rules={[{ required: true }]}>
            <Input.TextArea rows={2} placeholder="Motivo de la cita" />
          </Form.Item>

          <Form.Item name="observaciones" label="Observaciones">
            <Input.TextArea rows={2} placeholder="Observaciones adicionales" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Completar Perfil del Paciente</Text>}
        open={completingProfile}
        onCancel={() => setCompletingProfile(false)}
        onOk={handleProfileComplete}
        okText="Guardar y Continuar"
        okButtonProps={{ loading: savingProfile }}
        cancelText="Cancelar"
        width={500}
        destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Text style={{ color: 'var(--text-muted)', display: 'block', marginBottom: 20 }}>
          El paciente no tiene datos completos. Complete los siguientes campos obligatorios para continuar:
        </Text>
        <Form form={profileForm} layout="vertical" preserve={false} style={{ width: '100%' }}>
          <Form.Item name="fechaNacimiento" label="Fecha de Nacimiento" rules={[{ required: true, message: 'Requerido' }]}>
            <DatePicker style={{ width: '100%', borderRadius: 10 }} placeholder="Seleccione fecha" />
          </Form.Item>
          <Form.Item name="genero" label="Género" rules={[{ required: true, message: 'Requerido' }]}>
            <Select placeholder="Seleccione género" style={{ borderRadius: 10 }}>
              <Select.Option value="MASCULINO">Masculino</Select.Option>
              <Select.Option value="FEMENINO">Femenino</Select.Option>
              <Select.Option value="OTRO">Otro</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="direccion" label="Dirección" rules={[{ required: true, message: 'Requerido' }]}>
            <Input.TextArea rows={2} placeholder="Dirección" style={{ borderRadius: 10 }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Detalle de Cita #{selectedCita?.id}</Text>}
        open={detailOpen} onCancel={() => { setDetailOpen(false); setSelectedCita(null); }} footer={null} width={600} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        {!selectedCita ? <Spin /> : (
          <>
            <Descriptions bordered column={1} size="small" labelStyle={{ fontWeight: 500, color: 'var(--text-secondary)', background: 'rgba(0,0,0,0.02)' }} contentStyle={{ color: 'var(--text-primary)' }}>
              {!isPaciente && (
                <Descriptions.Item label="Paciente">
                  {(() => { const p = pacienteMap.get(selectedCita.pacienteId); return p ? `${p.nombres} ${p.apellidoPaterno} (${p.dni})` : `#${selectedCita.pacienteId}`; })()}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="Doctor">
                {(() => { const d = (doctores || []).find(d => d.id === selectedCita.doctorId); return d ? `Dr. ${d.nombres} ${d.apellidos}` : `#${selectedCita.doctorId}`; })()}
              </Descriptions.Item>
              <Descriptions.Item label="Fecha y Hora">{dayjs(selectedCita.fechaHora).format('DD/MM/YYYY HH:mm')}</Descriptions.Item>
              <Descriptions.Item label="Motivo">{selectedCita.motivo || '-'}</Descriptions.Item>
              <Descriptions.Item label="Estado">
                <Tag color={statusColors[selectedCita.estado] || 'default'}>{selectedCita.estado}</Tag>
              </Descriptions.Item>
              {selectedCita.observaciones && <Descriptions.Item label="Observaciones">{selectedCita.observaciones}</Descriptions.Item>}
              {selectedCita.precio != null && <Descriptions.Item label="Precio">S/. {selectedCita.precio.toFixed(2)}</Descriptions.Item>}
            </Descriptions>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
              <Button onClick={() => { setDetailOpen(false); setSelectedCita(null); }}>Cerrar</Button>
              {!isPaciente && (
                <Button icon={<EditOutlined />} onClick={() => { setDetailOpen(false); openEdit(selectedCita); }}>
                  Editar
                </Button>
              )}
              {isPaciente && ['PROGRAMADA', 'CONFIRMADA'].includes(selectedCita.estado) && (
                <Button danger type="primary" icon={<CloseCircleOutlined />}
                  loading={cancelarMutation.isPending}
                  onClick={() => Modal.confirm({
                    title: 'Cancelar Cita',
                    content: `¿Estás seguro de cancelar la cita #${selectedCita.id} del ${dayjs(selectedCita.fechaHora).format('DD/MM/YYYY HH:mm')}?`,
                    okText: 'Sí, cancelar',
                    okButtonProps: { danger: true },
                    cancelText: 'No, mantener',
                    onOk: () => cancelarMutation.mutate(selectedCita),
                  })}>
                  Cancelar Cita
                </Button>
              )}
            </div>
          </>
        )}
      </Modal>
    </div>
  );
}
