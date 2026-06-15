import LazyLottie from './LazyLottie';
import { Button, Typography } from 'antd';

const { Text } = Typography;

interface EmptyStateProps {
  title?: string;
  description?: string;
  actionText?: string;
  onAction?: () => void;
  lottieSrc?: string;
}

export default function EmptyState({
  title = 'Sin datos',
  description = 'No se encontraron registros para mostrar.',
  actionText,
  onAction,
  lottieSrc = 'https://lottie.host/1cefab3b-ff01-49e6-83c2-7a40ecaa2d14/FDM17LLkOa.lottie',
}: EmptyStateProps) {
  return (
    <div style={{
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center',
      padding: '60px 24px', gap: 20,
    }}>
      <div style={{ width: 140, height: 140 }}>
        <LazyLottie autoplay loop src={lottieSrc} style={{ width: '100%', height: '100%' }} />
      </div>
      <Text strong style={{ color: 'var(--text-secondary)', fontSize: 16 }}>{title}</Text>
      <Text style={{ color: 'var(--text-muted)', fontSize: 13, textAlign: 'center', maxWidth: 300 }}>{description}</Text>
      {actionText && onAction && (
        <Button type="primary" onClick={onAction} style={{ marginTop: 8, borderRadius: 8 }}>
          {actionText}
        </Button>
      )}
    </div>
  );
}

