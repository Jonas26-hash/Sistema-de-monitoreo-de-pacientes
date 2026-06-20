import { useState, useEffect, useRef } from 'react';
import { Card, Form, Input, Button, Avatar, Typography, message, Space, Divider, Modal, DatePicker, Select } from 'antd';
import { UserOutlined, MailOutlined, IdcardOutlined, CameraOutlined, LockOutlined, EnvironmentOutlined, CalendarOutlined, MedicineBoxOutlined } from '@ant-design/icons';
import PhoneInput from '../../components/common/PhoneInput';
import { showCrudSuccess } from '../../utils/notifications';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import type { Paciente } from '../../types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function Perfil() {
  const [form] = Form.useForm();
  const [passwordForm] = Form.useForm();
  const { user, token, updateUser } = useAuth();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [fullPaciente, setFullPaciente] = useState<Paciente | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [avatar, setAvatar] = useState<string>(() => localStorage.getItem('profile_avatar') || '');

  useEffect(() => {
    if (user) {
      setLoading(true);
      api.get('/auth/profile')
        .then((res) => {
          const me = res.data;
          form.setFieldsValue({
            nombres: me.nombres || '',
            apellidos: me.apellidos || '',
            email: me.email || '',
            dni: me.dni || '',
            telefono: me.telefono || '',
            especialidad: me.especialidad || '',
            username: me.username,
            fechaNacimiento: me.fechaNacimiento ? dayjs(me.fechaNacimiento) : null,
            genero: me.genero || undefined,
            direccion: me.direccion || '',
          });
          if (me.pacienteId) {
            api.get(`/pacientes/${me.pacienteId}`)
              .then((res) => setFullPaciente(res.data))
              .catch(() => {});
          }
        })
        .catch(() => message.error('Error al cargar datos'))
        .finally(() => setLoading(false));
    }
  }, [user, form]);

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const dataUrl = ev.target?.result as string;
      setAvatar(dataUrl);
      localStorage.setItem('profile_avatar', dataUrl);
    };
    reader.readAsDataURL(file);
  };

  const handleSave = async (values: Record<string, string>) => {
    setSaving(true);
    try {
      const fechaNacimiento = values.fechaNacimiento
        ? dayjs(values.fechaNacimiento).format('YYYY-MM-DD')
        : null;
      await api.put('/auth/profile', {
        nombres: values.nombres,
        apellidos: values.apellidos,
        email: values.email,
        dni: values.dni || null,
        telefono: values.telefono || null,
        especialidad: values.especialidad || null,
        fechaNacimiento,
        genero: values.genero || null,
        direccion: values.direccion || null,
      });
      updateUser({ email: values.email });
      const fullName = `${values.nombres} ${values.apellidos}`;
      localStorage.setItem('user_display_name', fullName);
      showCrudSuccess('actualizado', 'Perfil');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Error al actualizar';
      message.error(msg);
    } finally {
      setSaving(false);
    }
  };

  const handleChangePassword = async (values: { oldPassword: string; newPassword: string; confirmPassword: string }) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('Las contraseñas nuevas no coinciden');
      return;
    }
    setPasswordSaving(true);
    try {
      await api.put('/auth/change-password', {
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      showCrudSuccess('actualizado', 'Contraseña');
      setPasswordModalOpen(false);
      passwordForm.resetFields();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Error al cambiar contraseña';
      message.error(msg);
    } finally {
      setPasswordSaving(false);
    }
  };

  return (
    <div style={{ maxWidth: 720, margin: '0 auto' }}>
      <div style={{ marginBottom: 28 }}>
        <Title level={4} style={{ color: 'var(--text-primary)', margin: 0, fontWeight: 700 }}>Mi Perfil</Title>
        <Text style={{ color: 'var(--text-muted)', fontSize: 14 }}>Actualiza tus datos personales</Text>
      </div>

      <Card className="glass" style={{ borderRadius: 16 }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{ position: 'relative', display: 'inline-block' }}>
            <Avatar size={100} src={avatar || undefined}
              style={{ background: avatar ? 'transparent' : 'linear-gradient(135deg, #00D4AA, #059669)', fontSize: 40, fontWeight: 700, boxShadow: '0 10px 22px rgba(14,165,164,0.24)' }}>
              {!avatar ? (user?.username?.charAt(0).toUpperCase() || '?') : null}
            </Avatar>
            <div onClick={() => fileInputRef.current?.click()} style={{
              position: 'absolute', bottom: 0, right: 0, width: 32, height: 32, borderRadius: '50%',
              background: 'var(--brand-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer', border: '3px solid var(--bg-body)', color: '#fff', fontSize: 14,
            }}>
              <CameraOutlined />
            </div>
            <input ref={fileInputRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handleAvatarChange} />
          </div>
          <div style={{ marginTop: 12 }}>
            <Text style={{ color: 'var(--text-primary)', fontWeight: 600, fontSize: 16, display: 'block' }}>{user?.username}</Text>
            <Text style={{ color: 'var(--text-muted)', fontSize: 13 }}>{user?.roles?.join(', ')}</Text>
          </div>
        </div>

        <Form form={form} layout="vertical" onFinish={handleSave} size="large" requiredMark={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="nombres" label="Nombres" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Nombres" style={{ borderRadius: 10 }} />
            </Form.Item>
            <Form.Item name="apellidos" label="Apellidos" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Apellidos" style={{ borderRadius: 10 }} />
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="dni" label="DNI" rules={[{ pattern: /^\d{8}$/, message: 'DNI debe tener exactamente 8 dígitos numéricos' }]} style={{ width: '50%' }}>
              <Input prefix={<IdcardOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="12345678" maxLength={8} style={{ borderRadius: 10 }} />
            </Form.Item>
            <Form.Item name="telefono" label="Teléfono" style={{ width: '50%' }}>
              <PhoneInput />
            </Form.Item>
          </div>
          <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]}>
            <Input prefix={<MailOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="email@clinica.com" style={{ borderRadius: 10 }} />
          </Form.Item>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="fechaNacimiento" label="Fecha de Nacimiento" style={{ width: '50%' }}>
              <DatePicker style={{ width: '100%', borderRadius: 10 }} placeholder="Seleccione fecha" />
            </Form.Item>
            <Form.Item name="genero" label="Género" style={{ width: '50%' }}>
              <Select placeholder="Seleccione género" allowClear style={{ borderRadius: 10 }}>
                <Select.Option value="MASCULINO">Masculino</Select.Option>
                <Select.Option value="FEMENINO">Femenino</Select.Option>
                <Select.Option value="OTRO">Otro</Select.Option>
              </Select>
            </Form.Item>
          </div>
          <Form.Item name="direccion" label="Dirección">
            <Input.TextArea rows={2} placeholder="Dirección" style={{ borderRadius: 10 }} />
          </Form.Item>
          <Form.Item name="especialidad" label="Especialidad">
            <Input placeholder="Ej: Cardiología" style={{ borderRadius: 10 }} />
          </Form.Item>
          <Divider />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Button icon={<LockOutlined />} onClick={() => setPasswordModalOpen(true)}
              style={{ height: 44, borderRadius: 10, fontWeight: 500 }}>
              Cambiar Contraseña
            </Button>
            <Button type="primary" htmlType="submit" loading={saving}
              style={{ height: 44, borderRadius: 10, fontWeight: 600, minWidth: 200, background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none' }}>
              Guardar Cambios
            </Button>
          </div>
        </Form>
      </Card>

      {fullPaciente && (
        <Card className="glass" style={{ borderRadius: 16, marginTop: 24 }}>
          <Title level={5} style={{ color: 'var(--text-primary)', marginBottom: 16 }}>🩺 Información Médica</Title>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px 24px', marginBottom: 16 }}>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Alergias</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{fullPaciente.alergias || 'Ninguna registrada'}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Condiciones Preexistentes</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{fullPaciente.condiciones || 'Ninguna registrada'}</Text></div>
            <div style={{ gridColumn: '1 / -1' }}><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Antecedentes Familiares</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{fullPaciente.antecedentesFamiliares || 'Ninguno registrado'}</Text></div>
            <div style={{ gridColumn: '1 / -1' }}><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Medicamentos Actuales</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{fullPaciente.medicamentosActual || 'Ninguno registrado'}</Text></div>
          </div>

          <Divider style={{ margin: '16px 0' }} />
          <Title level={5} style={{ color: 'var(--text-primary)', marginBottom: 16 }}>🔒 Información del Seguro</Title>
          {fullPaciente.nombreSeguro ? (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px 24px' }}>
              <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Nombre del Seguro</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{fullPaciente.nombreSeguro}</Text></div>
              <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>N° Póliza</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{fullPaciente.numeroPoliza || '-'}</Text></div>
              <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Vigencia</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{fullPaciente.vigenciaSeguro ? dayjs(fullPaciente.vigenciaSeguro).format('DD/MM/YYYY') : '-'}</Text></div>
            </div>
          ) : (
            <Text style={{ color: 'var(--text-muted)' }}>Sin seguro registrado</Text>
          )}
        </Card>
      )}

      <Modal
        title="Cambiar Contraseña"
        open={passwordModalOpen}
        onCancel={() => { setPasswordModalOpen(false); passwordForm.resetFields(); }}
        footer={null}
        centered
        destroyOnClose
        width={440}
        styles={{ body: { paddingTop: 8 } }}
      >
        <Form form={passwordForm} layout="vertical" onFinish={handleChangePassword} size="large" requiredMark={false}>
          <Form.Item name="oldPassword" label="Contraseña actual" rules={[{ required: true, message: 'Ingresa tu contraseña actual' }]}>
            <Input.Password placeholder="••••••••" style={{ borderRadius: 10 }} />
          </Form.Item>
          <Form.Item name="newPassword" label="Nueva contraseña" rules={[
            { required: true, message: 'Ingresa la nueva contraseña' },
            { min: 6, message: 'Mínimo 6 caracteres' },
          ]}>
            <Input.Password placeholder="••••••••" style={{ borderRadius: 10 }} />
          </Form.Item>
          <Form.Item name="confirmPassword" label="Confirmar nueva contraseña" rules={[
            { required: true, message: 'Confirma la nueva contraseña' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                return Promise.reject(new Error('Las contraseñas no coinciden'));
              },
            }),
          ]}>
            <Input.Password placeholder="••••••••" style={{ borderRadius: 10 }} />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Button onClick={() => { setPasswordModalOpen(false); passwordForm.resetFields(); }}
              style={{ marginRight: 8, borderRadius: 10, height: 44, fontWeight: 500 }}>
              Cancelar
            </Button>
            <Button type="primary" htmlType="submit" loading={passwordSaving}
              style={{ borderRadius: 10, height: 44, fontWeight: 600, background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none' }}>
              Cambiar Contraseña
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
