import { Button, Typography, Space } from 'antd';
import { ReloadOutlined, WifiOutlined } from '@ant-design/icons';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';
import { useState, useEffect } from 'react';

const { Title, Text } = Typography;

export default function Offline() {
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    const interval = setInterval(() => {
      if (navigator.onLine) {
        window.location.reload();
      }
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleRetry = () => {
    setChecking(true);
    setTimeout(() => {
      if (navigator.onLine) {
        window.location.reload();
      }
      setChecking(false);
    }, 2000);
  };

  return (
    <div style={{
      height: '100vh', width: '100vw', display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%)',
      position: 'fixed', top: 0, left: 0, zIndex: 9999, overflow: 'hidden',
    }}>
      <DotLottieReact src='https://lottie.host/b3a2480a-6851-4f8e-ab6b-834eb396cce5/VLVvmhUJcq.lottie'
        autoplay loop style={{ width: 200, height: 200 }} />
      <Title level={2} style={{ color: '#e2e8f0', margin: '16px 0 4px', textAlign: 'center' }}>Sin Conexión</Title>
      <Text style={{ color: '#94a3b8', fontSize: 16, marginBottom: 32, textAlign: 'center', maxWidth: 400, padding: '0 20px' }}>
        No se puede establecer conexión con el servidor. Verifica tu conexión a internet e intenta nuevamente.
      </Text>
      <Space direction="vertical" align="center" size={12}>
        <Button type="primary" size="large" icon={<ReloadOutlined spin={checking} />} onClick={handleRetry} loading={checking}
          style={{ borderRadius: 8, height: 44, paddingInline: 32, background: 'linear-gradient(135deg, #14b8a6, #0d9488)', border: 'none' }}>
          Reintentar
        </Button>
        <Text style={{ color: '#64748b', fontSize: 12 }}>
          <WifiOutlined style={{ marginRight: 4 }} />Reintentando automáticamente cada 5 segundos
        </Text>
      </Space>
    </div>
  );
}
