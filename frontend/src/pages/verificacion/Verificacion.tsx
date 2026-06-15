import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Typography, message, Steps, Result } from 'antd';
import { KeyOutlined, UserOutlined, LockOutlined } from '@ant-design/icons';
import api from '../../services/api';

const { Title, Text } = Typography;

export default function Verificacion() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const emailParam = searchParams.get('email') || '';
  const usernameParam = searchParams.get('username') || '';
  const [step, setStep] = useState(0);
  const [loading, setLoading] = useState(false);

  const handleVerify = async (values: { email: string; codigo: string; username: string; password: string; confirmarPassword: string }) => {
    setLoading(true);
    try {
      await api.post('/auth/verificar-codigo', { email: values.email, codigo: values.codigo });
      const res = await api.post('/auth/completar-registro', {
        email: values.email,
        username: values.username,
        password: values.password,
        confirmarPassword: values.confirmarPassword,
      });
      setStep(2);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Error al verificar';
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-bg" style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <div className="login-grid" />
      <div style={{ width: '100%', maxWidth: 480, position: 'relative', zIndex: 1 }}>
        <Card className="glass-strong" style={{ borderRadius: 20, padding: '8px 0' }}
          styles={{ body: { padding: '32px 28px' } }}>
          <div style={{ textAlign: 'center', marginBottom: 24 }}>
            <div style={{ width: 180, height: 140, margin: '0 auto 16px' }}>
              <img src="/lottie/Untitled file.svg" alt="Verificación" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
            </div>
            <Title level={4} style={{ color: 'var(--text-primary)', margin: '0 0 4px', fontWeight: 700 }}>
              Verificación de Registro
            </Title>
            <Text style={{ color: 'var(--text-muted)', fontSize: 13, display: 'block' }}>
              Completa tu registro para activar tu cuenta
            </Text>
          </div>

          <Steps current={step} size="small" style={{ marginBottom: 28 }}
            items={[
              { title: 'Verificar' },
              { title: 'Completar', status: step === 2 ? 'finish' : 'process' },
            ]} />

          {step === 2 ? (
            <Result
              status="success"
              title="Registro Exitoso"
              subTitle="Tu cuenta ha sido creada. Ya puedes iniciar sesión."
              extra={[
                <Button key="login" type="primary" onClick={() => navigate('/login')}
                  style={{ borderRadius: 10, height: 44, background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none' }}>
                  Ir a Iniciar Sesión
                </Button>,
              ]}
            />
          ) : (
            <Form layout="vertical" onFinish={handleVerify} size="large" requiredMark={false}
              initialValues={{ email: emailParam, username: usernameParam }}>
              <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]}>
                <Input placeholder="tu@email.com" disabled={!!emailParam}
                  style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)' }} />
              </Form.Item>
              <Form.Item name="codigo" label="Código de Verificación" rules={[{ required: true, message: 'Ingresa el código enviado a tu email' }]}>
                <Input prefix={<KeyOutlined style={{ color: 'var(--text-muted)', marginRight: 8 }} />}
                  placeholder="123456" style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)' }} />
              </Form.Item>
              <Form.Item name="username" label="Nombre de Usuario" rules={[{ required: true, message: 'Elige un nombre de usuario' }]}>
                <Input prefix={<UserOutlined style={{ color: 'var(--text-muted)', marginRight: 8 }} />}
                  placeholder="ej: jperez" style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)' }} />
              </Form.Item>
              <Form.Item name="password" label="Contraseña" rules={[{ required: true, min: 6, message: 'Mínimo 6 caracteres' }]}>
                <Input.Password prefix={<LockOutlined style={{ color: 'var(--text-muted)', marginRight: 8 }} />}
                  placeholder="••••••••" style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)' }} />
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
                <Input.Password prefix={<LockOutlined style={{ color: 'var(--text-muted)', marginRight: 8 }} />}
                  placeholder="••••••••" style={{ height: 48, borderRadius: 10, background: 'var(--bg-surface)', borderColor: 'var(--border-color)' }} />
              </Form.Item>
              <Form.Item style={{ marginTop: 8 }}>
                <Button type="primary" htmlType="submit" loading={loading} block
                  style={{ height: 48, borderRadius: 10, fontWeight: 600, fontSize: 15,
                    background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none',
                    boxShadow: '0 4px 14px rgba(0,212,170,0.35)' }}>
                  Activar Cuenta
                </Button>
              </Form.Item>
            </Form>
          )}

          {step === 0 && (
            <div style={{ textAlign: 'center', marginTop: 8 }}>
              <Text style={{ color: 'var(--text-subtle)', fontSize: 13 }}>
                ¿Ya tienes cuenta?{' '}
                <Button type="link" onClick={() => navigate('/login')} style={{ color: '#00D4AA', fontWeight: 600, padding: 0 }}>
                  Inicia sesión
                </Button>
              </Text>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
