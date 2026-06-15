import { useState } from 'react';
import { Navigate, useNavigate, useLocation } from 'react-router-dom';
import { Card, Form, Input, Button, Typography, message, Space, Steps, Divider } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, SafetyOutlined } from '@ant-design/icons';
import { useAuth } from '../context/AuthContext';
import SuccessModal from '../components/common/SuccessModal';
import api from '../services/api';

const { Title, Text } = Typography;

export default function Login() {
  const [loading, setLoading] = useState(false);
  const [welcomeOpen, setWelcomeOpen] = useState(false);
  const [forgotOpen, setForgotOpen] = useState(false);
  const [forgotStep, setForgotStep] = useState(0);
  const [forgotEmail, setForgotEmail] = useState('');
  const [fpLoading, setFpLoading] = useState(false);
  const { login, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  if (isAuthenticated && !welcomeOpen) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      await login(values);
      setWelcomeOpen(true);
      setTimeout(() => {
        setWelcomeOpen(false);
        const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/';
        navigate(from, { replace: true });
      }, 2000);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { mensaje?: string; message?: string } } })?.response?.data;
      message.error(msg?.mensaje || msg?.message || 'Credenciales inválidas');
    } finally {
      setLoading(false);
    }
  };

  const handleSendCode = async (values: { email: string }) => {
    setFpLoading(true);
    try {
      await api.post('/auth/forgot-password', { email: values.email });
      setForgotEmail(values.email);
      setForgotStep(1);
      message.success('Código enviado si el email está registrado');
    } catch {
      message.error('Error al enviar código');
    } finally {
      setFpLoading(false);
    }
  };

  const handleResetPassword = async (values: { codigo: string; newPassword: string; confirmPassword: string }) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('Las contraseñas no coinciden');
      return;
    }
    setFpLoading(true);
    try {
      await api.post('/auth/reset-password', { email: forgotEmail, codigo: values.codigo, newPassword: values.newPassword });
      message.success('Contraseña restablecida exitosamente');
      setForgotOpen(false);
      setForgotStep(0);
      setForgotEmail('');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Error al restablecer contraseña';
      message.error(msg);
    } finally {
      setFpLoading(false);
    }
  };

  return (
    <div className="login-bg" style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 24,
    }}>
      <SuccessModal
        open={welcomeOpen}
        title={`¡Bienvenido, ${user?.username || 'Usuario'}!`}
        subtitle="Has iniciado sesión correctamente"
        lottieSrc="/lottie/Bienvenida.svg"
      />
      <div className="login-grid" />
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 80,
        width: '100%',
        maxWidth: 1100,
        position: 'relative',
        zIndex: 1,
        flexWrap: 'wrap',
      }}>
        <div style={{ flex: '1 1 450px', maxWidth: 500, textAlign: 'center' }}>
          <div style={{
            width: '100%',
            height: 440,
            margin: '0 auto',
            position: 'relative',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}>
            <div style={{
              position: 'absolute',
              width: 320,
              height: 320,
              borderRadius: '50%',
              background: 'radial-gradient(circle, rgba(20,214,176,0.12) 0%, transparent 70%)',
              animation: 'pulse-glow 4s ease-in-out infinite',
              zIndex: 0,
            }} />
            <img src="/lottie/Baner.svg" alt="MedTrack Illustration" style={{ width: '100%', height: '100%', objectFit: 'contain', position: 'relative', zIndex: 1 }} />
          </div>
          <Text style={{ 
            color: 'var(--text-secondary)', 
            fontSize: 16, 
            fontWeight: 500, 
            letterSpacing: '0.05em', 
            textTransform: 'uppercase',
            display: 'block',
            marginTop: -10,
          }}>
            Sistema de Monitoreo de Pacientes
          </Text>
        </div>

        <div style={{ flex: '0 0 380px', width: '100%' }}>
          <Card
            className="glass-strong"
            style={{
              borderRadius: 20,
              padding: '8px 0',
            }}
            styles={{ body: { padding: '32px 28px' } }}
          >
            {!forgotOpen ? (
              <>
                <div style={{ textAlign: 'center', marginBottom: 32 }}>
                  <img src="/Logo-1.png" alt="MT" style={{
                    width: 64,
                    height: 'auto',
                    borderRadius: 12,
                    margin: '0 auto 12px',
                    objectFit: 'contain',
                    background: 'rgba(255,255,255,0.1)',
                    padding: 8,
                  }} />
                  <Title level={4} style={{ color: 'var(--text-primary)', margin: 0, fontWeight: 700 }}>
                    Bienvenido
                  </Title>
                  <Text style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                    Inicia sesión para continuar
                  </Text>
                </div>

                <Form
                  layout="vertical"
                  onFinish={handleSubmit}
                  size="large"
                  requiredMark={false}
                >
                  <Form.Item
                    name="username"
                    label={<span style={{ color: 'var(--text-secondary)', fontSize: 13, fontWeight: 500 }}>Usuario</span>}
                    rules={[{ required: true, message: 'Ingresa tu usuario' }]}
                  >
                    <Input
                      prefix={<UserOutlined style={{ color: 'var(--text-muted)', marginRight: 8 }} />}
                      placeholder="Ingrese su usuario"
                      style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)', color: 'var(--text-primary)' }}
                    />
                  </Form.Item>

                  <Form.Item
                    name="password"
                    label={<span style={{ color: 'var(--text-secondary)', fontSize: 13, fontWeight: 500 }}>Contraseña</span>}
                    rules={[{ required: true, message: 'Ingresa tu contraseña' }]}
                  >
                    <Input.Password
                      prefix={<LockOutlined style={{ color: 'var(--text-muted)', marginRight: 8 }} />}
                      placeholder="Ingrese su contraseña"
                      style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)', color: 'var(--text-primary)' }}
                    />
                  </Form.Item>

                  <div style={{ textAlign: 'right', marginBottom: 16, marginTop: -8 }}>
                    <Button type="link" onClick={() => setForgotOpen(true)} style={{ color: '#00D4AA', fontSize: 13, padding: 0 }}>
                      ¿Olvidaste tu contraseña?
                    </Button>
                  </div>

                  <Form.Item style={{ marginBottom: 0 }}>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={loading}
                      block
                      style={{
                        height: 48,
                        borderRadius: 10,
                        fontWeight: 600,
                        fontSize: 15,
                        background: 'linear-gradient(135deg, #00D4AA, #059669)',
                        border: 'none',
                        boxShadow: '0 4px 14px rgba(0,212,170,0.35)',
                      }}
                    >
                      Iniciar Sesión
                    </Button>
                  </Form.Item>
                </Form>

                <div style={{ marginTop: 24, textAlign: 'center' }}>
                  <Text style={{ color: 'var(--text-subtle)', fontSize: 13 }}>¿No tienes una cuenta?</Text>
                  <div style={{ marginTop: 8 }}>
                    <Button type="link" onClick={() => navigate('/register')} style={{ color: '#00D4AA', fontWeight: 600, fontSize: 14, padding: 0 }}>
                      Registrarse aquí
                    </Button>
                  </div>
                </div>
              </>
            ) : (
              <>
                <div style={{ textAlign: 'center', marginBottom: 28 }}>
                  <img src="/Logo-1.png" alt="MT" style={{
                    width: 56,
                    height: 'auto',
                    borderRadius: 12,
                    margin: '0 auto 10px',
                    objectFit: 'contain',
                    background: 'rgba(255,255,255,0.1)',
                    padding: 6,
                  }} />
                  <Title level={4} style={{ color: 'var(--text-primary)', margin: 0, fontWeight: 700 }}>
                    Recuperar Contraseña
                  </Title>
                  <Text style={{ color: 'var(--text-muted)', fontSize: 13 }}>
                    {forgotStep === 0 ? 'Ingresa tu email para recibir un código' : 'Ingresa el código y tu nueva contraseña'}
                  </Text>
                </div>

                <Steps
                  current={forgotStep}
                  size="small"
                  style={{ marginBottom: 24 }}
                  items={[
                    { title: 'Email', icon: <MailOutlined /> },
                    { title: 'Código', icon: <SafetyOutlined /> },
                    { title: 'Listo', icon: <LockOutlined /> },
                  ]}
                />

                {forgotStep === 0 ? (
                  <Form layout="vertical" onFinish={handleSendCode} size="large" requiredMark={false}>
                    <Form.Item
                      name="email"
                      label={<span style={{ color: 'var(--text-secondary)', fontSize: 13, fontWeight: 500 }}>Email</span>}
                      rules={[{ required: true, type: 'email', message: 'Ingresa un email válido' }]}
                    >
                      <Input
                        prefix={<MailOutlined style={{ color: 'var(--text-muted)', marginRight: 8 }} />}
                        placeholder="correo@ejemplo.com"
                        style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)', color: 'var(--text-primary)' }}
                      />
                    </Form.Item>
                    <Form.Item style={{ marginBottom: 0 }}>
                      <Button
                        type="primary"
                        htmlType="submit"
                        loading={fpLoading}
                        block
                        style={{
                          height: 48,
                          borderRadius: 10,
                          fontWeight: 600,
                          background: 'linear-gradient(135deg, #00D4AA, #059669)',
                          border: 'none',
                          boxShadow: '0 4px 14px rgba(0,212,170,0.35)',
                        }}
                      >
                        Enviar Código
                      </Button>
                    </Form.Item>
                    {fpLoading && (
                      <div style={{ textAlign: 'center', padding: '12px 0' }}>
                        <img src="/lottie/Load.svg" alt="Enviando..." style={{ width: 120, height: 'auto' }} />
                        <Text style={{ display: 'block', color: 'var(--text-muted)', fontSize: 13, marginTop: 4 }}>Enviando código de verificación...</Text>
                      </div>
                    )}
                    <Divider style={{ borderColor: 'var(--border-color)', margin: '16px 0' }} />
                    <div style={{ textAlign: 'center' }}>
                      <Button type="link" onClick={() => setForgotOpen(false)} style={{ color: 'var(--text-muted)', fontSize: 13, padding: 0 }}>
                        Volver al inicio de sesión
                      </Button>
                    </div>
                  </Form>
                ) : (
                  <Form layout="vertical" onFinish={handleResetPassword} size="large" requiredMark={false}>
                    <Form.Item
                      name="codigo"
                      label={<span style={{ color: 'var(--text-secondary)', fontSize: 13, fontWeight: 500 }}>Código de Verificación</span>}
                      rules={[{ required: true, message: 'Ingresa el código' }]}
                    >
                      <Input
                        prefix={<SafetyOutlined style={{ color: 'var(--text-muted)', marginRight: 8 }} />}
                        placeholder="000000"
                        maxLength={6}
                        style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)', color: 'var(--text-primary)', textAlign: 'center', letterSpacing: 8, fontSize: 20 }}
                      />
                    </Form.Item>
                    <Form.Item
                      name="newPassword"
                      label={<span style={{ color: 'var(--text-secondary)', fontSize: 13, fontWeight: 500 }}>Nueva Contraseña</span>}
                      rules={[{ required: true, min: 6, message: 'Mínimo 6 caracteres' }]}
                    >
                      <Input.Password
                        prefix={<LockOutlined style={{ color: 'var(--text-muted)', marginRight: 8 }} />}
                        placeholder="Nueva contraseña"
                        style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)', color: 'var(--text-primary)' }}
                      />
                    </Form.Item>
                    <Form.Item
                      name="confirmPassword"
                      label={<span style={{ color: 'var(--text-secondary)', fontSize: 13, fontWeight: 500 }}>Confirmar Contraseña</span>}
                      rules={[{ required: true, message: 'Confirma tu contraseña' }]}
                    >
                      <Input.Password
                        prefix={<LockOutlined style={{ color: 'var(--text-muted)', marginRight: 8 }} />}
                        placeholder="Repite la contraseña"
                        style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)', color: 'var(--text-primary)' }}
                      />
                    </Form.Item>
                    <Form.Item style={{ marginBottom: 0 }}>
                      <Button
                        type="primary"
                        htmlType="submit"
                        loading={fpLoading}
                        block
                        style={{
                          height: 48,
                          borderRadius: 10,
                          fontWeight: 600,
                          background: 'linear-gradient(135deg, #00D4AA, #059669)',
                          border: 'none',
                          boxShadow: '0 4px 14px rgba(0,212,170,0.35)',
                        }}
                      >
                        Restablecer Contraseña
                      </Button>
                    </Form.Item>
                    <Divider style={{ borderColor: 'var(--border-color)', margin: '16px 0' }} />
                    <div style={{ textAlign: 'center' }}>
                      <Button type="link" onClick={() => { setForgotStep(0); }} style={{ color: 'var(--text-muted)', fontSize: 13, padding: 0 }}>
                        ¿No recibiste el código? Reenviar
                      </Button>
                    </div>
                  </Form>
                )}
              </>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
