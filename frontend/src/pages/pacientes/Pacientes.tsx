import { useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Modal, Form, Input, Select, DatePicker, Space, Input as SearchInput, Typography, Divider, message, Tag, Collapse, Tooltip } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, TeamOutlined, EyeOutlined, UserOutlined, UserAddOutlined, FileTextOutlined, DollarOutlined, RiseOutlined, FallOutlined, BellOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PhoneInput from '../../components/common/PhoneInput';
import { useCrud } from '../../hooks/useCrud';
import api from '../../services/api';
import { showCrudSuccess } from '../../utils/notifications';
import { useAuth } from '../../context/AuthContext';
import ErrorAlert from '../../components/common/ErrorAlert';
import type { Paciente, User, Cobro } from '../../types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function Pacientes() {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailPaciente, setDetailPaciente] = useState<Paciente | null>(null);
  const [reniecLoading, setReniecLoading] = useState(false);
  const [solicitaCuentaModalOpen, setSolicitaCuentaModalOpen] = useState(false);
  const [solicitaCuentaPaciente, setSolicitaCuentaPaciente] = useState<Paciente | null>(null);
  const [solicitaCuentaLoading, setSolicitaCuentaLoading] = useState(false);

  // Modal for ADMIN to create user account from patient list
  const [createUserModalOpen, setCreateUserModalOpen] = useState(false);
  const [createUserPaciente, setCreateUserPaciente] = useState<Paciente | null>(null);
  const [createUserLoading, setCreateUserLoading] = useState(false);

  // Modal for ATENCION_CLIENTE to notify that patient wants an account
  const [notifyModalOpen, setNotifyModalOpen] = useState(false);
  const [notifyPaciente, setNotifyPaciente] = useState<Paciente | null>(null);
  const [notifyLoading, setNotifyLoading] = useState(false);

  const queryClient = useQueryClient();

  const { user } = useAuth();
  const esAdmin = user?.roles?.includes('ADMIN') ?? false;
  const esAtencion = user?.roles?.includes('ATENCION_CLIENTE') ?? false;
  const puedeEditar = esAdmin || esAtencion;
  const { data: usuariosData, isFetched: usuariosLoaded } = useQuery({
    queryKey: ['usuarios-lista'],
    queryFn: async () => { const r = await api.get('/auth/usuarios', { params: { page: 0, size: 999 } }); return r.data.content as User[]; },
  });
  const pacientesConUsuario = useMemo(() => new Set((usuariosData || []).map((u) => u.pacienteId).filter(Boolean)), [usuariosData]);

  const { data: cobrosPaciente, isLoading: loadingCobros } = useQuery({
    queryKey: ['cobros-paciente', detailPaciente?.id],
    queryFn: async () => {
      if (!detailPaciente?.id) return [];
      const r = await api.get(`/cobros/paciente/${detailPaciente.id}`);
      return (r.data || []) as Cobro[];
    },
    enabled: detailOpen && !!detailPaciente?.id,
  });

  const deudasResumen = useMemo(() => {
    if (!cobrosPaciente) return { pendientes: 0, semanal: 0, mensual: 0 };
    const hoy = new Date();
    const semanaAtras = new Date(hoy); semanaAtras.setDate(semanaAtras.getDate() - 7);
    const mesAtras = new Date(hoy); mesAtras.setMonth(mesAtras.getMonth() - 1);
    const pendientes = cobrosPaciente
      .filter(c => c.estado === 'PENDIENTE')
      .reduce((s, c) => s + (c.monto || 0), 0);
    const semanal = cobrosPaciente
      .filter(c => c.estado === 'PAGADO' && c.fechaCobro && new Date(c.fechaCobro) >= semanaAtras)
      .reduce((s, c) => s + (c.monto || 0), 0);
    const mensual = cobrosPaciente
      .filter(c => c.estado === 'PAGADO' && c.fechaCobro && new Date(c.fechaCobro) >= mesAtras)
      .reduce((s, c) => s + (c.monto || 0), 0);
    return { pendientes, semanal, mensual };
  }, [cobrosPaciente]);

  const registroPacienteMutation = useMutation({
    mutationFn: (pacienteId: number) => api.post('/auth/registro-paciente-desde-lista', { pacienteId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['usuarios-lista'] });
      queryClient.invalidateQueries({ queryKey: ['pacientes'] });
      showCrudSuccess('creado', 'Usuario - correo enviado al paciente');
      setCreateUserModalOpen(false);
      setCreateUserPaciente(null);
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Error al crear usuario';
      message.error(msg);
    },
  });

  const updateSolicitaCuentaMutation = useMutation({
    mutationFn: ({ id, solicitaCuenta }: { id: number; solicitaCuenta: boolean }) =>
      api.put(`/pacientes/${id}/solicita-cuenta`, { solicitaCuenta }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pacientes'] });
    },
    onError: () => {
      message.error('Error al actualizar estado');
    },
  });

  const handleSolicitaCuentaConfirm = useCallback(async (solicita: boolean) => {
    if (!solicitaCuentaPaciente?.id) return;
    setSolicitaCuentaLoading(true);
    try {
      await updateSolicitaCuentaMutation.mutateAsync({
        id: solicitaCuentaPaciente.id,
        solicitaCuenta: solicita,
      });
      setSolicitaCuentaModalOpen(false);
      setSolicitaCuentaPaciente(null);
    } catch { } finally {
      setSolicitaCuentaLoading(false);
    }
  }, [solicitaCuentaPaciente, updateSolicitaCuentaMutation]);

  const handleNotifyChange = useCallback(async () => {
    if (!notifyPaciente?.id) return;
    setNotifyLoading(true);
    try {
      await updateSolicitaCuentaMutation.mutateAsync({
        id: notifyPaciente.id,
        solicitaCuenta: true,
      });
      message.success('Solicitud de cuenta registrada. El administrador podrá crear el usuario.');
      setNotifyModalOpen(false);
      setNotifyPaciente(null);
    } catch { } finally {
      setNotifyLoading(false);
    }
  }, [notifyPaciente, updateSolicitaCuentaMutation]);

  const handleOpenDetail = (p: Paciente) => {
    setDetailPaciente(p);
    setDetailOpen(true);
  };

  const { search, setSearch, data, loading, page, setPage, modalOpen, editing, openCreate, openEdit, closeModal, handleSave: crudHandleSave, handleDelete, isError: pacientesError } = useCrud<Paciente>({
    key: 'pacientes',
    endpoint: '/pacientes',
    dateFields: ['fechaNacimiento', 'vigenciaSeguro'],
  });

  const handleDniChange = useCallback(async (value: string) => {
    if (value.length !== 8) return;
    setReniecLoading(true);
    try {
      const res = await api.get(`/pacientes/reniec/dni/${value}`);
      if (res.data?.names) {
        form.setFieldsValue({
          nombres: res.data.names,
          apellidoPaterno: res.data.paternalLastName,
          apellidoMaterno: res.data.maternalLastName,
        });
      }
    } catch {
      // RENIEC no disponible — el usuario llena manual
    } finally {
      setReniecLoading(false);
    }
  }, [form]);

  const handleCreateOrEdit = useCallback(async (values: Paciente) => {
    if (editing?.id) {
      crudHandleSave(values);
      return;
    }
    const prepared = { ...values };
    const fechaVal = (prepared as any).fechaNacimiento;
    if (fechaVal && typeof fechaVal.format === 'function') {
      (prepared as any).fechaNacimiento = fechaVal.format('YYYY-MM-DD');
    }
    const vigVal = (prepared as any).vigenciaSeguro;
    if (vigVal && typeof vigVal.format === 'function') {
      (prepared as any).vigenciaSeguro = vigVal.format('YYYY-MM-DD');
    }
    try {
      const res = await api.post('/pacientes', prepared);
      const created = res.data as Paciente;
      queryClient.invalidateQueries({ queryKey: ['pacientes'] });
      closeModal();
      showCrudSuccess('creado', 'Paciente');
      if (esAtencion && created.id) {
        form.resetFields();
        setSolicitaCuentaPaciente(created);
        setSolicitaCuentaModalOpen(true);
      }
    } catch (err: unknown) {
      const d = (err as { response?: { data?: { error?: string; mensaje?: string } } }).response?.data;
      Modal.error({ title: 'Error', content: d?.mensaje || d?.error || 'Error al crear', centered: true });
    }
  }, [editing, crudHandleSave, queryClient, esAtencion, closeModal, form]);

  const columns = [
    { title: 'Nº', key: 'index', width: 50, render: (_v: unknown, _r: unknown, i: number) => <Text style={{ color: 'var(--text-muted)' }}>{i + 1}</Text> },
    { title: 'Paciente', key: 'nombre', render: (_: unknown, r: Paciente) => <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{r.nombres} {r.apellidoPaterno}{r.apellidoMaterno ? ' ' + r.apellidoMaterno : ''}</Text> },
    { title: 'DNI', dataIndex: 'dni', key: 'dni', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Email', dataIndex: 'email', key: 'email', responsive: ['md'] as ('md' | 'sm' | 'lg' | 'xl' | 'xxl')[], render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Teléfono', dataIndex: 'telefono', key: 'telefono', responsive: ['sm'] as ('md' | 'sm' | 'lg' | 'xl' | 'xxl')[], render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    {
      title: 'Acciones', key: 'acciones', width: 150,
      render: (_: unknown, r: Paciente) => (
        <Space size="small">
          <Button type="text" size="small" icon={<EyeOutlined />} style={{ color: '#00D4AA' }} onClick={() => handleOpenDetail(r)} />
          <Button type="text" size="small" icon={<FileTextOutlined />} style={{ color: '#8B5CF6' }} onClick={() => navigate(`/historial/paciente/${r.id}`)} title="Historial clínico" />
          {puedeEditar && <Button type="text" size="small" icon={<EditOutlined />} style={{ color: '#3B82F6' }} onClick={() => openEdit(r)} />}
          {usuariosLoaded && !pacientesConUsuario.has(r.id) && (
            (() => {
              const noSolicito = r.solicitaCuenta === false;
              const iconColor = noSolicito ? '#9CA3AF' : '#8B5CF6';
              const tooltipText = noSolicito
                ? 'Paciente no solicitó cuenta'
                : 'Crear usuario';
              const handleIconClick = () => {
                if (esAdmin) {
                  if (noSolicito) {
                    setCreateUserPaciente(r);
                    setCreateUserModalOpen(true);
                  } else {
                    setCreateUserPaciente(r);
                    setCreateUserModalOpen(true);
                  }
                } else if (esAtencion) {
                  if (noSolicito) {
                    setNotifyPaciente(r);
                    setNotifyModalOpen(true);
                  } else {
                    message.info('Solo el administrador puede crear usuarios');
                  }
                }
              };
              return (
                <Tooltip title={tooltipText}>
                  <Button type="text" size="small" icon={<UserOutlined />} style={{ color: iconColor }} onClick={handleIconClick} />
                </Tooltip>
              );
            })()
          )}
          {esAdmin && <Button type="text" size="small" icon={<DeleteOutlined />} style={{ color: '#EF4444' }} onClick={() => handleDelete(r.id!)} />}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(0,212,170,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#00D4AA', fontSize: 20 }}>
              <TeamOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Pacientes</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Gestión de pacientes de la clínica</Text>
            </div>
          </Space>
        </div>
        <Space>
          <SearchInput.Search placeholder="Buscar por DNI o nombre del paciente" allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          {puedeEditar && <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nuevo Paciente</Button>}
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'auto' }}>
        {pacientesError ? (
          <ErrorAlert
            message="No se pudieron cargar los pacientes"
            description="El servicio de pacientes no está disponible. Por favor intenta más tarde."
            onRetry={() => queryClient.invalidateQueries({ queryKey: ['pacientes'] })}
          />
        ) : (
          <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={loading} scroll={{ x: 550 }}
            pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false, size: 'small' }} />
        )}
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Detalle del Paciente</Text>}
        open={detailOpen} onCancel={() => setDetailOpen(false)} footer={[<Button onClick={() => setDetailOpen(false)}>Cerrar</Button>]}
        width={Math.min(680, typeof window !== 'undefined' && window.innerWidth < 768 ? window.innerWidth - 32 : 680)}
        destroyOnClose styles={{ body: { padding: '24px 28px' } }}>
        {detailPaciente && <>
          <Title level={5} style={{ color: 'var(--text-primary)', marginBottom: 16 }}>Datos Personales</Title>
          <div className="detail-grid-2col">
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Nombre</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.nombres} {detailPaciente.apellidoPaterno}{detailPaciente.apellidoMaterno ? ' ' + detailPaciente.apellidoMaterno : ''}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>DNI</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.dni}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Email</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.email || '-'}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Teléfono</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.telefono || '-'}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Fecha Nac.</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.fechaNacimiento ? dayjs(detailPaciente.fechaNacimiento).format('DD/MM/YYYY') : '-'}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Género</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.genero ? (detailPaciente.genero === 'MASCULINO' ? 'Masculino' : detailPaciente.genero === 'FEMENINO' ? 'Femenino' : detailPaciente.genero) : '-'}</Text></div>
            <div style={{ gridColumn: '1 / -1' }}><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Dirección</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.direccion || '-'}</Text></div>
          </div>

          <Divider style={{ margin: '16px 0' }} />
          <Title level={5} style={{ color: 'var(--text-primary)', marginBottom: 16 }}>Información Médica</Title>
          <div className="detail-grid-2col">
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Alergias</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.alergias || 'Ninguna'}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Condiciones</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.condiciones || 'Ninguna'}</Text></div>
            <div style={{ gridColumn: '1 / -1' }}><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Antecedentes Familiares</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.antecedentesFamiliares || 'Ninguno'}</Text></div>
            <div style={{ gridColumn: '1 / -1' }}><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Medicamentos Actuales</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.medicamentosActual || 'Ninguno'}</Text></div>
          </div>

          <Divider style={{ margin: '16px 0' }} />
          <Title level={5} style={{ color: 'var(--text-primary)', marginBottom: 16 }}>Información del Seguro</Title>
          {detailPaciente.nombreSeguro ? (
            <div className="detail-grid-2col">
              <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Nombre</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.nombreSeguro}</Text></div>
              <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>N° Póliza</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.numeroPoliza || '-'}</Text></div>
              <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Vigencia</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.vigenciaSeguro ? dayjs(detailPaciente.vigenciaSeguro).format('DD/MM/YYYY') : '-'}</Text></div>
            </div>
          ) : (
            <Text style={{ color: 'var(--text-muted)' }}>Sin seguro registrado</Text>
          )}

          <Divider style={{ margin: '16px 0' }} />
          <Title level={5} style={{ color: 'var(--text-primary)', marginBottom: 16 }}>
            <DollarOutlined style={{ marginRight: 8 }} />Deudas y Pagos
          </Title>
          {loadingCobros ? (
            <Text style={{ color: 'var(--text-muted)' }}>Cargando...</Text>
          ) : !cobrosPaciente || cobrosPaciente.length === 0 ? (
            <Text style={{ color: 'var(--text-muted)' }}>Sin movimientos registrados</Text>
          ) : (
            <>
              <div className="resumen-gastos">
                <div className="gasto-card" style={{ borderLeft: '3px solid #EF4444' }}>
                  <Text style={{ color: 'var(--text-muted)', fontSize: 11 }}>Pendiente</Text>
                  <Text style={{ color: '#EF4444', fontSize: 20, fontWeight: 700 }}>S/{deudasResumen.pendientes.toFixed(2)}</Text>
                </div>
                <div className="gasto-card" style={{ borderLeft: '3px solid #F59E0B' }}>
                  <Text style={{ color: 'var(--text-muted)', fontSize: 11 }}>Esta Semana</Text>
                  <Text style={{ color: '#F59E0B', fontSize: 20, fontWeight: 700 }}>S/{deudasResumen.semanal.toFixed(2)}</Text>
                </div>
                <div className="gasto-card" style={{ borderLeft: '3px solid #00D4AA' }}>
                  <Text style={{ color: 'var(--text-muted)', fontSize: 11 }}>Este Mes</Text>
                  <Text style={{ color: '#00D4AA', fontSize: 20, fontWeight: 700 }}>S/{deudasResumen.mensual.toFixed(2)}</Text>
                </div>
              </div>
              <div style={{ maxHeight: 200, overflowY: 'auto', marginTop: 12 }}>
                {cobrosPaciente.slice().reverse().map(c => (
                  <div key={c.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid var(--border-soft)' }}>
                    <div>
                      <Text style={{ color: 'var(--text-primary)', fontSize: 13 }}>{c.descripcion || c.tipo || 'Cobro'}</Text>
                      <br /><Text style={{ color: 'var(--text-muted)', fontSize: 11 }}>{c.fechaCobro ? dayjs(c.fechaCobro).format('DD/MM/YYYY') : '-'}</Text>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <Text style={{ color: 'var(--text-primary)', fontWeight: 600, fontSize: 14 }}>S/{c.monto?.toFixed(2)}</Text>
                      <br /><Tag color={c.estado === 'PAGADO' ? 'green' : c.estado === 'PENDIENTE' ? 'orange' : 'red'} style={{ fontSize: 10, margin: 0 }}>{c.estado}</Tag>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </>}
      </Modal>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{editing ? 'Editar Paciente' : 'Nuevo Paciente'}</Text>}
        open={modalOpen} onCancel={closeModal} onOk={() => form.submit()} okText={editing ? 'Actualizar' : 'Crear'} width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={handleCreateOrEdit} initialValues={editing || {}} preserve={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="nombres" label="Nombres" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Nombres" />
            </Form.Item>
            <Form.Item name="apellidoPaterno" label="Apellido Paterno" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Apellido paterno" />
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="apellidoMaterno" label="Apellido Materno" style={{ width: '50%' }}>
              <Input placeholder="Apellido materno" />
            </Form.Item>
            <Form.Item name="dni" label="DNI" rules={[{ required: true, pattern: /^\d{8}$/, message: 'DNI debe tener exactamente 8 dígitos numéricos' }]} style={{ width: '50%' }}>
              <Input placeholder="12345678" maxLength={8} onChange={e => handleDniChange(e.target.value)} suffix={reniecLoading ? <SearchOutlined /> : null} />
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="fechaNacimiento" label="Fecha de Nacimiento" style={{ width: '50%' }}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="genero" label="Género" style={{ width: '50%' }}>
              <Select placeholder="Seleccionar">
                <Select.Option value="MASCULINO">Masculino</Select.Option>
                <Select.Option value="FEMENINO">Femenino</Select.Option>
                <Select.Option value="OTRO">Otro</Select.Option>
              </Select>
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="email" label="Email" rules={[{ type: 'email' }]} style={{ width: '50%' }}>
              <Input placeholder="email@ejemplo.com" />
            </Form.Item>
            <Form.Item name="telefono" label="Teléfono" style={{ width: '50%' }}>
              <PhoneInput />
            </Form.Item>
          </div>
          <Form.Item name="direccion" label="Dirección">
            <Input.TextArea rows={2} placeholder="Dirección completa" />
          </Form.Item>
          <details style={{ marginBottom: 16, cursor: 'pointer', color: 'var(--text-secondary)' }}>
            <summary style={{ fontSize: 13, fontWeight: 600 }}>Información médica adicional</summary>
            <div style={{ marginTop: 12 }}>
              <Form.Item name="alergias" label="Alergias">
                <Input placeholder="Alergias conocidas" />
              </Form.Item>
              <div style={{ display: 'flex', gap: 16 }}>
                <Form.Item name="antecedentesFamiliares" label="Antecedentes Familiares" style={{ width: '50%' }}>
                  <Input.TextArea rows={2} placeholder="Antecedentes familiares" />
                </Form.Item>
                <Form.Item name="condiciones" label="Condiciones Preexistentes" style={{ width: '50%' }}>
                  <Input placeholder="Condiciones médicas" />
                </Form.Item>
              </div>
              <Form.Item name="medicamentosActual" label="Medicamentos Actuales">
                <Input placeholder="Medicamentos que toma actualmente" />
              </Form.Item>
            </div>
          </details>
          <details style={{ marginBottom: 16, cursor: 'pointer', color: 'var(--text-secondary)' }}>
            <summary style={{ fontSize: 13, fontWeight: 600 }}>Información del Seguro</summary>
            <div style={{ marginTop: 12 }}>
              <div style={{ display: 'flex', gap: 16 }}>
                <Form.Item name="nombreSeguro" label="Nombre del Seguro" style={{ width: '50%' }}>
                  <Input placeholder="Nombre del seguro" />
                </Form.Item>
                <Form.Item name="numeroPoliza" label="N° Póliza" style={{ width: '50%' }}>
                  <Input placeholder="Número de póliza" />
                </Form.Item>
              </div>
              <Form.Item name="vigenciaSeguro" label="Vigencia del Seguro">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </div>
          </details>
        </Form>
      </Modal>

      {/* Modal: ¿El paciente solicita cuenta? (solo ATENCION_CLIENTE después de crear) */}
      <Modal
        title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Registro de Cuenta</Text>}
        open={solicitaCuentaModalOpen}
        onCancel={() => { setSolicitaCuentaModalOpen(false); setSolicitaCuentaPaciente(null); }}
        footer={null}
        centered
        destroyOnClose
        width={480}
        styles={{ body: { padding: '28px', textAlign: 'center' } }}
      >
        <div style={{ marginBottom: 24 }}>
          <div style={{
            width: 64, height: 64, borderRadius: '50%', margin: '0 auto 16px',
            background: 'rgba(139,92,246,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28, color: '#8B5CF6'
          }}>
            <UserAddOutlined />
          </div>
          <Title level={4} style={{ margin: '0 0 8px' }}>
            ¿El paciente solicita una cuenta de usuario?
          </Title>
          <Text style={{ color: 'var(--text-muted)', fontSize: 14 }}>
            {solicitaCuentaPaciente?.nombres} {solicitaCuentaPaciente?.apellidoPaterno} - DNI: {solicitaCuentaPaciente?.dni}
          </Text>
          <div style={{ marginTop: 8 }}>
            <Text style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
              Si el paciente desea acceder al sistema, selecciona "Sí" para que el administrador cree su cuenta.
              En caso contrario, selecciona "No" y el sistema lo registrará sin usuario.
            </Text>
          </div>
        </div>
        <Space size={16} style={{ width: '100%', justifyContent: 'center' }}>
          <Button
            size="large"
            icon={<CloseCircleOutlined />}
            onClick={() => handleSolicitaCuentaConfirm(false)}
            loading={solicitaCuentaLoading}
            style={{ borderRadius: 10, height: 48, minWidth: 140, fontWeight: 500 }}
          >
            No, solo registrar
          </Button>
          <Button
            type="primary"
            size="large"
            icon={<CheckCircleOutlined />}
            onClick={() => handleSolicitaCuentaConfirm(true)}
            loading={solicitaCuentaLoading}
            style={{
              borderRadius: 10, height: 48, minWidth: 140, fontWeight: 600,
              background: 'linear-gradient(135deg, #8B5CF6, #7C3AED)', border: 'none'
            }}
          >
            Sí, solicita cuenta
          </Button>
        </Space>
      </Modal>

      {/* Modal: ADMIN crea cuenta de usuario */}
      <Modal
        title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Crear Cuenta de Usuario</Text>}
        open={createUserModalOpen}
        onCancel={() => { setCreateUserModalOpen(false); setCreateUserPaciente(null); }}
        footer={null}
        centered
        destroyOnClose
        width={500}
        styles={{ body: { padding: '28px', textAlign: 'center' } }}
      >
        {(() => {
          const p = createUserPaciente;
          if (!p) return null;
          const noSolicito = p.solicitaCuenta === false;
          const nombreCompleto = `${p.nombres} ${p.apellidoPaterno}${p.apellidoMaterno ? ' ' + p.apellidoMaterno : ''}`;
          return (
            <>
              <div style={{ marginBottom: 20 }}>
                <div style={{
                  width: 64, height: 64, borderRadius: '50%', margin: '0 auto 16px',
                  background: noSolicito ? 'rgba(156,163,175,0.1)' : 'rgba(139,92,246,0.1)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28,
                  color: noSolicito ? '#9CA3AF' : '#8B5CF6'
                }}>
                  <UserOutlined />
                </div>
                <Title level={4} style={{ margin: '0 0 4px' }}>{nombreCompleto}</Title>
                <Text style={{ color: 'var(--text-muted)', fontSize: 13 }}>DNI: {p.dni} | {p.email || 'Sin email'}</Text>
              </div>

              {noSolicito && (
                <div style={{
                  padding: '12px 16px', borderRadius: 8, marginBottom: 20,
                  background: 'rgba(251,191,36,0.08)', border: '1px solid rgba(251,191,36,0.2)',
                }}>
                  <Space>
                    <BellOutlined style={{ color: '#F59E0B', fontSize: 16 }} />
                    <Text style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
                      Este paciente no solicitó activar su cuenta. Puedes crear su cuenta de todas formas si lo deseas.
                    </Text>
                  </Space>
                </div>
              )}

              {!noSolicito && (
                <div style={{
                  padding: '12px 16px', borderRadius: 8, marginBottom: 20,
                  background: 'rgba(0,212,170,0.08)', border: '1px solid rgba(0,212,170,0.2)',
                }}>
                  <Space>
                    <CheckCircleOutlined style={{ color: '#00D4AA', fontSize: 16 }} />
                    <Text style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
                      El paciente solicitó una cuenta. Se enviará un correo con su usuario y código de verificación.
                    </Text>
                  </Space>
                </div>
              )}

              <Text style={{ color: 'var(--text-muted)', fontSize: 13, display: 'block', marginBottom: 24 }}>
                Se generará un username automáticamente (P + DNI) y se enviará un correo a <strong>{p.email}</strong> con las instrucciones para completar el registro.
              </Text>

              <Space size={16} style={{ width: '100%', justifyContent: 'center' }}>
                <Button
                  size="large"
                  onClick={() => { setCreateUserModalOpen(false); setCreateUserPaciente(null); }}
                  disabled={createUserLoading}
                  style={{ borderRadius: 10, height: 48, minWidth: 140, fontWeight: 500 }}
                >
                  Cancelar
                </Button>
                <Button
                  type="primary"
                  size="large"
                  icon={<UserAddOutlined />}
                  loading={createUserLoading}
                  onClick={() => {
                    if (p.id) {
                      setCreateUserLoading(true);
                      registroPacienteMutation.mutate(p.id, {
                        onSettled: () => setCreateUserLoading(false),
                      });
                    }
                  }}
                  style={{
                    borderRadius: 10, height: 48, minWidth: 180, fontWeight: 600,
                    background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none'
                  }}
                >
                  Crear cuenta y enviar correo
                </Button>
              </Space>
            </>
          );
        })()}
      </Modal>

      {/* Modal: ATENCION_CLIENTE notifica que paciente quiere cuenta */}
      <Modal
        title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Notificar Solicitud de Cuenta</Text>}
        open={notifyModalOpen}
        onCancel={() => { setNotifyModalOpen(false); setNotifyPaciente(null); }}
        footer={null}
        centered
        destroyOnClose
        width={480}
        styles={{ body: { padding: '28px', textAlign: 'center' } }}
      >
        <div style={{ marginBottom: 24 }}>
          <div style={{
            width: 64, height: 64, borderRadius: '50%', margin: '0 auto 16px',
            background: 'rgba(251,191,36,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28, color: '#F59E0B'
          }}>
            <BellOutlined />
          </div>
          <Title level={4} style={{ margin: '0 0 8px' }}>
            ¿El paciente ahora desea una cuenta?
          </Title>
          <Text style={{ color: 'var(--text-muted)', fontSize: 14 }}>
            {notifyPaciente?.nombres} {notifyPaciente?.apellidoPaterno} - DNI: {notifyPaciente?.dni}
          </Text>
          <div style={{ marginTop: 12 }}>
            <Text style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
              Al confirmar, se notificará al administrador que este paciente solicita una cuenta de usuario.
              El icono cambiará a color morado y el administrador podrá crear la cuenta.
            </Text>
          </div>
        </div>
        <Space size={16} style={{ width: '100%', justifyContent: 'center' }}>
          <Button
            size="large"
            onClick={() => { setNotifyModalOpen(false); setNotifyPaciente(null); }}
            disabled={notifyLoading}
            style={{ borderRadius: 10, height: 48, minWidth: 120, fontWeight: 500 }}
          >
            Cancelar
          </Button>
          <Button
            type="primary"
            size="large"
            icon={<BellOutlined />}
            loading={notifyLoading}
            onClick={handleNotifyChange}
            style={{
              borderRadius: 10, height: 48, minWidth: 160, fontWeight: 600,
              background: 'linear-gradient(135deg, #F59E0B, #D97706)', border: 'none'
            }}
          >
            Notificar
          </Button>
        </Space>
      </Modal>
    </div>
  );
}