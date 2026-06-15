import { useEffect } from 'react';
import { Modal, Typography } from 'antd';
import { CheckCircleOutlined } from '@ant-design/icons';
import LazyLottie from './LazyLottie';

const { Text } = Typography;

interface SuccessModalProps {
  open: boolean;
  title: string;
  subtitle?: string;
  lottieSrc?: string;
  onClose?: () => void;
  duration?: number;
}

export default function SuccessModal({
  open,
  title,
  subtitle,
  lottieSrc = 'https://lottie.host/5f1e0f6e-1bb6-4dcf-9111-2ba16d2803d0/4kyt29vWqx.lottie',
  onClose,
  duration = 2500,
}: SuccessModalProps) {
  useEffect(() => {
    if (open && duration > 0) {
      const timer = setTimeout(() => onClose?.(), duration);
      return () => clearTimeout(timer);
    }
  }, [open, duration, onClose]);

  const isSvg = lottieSrc?.endsWith('.svg');

  return (
    <Modal
      open={open}
      footer={null}
      closable={false}
      centered
      width={520}
      maskStyle={{ background: 'rgba(8,13,24,0.6)', backdropFilter: 'blur(6px)' }}
      styles={{ body: { textAlign: 'center', padding: '48px 36px' } }}
      className="success-modal-styled"
    >
      <div style={{ 
        width: '100%', 
        height: 260, 
        margin: '0 auto 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative'
      }}>
        {isSvg ? (
          <img src={lottieSrc} alt="" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
        ) : (
          <LazyLottie autoplay loop={false} src={lottieSrc} style={{ width: '100%', height: '100%' }} />
        )}
      </div>
      <div style={{
        width: 52, height: 52, borderRadius: '50%',
        background: 'rgba(20,214,176,0.12)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        margin: '0 auto 20px',
        boxShadow: '0 8px 24px rgba(20,214,176,0.25)',
      }}>
        <CheckCircleOutlined style={{ fontSize: 26, color: '#14D6B0' }} />
      </div>
      <Text strong style={{ 
        color: 'var(--text-primary)', 
        fontSize: 22, 
        display: 'block', 
        marginBottom: 10,
        fontWeight: 700,
        letterSpacing: '-0.02em',
      }}>
        {title}
      </Text>
      {subtitle && (
        <Text style={{ 
          color: 'var(--text-secondary)', 
          fontSize: 15, 
          display: 'block',
          lineHeight: 1.5,
          maxWidth: 400,
          margin: '0 auto',
        }}>
          {subtitle}
        </Text>
      )}
    </Modal>
  );
}
