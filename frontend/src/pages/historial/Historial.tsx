import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Typography, Timeline, Card, Tag, Spin, Button, Space, Descriptions, Empty, Alert, message } from 'antd';
import { ArrowLeftOutlined, CalendarOutlined, ExperimentOutlined, MedicineBoxOutlined, FileTextOutlined, UserOutlined, DownloadOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import type { HistorialEntry, Paciente } from '../../types';
import dayjs from 'dayjs';
import { generarPDF } from '../../services/reportePDF';

const { Title, Text } = Typography;

const tipoConfig: Record<string, { color: string; icon: React.ReactNode }> = {
  CITA: { color: '#3B82F6', icon: <CalendarOutlined /> },
  TRIAJE: { color: '#8B5CF6', icon: <UserOutlined /> },
  CONSULTA: { color: '#00D4AA', icon: <FileTextOutlined /> },
  EXAMEN: { color: '#F59E0B', icon: <ExperimentOutlined /> },
  RECETA: { color: '#EF4444', icon: <MedicineBoxOutlined /> },
};

export default function Historial() {
  const { pacienteId } = useParams<{ pacienteId: string }>();
  const navigate = useNavigate();
  const [selected, setSelected] = useState<HistorialEntry | null>(null);
  const [pdfLoading, setPdfLoading] = useState(false);

  const { data: entries, isLoading, error } = useQuery({
    queryKey: ['historial', pacienteId],
    queryFn: async () => {
      const res = await api.get(`/historial/paciente/${pacienteId}`);
      return res.data as HistorialEntry[];
    },
    enabled: !!pacienteId,
  });

  const { data: paciente } = useQuery({
    queryKey: ['paciente', pacienteId],
    queryFn: async () => {
      const res = await api.get(`/pacientes/${pacienteId}`);
      return res.data as Paciente;
    },
    enabled: !!pacienteId,
  });

  const descargarPDF = async () => {
    if (!entries || !paciente) return;
    setPdfLoading(true);
    try {
      await generarPDF(entries, `${paciente.nombres} ${paciente.apellidoPaterno}`.trim(), paciente.dni);
      message.success('PDF descargado exitosamente');
    } catch {
      message.error('Error al generar el PDF');
    } finally {
      setPdfLoading(false);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} type="text" size="large" />
        <div style={{ flex: 1 }}>
          <Title level={4} style={{ margin: 0 }}>Historial Clínico</Title>
          <Text style={{ color: 'var(--text-muted)' }}>{paciente ? `${paciente.nombres} ${paciente.apellidoPaterno} ${paciente.apellidoMaterno || ''}`.trim() : 'Línea de tiempo del paciente'}</Text>
        </div>
        <Button
          type="primary"
          icon={<DownloadOutlined />}
          loading={pdfLoading}
          disabled={!entries || entries.length === 0}
          onClick={descargarPDF}
          style={{ background: '#00D4AA', borderColor: '#00D4AA' }}
        >
          Descargar PDF
        </Button>
      </div>

      {isLoading && <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>}

      {error && <Alert message="Error al cargar historial" type="error" showIcon />}

      {entries && entries.length === 0 && <Empty description="No hay registros en el historial" />}

      {entries && entries.length > 0 && (
        <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: 400 }}>
            <Card className="glass" styles={{ body: { padding: '24px 28px' } }}>
              <Timeline
                items={entries.map((entry, i) => {
                  const cfg = tipoConfig[entry.tipo] || { color: '#738195', icon: null };
                  return {
                    color: cfg.color,
                    dot: <span style={{ color: cfg.color, fontSize: 16 }}>{cfg.icon}</span>,
                    children: (
                      <div
                        onClick={() => setSelected(selected?.fecha === entry.fecha && selected?.tipo === entry.tipo ? null : entry)}
                        style={{ cursor: 'pointer', padding: '8px 12px', borderRadius: 8, background: selected?.fecha === entry.fecha && selected?.tipo === entry.tipo ? 'rgba(0,212,170,0.08)' : 'transparent' }}
                      >
                        <Space>
                          <Tag color={cfg.color} style={{ borderRadius: 4 }}>{entry.tipo}</Tag>
                          <Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{dayjs(entry.fecha).format('DD/MM/YYYY HH:mm')}</Text>
                        </Space>
                        <div style={{ marginTop: 4 }}>
                          <Text strong style={{ color: 'var(--text-primary)' }}>{entry.titulo}</Text>
                        </div>
                        <Text style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{entry.descripcion}</Text>
                      </div>
                    ),
                  };
                })}
              />
            </Card>
          </div>

          {selected && (
            <div style={{ flex: 1, minWidth: 350, maxWidth: 500 }}>
              <Card className="glass" title={
                <Space>
                  <Tag color={tipoConfig[selected.tipo]?.color}>{selected.tipo}</Tag>
                  <Text strong>{selected.titulo}</Text>
                </Space>
              } styles={{ body: { padding: 16 } }}>
                <Descriptions column={1} size="small" bordered
                  styles={{
                    label: { color: 'var(--text-secondary)', fontWeight: 500, background: 'var(--bg-surface)' },
                    content: { color: 'var(--text-primary)', background: 'var(--bg-card)' },
                  }}
                >
                  <Descriptions.Item label="Fecha">{dayjs(selected.fecha).format('DD/MM/YYYY HH:mm')}</Descriptions.Item>
                  {Object.entries(selected.data || {}).filter(([k]) => !['id', 'pacienteId', 'citaId', 'doctorId', 'enfermeroId', 'farmaceuticoId', 'consultaId'].includes(k)).map(([key, val]) => (
                    val != null && <Descriptions.Item label={key.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase())} key={key}>
                      {String(val)}
                    </Descriptions.Item>
                  ))}
                </Descriptions>
              </Card>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
