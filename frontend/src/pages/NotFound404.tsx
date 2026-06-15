import { Button, Typography } from 'antd';
import { HomeOutlined } from '@ant-design/icons';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';
import { useNavigate } from 'react-router-dom';
import { useThemeMode } from '../context/ThemeContext';

const { Title, Text } = Typography;

export default function NotFound404() {
  const navigate = useNavigate();
  const { mode } = useThemeMode();
  const isDark = mode === 'dark';

  const bgGradient = isDark
    ? 'linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%)'
    : 'linear-gradient(135deg, #f0f4f8 0%, #e2e8f0 50%, #f8fafc 100%)';

  return (
    <div style={{
      height: '100vh', width: '100vw', display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center', background: bgGradient,
      position: 'fixed', top: 0, left: 0, zIndex: 9999, overflow: 'hidden',
    }}>
      <DotLottieReact src='https://lottie.host/4453ca27-3cdb-40a3-945c-9754d22eef42/qVQ4ZqifSA.lottie'
        autoplay loop style={{
          width: '85vw', maxWidth: 1200, maxHeight: '70vh', height: 'auto', borderRadius: 20,
          background: isDark ? 'rgba(255,255,255,0.9)' : 'transparent',
        }} />
      <Title level={2} style={{ color: isDark ? '#e2e8f0' : '#1e293b', margin: '8px 0 4px', textAlign: 'center' }}>
        Página no encontrada
      </Title>
      <Text style={{ color: isDark ? '#94a3b8' : '#475569', fontSize: 16, marginBottom: 32, textAlign: 'center' }}>
        La página que buscas no existe o ha sido movida
      </Text>
      <Button type="primary" size="large" icon={<HomeOutlined />} onClick={() => navigate('/')}
        style={{ borderRadius: 8, height: 44, paddingInline: 32, background: 'linear-gradient(135deg, #14b8a6, #0d9488)', border: 'none' }}>
        Volver al Inicio
      </Button>
    </div>
  );
}