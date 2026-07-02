import { useState } from 'react';
import { Drawer, Typography, Spin, Empty, Tag, Badge, Button } from 'antd';
import { BellOutlined, UserOutlined, CloseOutlined } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import type { Notificacion } from '../../types';
import dayjs from 'dayjs';

const { Text } = Typography;

const remitenteColors: Record<string, string> = {
  SISTEMA: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
  DOCTOR: 'linear-gradient(135deg, #3b82f6, #06b6d4)',
  ADMIN: 'linear-gradient(135deg, #f59e0b, #ef4444)',
};

const remitenteIcons: Record<string, string> = {
  SISTEMA: 'S',
  DOCTOR: 'D',
  ADMIN: 'A',
};

interface Props {
  open: boolean;
  onClose: () => void;
  pacienteId?: number;
}

export default function NotificationDrawer({ open, onClose, pacienteId }: Props) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const isStaff = user?.roles?.some(r => ['ADMIN', 'ATENCION_CLIENTE'].includes(r));

  const { data: notificaciones, isLoading } = useQuery({
    queryKey: ['notificaciones-drawer', pacienteId],
    queryFn: async () => {
      if (pacienteId) {
        const r = await api.get(`/notificaciones/paciente/${pacienteId}`);
        return (r.data || []) as Notificacion[];
      }
      if (isStaff) {
        const r = await api.get('/notificaciones?size=30');
        return (r.data?.content || r.data || []) as Notificacion[];
      }
      return [];
    },
    enabled: open && (!!pacienteId || !!isStaff),
  });

  const noLeidas = (notificaciones || []).filter(n => !n.leida).length;

  const marcarLeida = async (id: number) => {
    try {
      await api.put(`/notificaciones/${id}/leer`);
      queryClient.invalidateQueries({ queryKey: ['notificaciones-drawer'] });
    } catch {}
  };

  const formatTime = (dateStr?: string) => {
    if (!dateStr) return '';
    const d = dayjs(dateStr);
    const now = dayjs();
    const diffMin = now.diff(d, 'minute');
    if (diffMin < 1) return 'Ahora';
    if (diffMin < 60) return `Hace ${diffMin} min`;
    const diffHrs = now.diff(d, 'hour');
    if (diffHrs < 24) return `Hace ${diffHrs}h`;
    const diffDays = now.diff(d, 'day');
    if (diffDays < 7) return `Hace ${diffDays}d`;
    return d.format('DD/MM/YY');
  };

  const selected = notificaciones?.find(n => n.id === selectedId);

  return (
    <Drawer
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <BellOutlined style={{ color: 'var(--brand-primary)' }} />
          <Text strong style={{ color: 'var(--text-primary)' }}>Notificaciones</Text>
          {noLeidas > 0 && <Badge count={noLeidas} style={{ backgroundColor: '#EF4444' }} />}
        </div>
      }
      placement="right"
      onClose={() => { setSelectedId(null); onClose(); }}
      open={open}
      width={Math.min(400, typeof window !== 'undefined' ? window.innerWidth - 16 : 400)}
      className="notif-drawer"
      styles={{ body: { padding: 0, display: 'flex', flexDirection: 'column' } }}
      closeIcon={<CloseOutlined style={{ color: 'var(--text-secondary)' }} />}
    >
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: 60 }}><Spin /></div>
      ) : !notificaciones?.length ? (
        <div style={{ textAlign: 'center', padding: 60 }}><Empty description="Sin notificaciones" /></div>
      ) : selectedId && selected ? (
        <div className="notif-detail" style={{ padding: 20 }}>
          <Button type="text" size="small" onClick={() => setSelectedId(null)} style={{ marginBottom: 12, color: 'var(--brand-primary)' }}>
            ← Volver
          </Button>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
            <div className="notif-avatar" style={{ background: remitenteColors[selected.remitenteTipo || 'SISTEMA'] || remitenteColors.SISTEMA }}>
              {selected.remitenteNombre?.[0] || 'S'}
            </div>
            <div>
              <Text strong style={{ color: 'var(--text-primary)' }}>{selected.remitenteNombre || 'Sistema'}</Text>
              <br /><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{formatTime(selected.fechaEnvio)}</Text>
            </div>
            <Tag color="blue" style={{ marginLeft: 'auto' }}>{selected.tipo}</Tag>
          </div>
          <div className="notif-mensaje">
            <Text style={{ color: 'var(--text-primary)', whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>{selected.mensaje}</Text>
          </div>
        </div>
      ) : (
        <div className="notif-list" style={{ flex: 1, overflowY: 'auto' }}>
          {notificaciones.slice().reverse().map(n => (
            <div
              key={n.id}
              className={`notif-item ${!n.leida ? 'notif-unread' : ''}`}
              onClick={() => { setSelectedId(n.id); if (!n.leida) marcarLeida(n.id); }}
            >
              <div className="notif-avatar" style={{ background: remitenteColors[n.remitenteTipo || 'SISTEMA'] || remitenteColors.SISTEMA }}>
                {n.remitenteNombre?.[0] || 'S'}
              </div>
              <div className="notif-content">
                <div className="notif-header">
                  <Text strong style={{ color: 'var(--text-primary)', fontSize: 13 }}>{n.remitenteNombre || 'Sistema'}</Text>
                  <Text style={{ color: 'var(--text-muted)', fontSize: 11 }}>{formatTime(n.fechaEnvio)}</Text>
                </div>
                <Text style={{ color: 'var(--text-primary)', fontSize: 13, fontWeight: n.leida ? 400 : 600 }}>{n.tipo}</Text>
                <Text className="notif-preview" style={{ color: 'var(--text-secondary)', fontSize: 12 }}>
                  {n.mensaje?.length > 80 ? n.mensaje.substring(0, 80) + '...' : n.mensaje}
                </Text>
              </div>
              {!n.leida && <span className="notif-dot" />}
            </div>
          ))}
        </div>
      )}
    </Drawer>
  );
}
