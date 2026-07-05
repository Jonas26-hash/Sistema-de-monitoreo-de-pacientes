import { useState } from 'react';
import { Table, Button, Modal, Form, Input, Select, Tag, Space, Input as SearchInput, Typography, message, App } from 'antd';
import { PlusOutlined, EditOutlined, StopOutlined, CheckCircleOutlined, SearchOutlined, UserOutlined, MailOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useCrud } from '../../hooks/useCrud';
import { showCrudSuccess } from '../../utils/notifications';
import type { User } from '../../types';
import api from '../../services/api';
import SuccessModal from '../../components/common/SuccessModal';
import PhoneInput from '../../components/common/PhoneInput';

const { Title, Text } = Typography;

const roleColors: Record<string, string> = {
  ADMIN: '#EF4444', DOCTOR: '#3B82F6', FARMACEUTICO: '#F59E0B',
  ATENCION_CLIENTE: '#8B5CF6', ENFERMERO: '#00D4AA', PACIENTE: '#06B6D4',
};

export default function Usuarios() {
  const [form] = Form.useForm();
  const { search, setSearch, data, loading, page, setPage, modalOpen, editing, openCreate, openEdit, closeModal, handleSave } = useCrud<User>({
    key: 'usuarios',
    endpoint: '/auth/usuarios',
    createEndpoint: '/auth/register',
  });

  const [reniecLoading, setReniecLoading] = useState(false);
  const [successModal, setSuccessModal] = useState<{ open: boolean; title: string; subtitle?: string; lottieSrc?: string }>({ open: false, title: '' });
  const [creatingWorker, setCreatingWorker] = useState(false);
  const queryClient = useQueryClient();
  const { modal: appModal } = App.useApp();

  const showDisableConfirm = (user: User) => {
    appModal.confirm({
      title: 'Deshabilitar Usuario',
      icon: <ExclamationCircleOutlined style={{ color: '#EF4444' }} />,
      content: (
        <Typography.Text>
          ¿Estás seguro de deshabilitar a <strong>{user.nombres} {user.apellidos}</strong>?
          El usuario no podrá acceder al sistema hasta que sea reactivado por un administrador.
        </Typography.Text>
      ),
      okText: 'Deshabilitar',
      okType: 'danger',
      cancelText: 'Cancelar',
      centered: true,
      onOk: async () => {
        try {
          await api.delete(`/auth/usuarios/${user.id}`);
          message.success(`Usuario "${user.nombres} ${user.apellidos}" desactivado`);
          queryClient.invalidateQueries({ queryKey: ['usuarios'] });
        } catch (err: unknown) {
          const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Error al deshabilitar usuario';
          message.error(msg);
        }
      },
    });
  };

  const showReactivateConfirm = (user: User) => {
    appModal.confirm({
      title: 'Reactivar Usuario',
      icon: <CheckCircleOutlined style={{ color: '#10B981' }} />,
      content: (
        <Typography.Text>
          ¿Estás seguro de reactivar a <strong>{user.nombres} {user.apellidos}</strong>?
          El usuario podrá acceder nuevamente al sistema.
        </Typography.Text>
      ),
      okText: 'Reactivar',
      okType: 'primary',
      cancelText: 'Cancelar',
      centered: true,
      onOk: async () => {
        try {
          await api.put(`/auth/pendientes/${user.id}/aprobar`);
          message.success(`Usuario "${user.nombres} ${user.apellidos}" reactivado`);
          queryClient.invalidateQueries({ queryKey: ['usuarios'] });
        } catch (err: unknown) {
          const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Error al reactivar usuario';
          message.error(msg);
        }
      },
    });
  };

  const columns = [
    { title: 'Nº', key: 'index', width: 60, render: (_v: unknown, _r: unknown, i: number) => <Text style={{ color: 'var(--text-muted)' }}>{i + 1}</Text> },
    { title: 'Usuario', key: 'nombre', render: (_: unknown, r: User) => <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{r.nombres} {r.apellidos}</Text> },
    { title: 'Email', dataIndex: 'email', key: 'email', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Username', dataIndex: 'username', key: 'username', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    {
      title: 'Roles', dataIndex: 'roles', key: 'roles',
      render: (roles: string[]) => (
        <Space size={4}>
          {roles?.map((r) => (
            <Tag key={r} color={roleColors[r] || '#F59E0B'} style={{ borderRadius: 4 }}>{r}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: 'Estado', key: 'estado', width: 90,
      render: (_: unknown, r: User) => (
        r.activo === false
          ? <Tag color="error" style={{ borderRadius: 4, margin: 0 }}>Inactivo</Tag>
          : <Tag color="success" style={{ borderRadius: 4, margin: 0 }}>Activo</Tag>
      ),
    },
    {
      title: 'Acciones', key: 'acciones', width: 110,
      render: (_: unknown, r: User) => (
        <Space>
          <Button type="text" icon={<EditOutlined />} style={{ color: '#3B82F6' }} onClick={() => openEdit(r)} />
          {r.activo === false ? (
            <Button type="text" icon={<CheckCircleOutlined />} style={{ color: '#10B981' }} onClick={() => showReactivateConfirm(r)} />
          ) : (
            <Button type="text" icon={<StopOutlined />} style={{ color: '#EF4444' }} onClick={() => showDisableConfirm(r)} />
          )}
        </Space>
      ),
    },
  ];

  const handleDniChange = async (value: string) => {
    if (value.length !== 8) return;
    setReniecLoading(true);
    try {
      const res = await api.get(`/pacientes/reniec/dni/${value}`);
      if (res.data?.names) {
        form.setFieldsValue({
          nombres: res.data.names,
          apellidos: `${res.data.paternalLastName} ${res.data.maternalLastName ?? ''}`.trim(),
        });
      }
    } catch {
    } finally {
      setReniecLoading(false);
    }
  };

  const handleCreateUser = async (values: User) => {
    setCreatingWorker(true);
    try {
      if (values.rol === 'PACIENTE') {
        await api.post('/auth/register', { ...values, password: 'MedTrack2026' });
        queryClient.invalidateQueries({ queryKey: ['usuarios'] });
        showCrudSuccess('creado');
        closeModal();
        form.resetFields();
      } else {
        await api.post('/auth/pre-registro-personal', {
          email: values.email,
          nombres: values.nombres,
          apellidos: values.apellidos,
          dni: values.dni,
          telefono: values.telefono,
          rolSolicitado: values.rol,
        });
        setSuccessModal({
          open: true,
          title: 'Código Enviado',
          subtitle: `Se ha enviado un código de verificación a ${values.email}. El empleado debe revisar su correo para completar el registro.`,
          lottieSrc: '/lottie/Email successfully sent.svg',
        });
        closeModal();
        form.resetFields();
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Error al crear usuario';
      message.error(msg);
    } finally {
      setCreatingWorker(false);
    }
  };

  return (
    <div>
      <SuccessModal
        open={successModal.open}
        title={successModal.title}
        subtitle={successModal.subtitle}
        lottieSrc={successModal.lottieSrc}
        onClose={() => setSuccessModal({ open: false, title: '' })}
        duration={4000}
      />
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(239,68,68,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#EF4444', fontSize: 20 }}>
              <UserOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Usuarios</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Gestión de usuarios del sistema</Text>
            </div>
          </Space>
        </div>
        <Space>
          <SearchInput.Search placeholder="Buscar por DNI, nombre o usuario" allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nuevo Usuario</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'auto' }}>
        <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={loading} scroll={{ x: 650 }}
          pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false }} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{editing ? 'Editar Usuario' : 'Nuevo Usuario'}</Text>}
        open={modalOpen} onCancel={closeModal} onOk={() => form.submit()} okText={editing ? 'Actualizar' : 'Crear'} width={640} destroyOnClose
        okButtonProps={{ loading: creatingWorker, disabled: creatingWorker }}
        cancelButtonProps={{ disabled: creatingWorker }}
        styles={{ body: { padding: '24px 28px', position: 'relative' } }}>
        {creatingWorker && (
          <div style={{
            position: 'absolute', inset: 0, zIndex: 10,
            background: 'color-mix(in srgb, var(--bg-card) 92%, transparent)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            borderRadius: 12,
          }}>
            <img src="/lottie/Load.svg" alt="Enviando..." style={{ width: 200, height: 'auto' }} />
          </div>
        )}
        <Form form={form} layout="vertical" onFinish={editing ? handleSave : handleCreateUser} initialValues={editing || { rol: 'DOCTOR' }} preserve={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="dni" label="DNI" rules={[{ pattern: /^\d{8}$/, message: 'DNI debe tener exactamente 8 dígitos numéricos' }]} style={{ width: '50%' }}>
              <Input placeholder="12345678" maxLength={8} onChange={e => handleDniChange(e.target.value)} suffix={reniecLoading ? <SearchOutlined /> : null} />
            </Form.Item>
            <Form.Item name="telefono" label="Teléfono" style={{ width: '50%' }}>
              <PhoneInput />
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="nombres" label="Nombres" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Nombres" />
            </Form.Item>
            <Form.Item name="apellidos" label="Apellidos" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Apellidos" />
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]} style={{ width: '50%' }}>
              <Input prefix={<MailOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="email@clinica.com" />
            </Form.Item>
            {!editing && (
              <Form.Item noStyle shouldUpdate={(prev, cur) => prev.rol !== cur.rol}>
                {({ getFieldValue }) => {
                  const rol = getFieldValue('rol');
                  return rol !== 'PACIENTE' ? null : (
                    <Form.Item name="username" label="Username" rules={[{ required: true }, { pattern: /^[a-zA-Z0-9_]{3,50}$/, message: 'Solo letras, números y guión bajo (3-50 caracteres)' }]} style={{ width: '50%' }}>
                      <Input placeholder="ej: jperez" />
                    </Form.Item>
                  );
                }}
              </Form.Item>
            )}
            {editing && (
              <Form.Item name="username" label="Username" style={{ width: '50%' }}>
                <Input disabled />
              </Form.Item>
            )}
          </div>
          {!editing && (
            <Form.Item name="rol" label="Rol" rules={[{ required: true }]}>
              <Select placeholder="Seleccionar rol">
                <Select.Option value="ADMIN">Administrador</Select.Option>
                <Select.Option value="DOCTOR">Médico</Select.Option>
                <Select.Option value="ATENCION_CLIENTE">Atención al Cliente</Select.Option>
                <Select.Option value="ENFERMERO">Enfermero(a)</Select.Option>
                <Select.Option value="FARMACEUTICO">Farmacéutico</Select.Option>
                <Select.Option value="PACIENTE">Paciente</Select.Option>
              </Select>
            </Form.Item>
          )}
          {editing && (
            <Form.Item name="roles" label="Roles" rules={[{ required: true }]}>
              <Select mode="multiple" placeholder="Seleccionar roles">
                <Select.Option value="ADMIN">Administrador</Select.Option>
                <Select.Option value="DOCTOR">Médico</Select.Option>
                <Select.Option value="ATENCION_CLIENTE">Atención al Cliente</Select.Option>
                <Select.Option value="ENFERMERO">Enfermero(a)</Select.Option>
                <Select.Option value="FARMACEUTICO">Farmacéutico</Select.Option>
                <Select.Option value="PACIENTE">Paciente</Select.Option>
              </Select>
            </Form.Item>
          )}
          {!editing && (
            <Form.Item noStyle shouldUpdate={(prev, cur) => prev.rol !== cur.rol}>
              {({ getFieldValue }) => {
                const rol = getFieldValue('rol');
                return rol === 'PACIENTE' ? null : (
                  <div style={{
                    padding: '12px 16px', borderRadius: 8,
                    background: 'rgba(59,130,246,0.06)', border: '1px solid rgba(59,130,246,0.15)',
                    marginTop: 8,
                  }}>
                    <Space>
                      <MailOutlined style={{ color: '#3B82F6', fontSize: 16 }} />
                      <Text style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
                        Al crear un trabajador, se enviará un código de verificación a su email para que complete su registro.
                      </Text>
                    </Space>
                  </div>
                );
              }}
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
}
