import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Typography, Steps, message } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined, IdcardOutlined } from '@ant-design/icons';
import api from '../../services/api';
import SuccessModal from '../../components/common/SuccessModal';

const { Title, Text } = Typography;

export default function Register() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState(0);
  const [successOpen, setSuccessOpen] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (values: Record<string, string>) => {
    setLoading(true);
    try {
      await api.post('/auth/self-register', {
        username: values.username,
        password: values.password,
        email: values.email,
        nombres: values.nombres,
        apellidos: values.apellidos,
        dni: values.dni || null,
        telefono: values.telefono || null,
      });
      setSuccessOpen(true);
      setTimeout(() => {
        setSuccessOpen(false);
        navigate('/login');
      }, 2500);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string; mensaje?: string } } })?.response?.data;
      message.error(msg?.mensaje || msg?.error || 'Error al registrarse');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-bg" style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <SuccessModal
        open={successOpen}
        title="Registro Exitoso"
        subtitle="Tu cuenta ha sido creada. Serás redirigido al inicio de sesión."
        lottieSrc="/lottie/Registro.svg"
      />
      <div className="login-grid" />
      <div style={{ width: '100%', maxWidth: 480, position: 'relative', zIndex: 1 }}>
        <Card className="glass-strong" style={{ borderRadius: 20, padding: '8px 0' }}
          styles={{ body: { padding: '32px 28px' } }}>
          <div style={{ textAlign: 'center', marginBottom: 28 }}>
            <img src="/Logo-1.png" alt="MT" style={{ width: 64, height: 'auto', borderRadius: 12, margin: '0 auto 12px', objectFit: 'contain', background: 'rgba(255,255,255,0.1)', padding: 8 }} />
            <Title level={4} style={{ color: 'var(--text-primary)', margin: 0, fontWeight: 700 }}>Crear Cuenta</Title>
            <Text style={{ color: 'var(--text-muted)', fontSize: 13 }}>Regístrate como paciente del hospital</Text>
          </div>

          <Steps current={step} size="small" style={{ marginBottom: 28 }}
            items={[{ title: 'Datos' }, { title: 'Cuenta' }]} />

          <Form form={form} layout="vertical" onFinish={handleSubmit} size="large" requiredMark={false}>
            {step === 0 && (
              <>
                <Form.Item name="nombres" label="Nombres" rules={[{ required: true, message: 'Ingresa tus nombres' }]}>
                  <Input prefix={<UserOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="Tus nombres" style={{ height: 48, borderRadius: 10 }} />
                </Form.Item>
                <Form.Item name="apellidos" label="Apellidos" rules={[{ required: true, message: 'Ingresa tus apellidos' }]}>
                  <Input prefix={<UserOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="Tus apellidos" style={{ height: 48, borderRadius: 10 }} />
                </Form.Item>
                <Form.Item name="dni" label="DNI" rules={[{ pattern: /^\d{8}$/, message: 'DNI debe tener exactamente 8 dígitos numéricos' }]}>
                  <Input prefix={<IdcardOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="12345678" maxLength={8} style={{ height: 48, borderRadius: 10 }} />
                </Form.Item>
                <Form.Item name="telefono" label="Teléfono">
                  <Input prefix={<PhoneOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="+51 999 999 999" style={{ height: 48, borderRadius: 10 }} />
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
                <Form.Item name="username" label="Usuario" rules={[{ required: true, message: 'Elige un nombre de usuario' }]}>
                  <Input prefix={<UserOutlined style={{ color: 'var(--text-muted)' }} />} placeholder="ej: jperez" style={{ height: 48, borderRadius: 10 }} />
                </Form.Item>
                <Form.Item name="password" label="Contraseña" rules={[{ required: true, min: 6, message: 'Mínimo 6 caracteres' }]}>
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

          <div style={{ marginTop: 20, textAlign: 'center' }}>
            <Text style={{ color: 'var(--text-subtle)', fontSize: 13 }}>¿Ya tienes cuenta? </Text>
            <Button type="link" onClick={() => navigate('/login')} style={{ color: '#00D4AA', fontWeight: 600, padding: 0 }}>Iniciar Sesión</Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
