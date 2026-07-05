import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Typography, Steps, message, Result } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, IdcardOutlined, KeyOutlined, SearchOutlined } from '@ant-design/icons';
import PhoneInput from '../../components/common/PhoneInput';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';

const { Title, Text } = Typography;

export default function Register() {
  const [form] = Form.useForm();
  const [reniecLoading, setReniecLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [step, setStep] = useState(0);
  const [email, setEmail] = useState('');
  const { setAuthFromToken } = useAuth();
  const navigate = useNavigate();

  const handleSubmitData = async () => {
    setLoading(true);
    try {
      const values = form.getFieldsValue(true);
      await api.post('/auth/pre-registro', {
        nombres: values.nombres,
        apellidos: values.apellidos,
        dni: values.dni || null,
        telefono: values.telefono || null,
        email: values.email,
        username: values.username,
        password: values.password,
      });
      setEmail(values.email);
      setStep(2);
    } catch (err: unknown) {
      const data = (err as { response?: { data?: Record<string, unknown> } })?.response?.data;
      const errMsg = data?.error as string
        || (data?.violations as Array<{ message: string }>)?.[0]?.message
        || (data?.message as string)
        || 'Error al enviar código de verificación';
      message.error(errMsg);
    } finally {
      setLoading(false);
    }
  };

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

  const handleVerifyCode = async (values: { codigo: string }) => {
    setVerifying(true);
    try {
      // Try verify + complete in one atomic call
      const res = await api.post('/auth/completar-registro', { email, codigo: values.codigo });
      const token = res.data?.token;
      if (token) {
        setAuthFromToken(token);
        message.success('Registro exitoso. ¡Bienvenido!');
        navigate('/', { replace: true });
      } else {
        message.success(res.data?.mensaje || 'Registro exitoso');
        navigate('/login', { replace: true });
      }
    } catch (err: unknown) {
      const data = (err as { response?: { data?: Record<string, unknown> } })?.response?.data;
      const errMsg = data?.error as string
        || (data?.violations as Array<{ message: string }>)?.[0]?.message
        || (data?.message as string)
        || 'Error al verificar código';
      message.error(errMsg);
    } finally {
      setVerifying(false);
    }
  };

  return (
    <div className="login-bg" style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <div className="login-grid" />
      <div style={{ width: '100%', maxWidth: 480, position: 'relative', zIndex: 1 }}>
        <Card className="glass-strong" style={{ borderRadius: 20, padding: '8px 0' }}
          styles={{ body: { padding: '32px 28px' } }}>
          <div style={{ textAlign: 'center', marginBottom: 28 }}>
            <img src="/Logo-1.png" alt="MT" style={{ width: 64, height: 'auto', borderRadius: 12, margin: '0 auto 12px', objectFit: 'contain', background: 'rgba(255,255,255,0.1)', padding: 8 }} />
            <Title level={4} style={{ color: 'var(--text-primary)', margin: 0, fontWeight: 700 }}>Crear Cuenta</Title>
            <Text style={{ color: 'var(--text-muted)', fontSize: 13 }}>Regístrate como paciente de la clínica</Text>
          </div>

          <Steps current={step} size="small" style={{ marginBottom: 28 }}
            items={[{ title: 'Datos' }, { title: 'Cuenta' }, { title: 'Verificar' }]} />

          {step < 2 && (
            <Form form={form} layout="vertical" onFinish={handleSubmitData} size="large" requiredMark={false}>
              {step === 0 && (
                <>
                  <Form.Item name="dni" label="DNI" rules={[{ pattern: /^\d{8}$/, message: 'DNI debe tener exactamente 8 dígitos numéricos' }]}>
                    <Input prefix={<IdcardOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="12345678" maxLength={8} onChange={e => handleDniChange(e.target.value)} suffix={reniecLoading ? <SearchOutlined /> : null} style={{ height: 48, borderRadius: 10 }} />
                  </Form.Item>
                  <Form.Item name="nombres" label="Nombres" rules={[{ required: true, message: 'Ingresa tus nombres' }]}>
                    <Input prefix={<UserOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="Tus nombres" style={{ height: 48, borderRadius: 10 }} />
                  </Form.Item>
                  <Form.Item name="apellidos" label="Apellidos" rules={[{ required: true, message: 'Ingresa tus apellidos' }]}>
                    <Input prefix={<UserOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="Tus apellidos" style={{ height: 48, borderRadius: 10 }} />
                  </Form.Item>
                  <Form.Item name="telefono" label="Teléfono">
                    <PhoneInput size="large" style={{ height: 48, borderRadius: 10, overflow: 'hidden' }} />
                  </Form.Item>
                  <Form.Item style={{ marginBottom: 0 }}>
                    <Button type="primary" block onClick={() => form.validateFields().then(() => setStep(1))}
                      style={{ height: 48, borderRadius: 10, fontWeight: 600, background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none' }}>
                      Siguiente
                    </Button>
                  </Form.Item>
                </>
              )}

              {step === 1 && (
                <>
                  <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email', message: 'Email inválido' }]}>
                    <Input prefix={<MailOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="email@ejemplo.com" style={{ height: 48, borderRadius: 10 }} />
                  </Form.Item>
                  <Form.Item name="username" label="Usuario" rules={[
                    { required: true, message: 'Elige un nombre de usuario' },
                    { pattern: /^[a-zA-Z0-9_]{3,50}$/, message: 'Solo letras, números y guión bajo (3-50 caracteres)' },
                  ]}>
                    <Input prefix={<UserOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="ej: jperez" style={{ height: 48, borderRadius: 10 }} />
                  </Form.Item>
                  <Form.Item name="password" label="Contraseña" rules={[{ required: true, min: 6, message: 'Mínimo 6 caracteres' }]}>
                    <Input.Password prefix={<LockOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="••••••••" style={{ height: 48, borderRadius: 10 }} />
                  </Form.Item>
                  <Form.Item name="confirmarPassword" label="Confirmar Contraseña" rules={[
                    { required: true, message: 'Confirma tu contraseña' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('password') === value) return Promise.resolve();
                        return Promise.reject(new Error('Las contraseñas no coinciden'));
                      },
                    }),
                  ]}>
                    <Input.Password prefix={<LockOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="••••••••" style={{ height: 48, borderRadius: 10 }} />
                  </Form.Item>
                  <div style={{ display: 'flex', gap: 12 }}>
                    <Button onClick={() => setStep(0)} style={{ height: 48, borderRadius: 10, flex: 1 }}>Atrás</Button>
                    <Button type="primary" htmlType="submit" loading={loading}
                      style={{ height: 48, borderRadius: 10, fontWeight: 600, flex: 1, background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none' }}>
                      Registrarse
                    </Button>
                  </div>
                </>
              )}
            </Form>
          )}

          {step === 2 && (
            <Form layout="vertical" onFinish={handleVerifyCode} size="large" requiredMark={false}>
              <div style={{ textAlign: 'center', marginBottom: 20 }}>
                <div style={{ width: 100, height: 80, margin: '0 auto 12px' }}>
                  <img src="/lottie/Untitled file.svg" alt="Verificar" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                </div>
                <Text style={{ color: 'var(--text-muted)', fontSize: 13, display: 'block' }}>
                  Hemos enviado un código de verificación a <strong>{email}</strong>
                </Text>
                <Text style={{ color: 'var(--text-subtle)', fontSize: 12, display: 'block', marginTop: 4 }}>
                  Revisa tu bandeja de entrada y copia el código
                </Text>
              </div>
              <Form.Item name="codigo" label="Código de Verificación" rules={[
                { required: true, message: 'Ingresa el código de 6 dígitos' },
                { len: 6, message: 'El código tiene 6 dígitos' },
              ]}>
                <Input prefix={<KeyOutlined style={{ color: 'var(--text-muted)' }} />}
                  placeholder="123456" maxLength={6} style={{ height: 48, borderRadius: 10, textAlign: 'center', fontSize: 24, letterSpacing: 8 }} />
              </Form.Item>
              <Form.Item style={{ marginTop: 8 }}>
                <Button type="primary" htmlType="submit" loading={verifying} block
                  style={{ height: 48, borderRadius: 10, fontWeight: 600, fontSize: 15,
                    background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none',
                    boxShadow: '0 4px 14px rgba(0,212,170,0.35)' }}>
                  Verificar Código
                </Button>
              </Form.Item>
              <div style={{ textAlign: 'center' }}>
                <Button type="link" onClick={() => setStep(1)} style={{ color: 'var(--text-muted)' }}>
                  Volver atrás
                </Button>
              </div>
            </Form>
          )}

          <div style={{ marginTop: 20, textAlign: 'center' }}>
            <Text style={{ color: 'var(--text-subtle)', fontSize: 13 }}>¿Ya tienes cuenta? </Text>
            <Button type="link" onClick={() => navigate('/login')} style={{ color: '#00D4AA', fontWeight: 600, padding: 0 }}>Iniciar Sesión</Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
