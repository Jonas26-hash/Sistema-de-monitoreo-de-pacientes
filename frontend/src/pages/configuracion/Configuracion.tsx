import { useState, useRef, useEffect } from 'react';
import { Card, Form, Input, Button, Typography, message, Space, Divider, Spin } from 'antd';
import { SettingOutlined, UploadOutlined, DeleteOutlined, MedicineBoxOutlined } from '@ant-design/icons';
import { showCrudSuccess } from '../../utils/notifications';
import api from '../../services/api';

const { Title, Text } = Typography;

interface SystemConfig {
  hospitalName: string;
  hospitalLogo: string;
}

export default function Configuracion() {
  const [config, setConfig] = useState<SystemConfig>({ hospitalName: '', hospitalLogo: '' });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [logoPreview, setLogoPreview] = useState<string>('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    api.get('/auth/config')
      .then((res) => {
        const c = res.data;
        setConfig(c);
        setLogoPreview(c.hospitalLogo || '');
      })
      .catch(() => {
        message.error('Error al cargar configuración');
      })
      .finally(() => setLoading(false));
  }, []);

  const handleLogoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const dataUrl = ev.target?.result as string;
      setLogoPreview(dataUrl);
    };
    reader.readAsDataURL(file);
  };

  const removeLogo = () => {
    setLogoPreview('');
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleSave = async (values: { hospitalName: string }) => {
    setSaving(true);
    try {
      const res = await api.put('/auth/config', {
        hospitalName: values.hospitalName,
        hospitalLogo: logoPreview,
      });
      setConfig(res.data);
      if (values.hospitalName) {
        document.title = `${values.hospitalName} — MedTrack`;
      } else {
        document.title = 'MedTrack — Sistema de Monitoreo Clínico';
      }
      showCrudSuccess('actualizado', 'Configuración');
    } catch {
      message.error('Error al guardar configuración');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;
  }

  return (
    <div style={{ maxWidth: 720, margin: '0 auto' }}>
      <div className="crud-page-header" style={{ marginBottom: 28 }}>
        <Space align="center" size={12}>
          <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(0,212,170,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#00D4AA', fontSize: 20 }}>
            <SettingOutlined />
          </div>
          <div>
            <Title level={4} style={{ margin: 0, fontWeight: 700 }}>Configuración del Sistema</Title>
            <Text style={{ color: 'var(--text-muted)' }}>Personaliza el sistema con los datos de tu clínica</Text>
          </div>
        </Space>
      </div>

      <Card className="glass" style={{ borderRadius: 16 }}>
        <Form layout="vertical" onFinish={handleSave} size="large" requiredMark={false}
          initialValues={{ hospitalName: config.hospitalName }}
          style={{ width: '100%' }}>
          <div style={{ textAlign: 'center', marginBottom: 28 }}>
            <div style={{ width: 120, height: 120, margin: '0 auto', borderRadius: 16, border: '2px dashed var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', background: 'var(--bg-surface)', position: 'relative' }}>
              {logoPreview ? (
                <img src={logoPreview} alt="Logo" style={{ width: '100%', height: '100%', objectFit: 'contain', padding: 8 }} />
              ) : (
                <MedicineBoxOutlined style={{ fontSize: 40, color: 'var(--text-muted)', opacity: 0.4 }} />
              )}
            </div>
            <Space style={{ marginTop: 12, justifyContent: 'center' }} size={8}>
              <Button size="small" icon={<UploadOutlined />} onClick={() => fileInputRef.current?.click()}>
                {logoPreview ? 'Cambiar Logo' : 'Subir Logo'}
              </Button>
              {logoPreview && (
                <Button size="small" icon={<DeleteOutlined />} danger onClick={removeLogo}>Quitar</Button>
              )}
            </Space>
            <input ref={fileInputRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handleLogoChange} />
          </div>

          <Form.Item name="hospitalName" label="Nombre de la Clínica" rules={[{ required: true, message: 'Ingresa el nombre de tu clínica' }]}>
            <Input prefix={<MedicineBoxOutlined style={{ color: 'var(--text-muted)' }} />}
              placeholder="Ej: Clínica San Pablo" style={{ borderRadius: 10, height: 48 }} />
          </Form.Item>

          <Divider />

          <Card style={{ background: 'rgba(0,212,170,0.05)', border: '1px solid rgba(0,212,170,0.15)', borderRadius: 12 }}>
            <Space align="start" size={12}>
              <MedicineBoxOutlined style={{ color: '#00D4AA', fontSize: 20, marginTop: 2 }} />
              <div>
                <Text style={{ color: 'var(--text-primary)', fontWeight: 600, fontSize: 14 }}>Vista previa</Text>
                <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 12 }}>
                  {logoPreview && (
                    <img src={logoPreview} alt="" style={{ width: 32, height: 32, objectFit: 'contain', borderRadius: 6 }} />
                  )}
                  <Text style={{ color: 'var(--text-primary)', fontWeight: 700, fontSize: 16 }}>
                    {config.hospitalName || 'Mi Clínica'}
                  </Text>
                </div>
                <Text style={{ color: 'var(--text-muted)', fontSize: 12, display: 'block', marginTop: 4 }}>
                  Así se verá tu sistema con la configuración actual
                </Text>
              </div>
            </Space>
          </Card>

          <Divider />

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Button type="primary" htmlType="submit" loading={saving}
              style={{ height: 44, borderRadius: 10, fontWeight: 600, minWidth: 200, background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none' }}>
              Guardar Configuración
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
