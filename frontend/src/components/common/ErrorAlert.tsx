import { Alert, Button, Space, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';

const { Text } = Typography;

interface ErrorAlertProps {
  message?: string;
  description?: string;
  onRetry?: () => void;
}

export default function ErrorAlert({ message = 'Error al cargar datos', description, onRetry }: ErrorAlertProps) {
  return (
    <Alert
      type="error"
      showIcon
      message={
        <Space>
          <Text strong>{message}</Text>
          {onRetry && (
            <Button size="small" icon={<ReloadOutlined />} onClick={onRetry}>
              Reintentar
            </Button>
          )}
        </Space>
      }
      description={description}
      style={{ borderRadius: 12, marginBottom: 16 }}
    />
  );
}
