import { Spin } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';

export default function LoadingState({ text = 'Cargando...' }: { text?: string }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: 80, gap: 16 }}>
      <Spin indicator={<LoadingOutlined spin style={{ fontSize: 36, color: '#00D4AA' }} />} />
      <span style={{ color: 'var(--text-secondary)', fontSize: 14 }}>{text}</span>
    </div>
  );
}

