import { Component, type ReactNode, type ErrorInfo } from 'react';
import { Button, Typography, Space } from 'antd';
import { WarningOutlined, ReloadOutlined } from '@ant-design/icons';

const { Title, Text, Paragraph } = Typography;

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export default class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ errorInfo });
    console.error('[ErrorBoundary]', error, errorInfo);
  }

  handleReload = () => {
    this.setState({ error: null, errorInfo: null });
  };

  render() {
    if (this.state.error) {
      return (
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          minHeight: '100vh', padding: 32, background: 'var(--bg-body)',
        }}>
          <div style={{ textAlign: 'center', maxWidth: 520 }}>
            <div style={{
              width: 72, height: 72, borderRadius: '50%', background: 'rgba(239,68,68,0.1)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 24px', fontSize: 32, color: '#EF4444',
            }}>
              <WarningOutlined />
            </div>
            <Title level={3} style={{ color: 'var(--text-primary)', marginBottom: 8 }}>Algo salió mal</Title>
            <Paragraph style={{ color: 'var(--text-secondary)', marginBottom: 24 }}>
              Ocurrió un error inesperado. Puedes intentar recargar o reportar el problema.
            </Paragraph>
            <div style={{
              background: 'var(--bg-secondary)', borderRadius: 12, padding: '12px 16px',
              marginBottom: 24, textAlign: 'left', maxHeight: 200, overflow: 'auto',
            }}>
              <Text style={{ color: 'var(--text-muted)', fontSize: 12, fontFamily: 'monospace', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {this.state.error.name}: {this.state.error.message}
                {this.state.errorInfo?.componentStack && (
                  <>\n\n{this.state.errorInfo.componentStack}</>
                )}
              </Text>
            </div>
            <Space>
              <Button type="primary" icon={<ReloadOutlined />} onClick={this.handleReload} size="large"
                style={{ height: 44, borderRadius: 10, fontWeight: 600, minWidth: 180, background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none' }}>
                Reintentar
              </Button>
              <Button onClick={() => window.location.href = '/'} size="large"
                style={{ height: 44, borderRadius: 10, fontWeight: 500 }}>
                Ir al inicio
              </Button>
            </Space>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
