import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Typography, Tabs, Card, Tag, Spin, Button, Space, Empty, Timeline, message, Divider, Steps, Modal, Descriptions } from 'antd';
import { CalendarOutlined, MedicineBoxOutlined, ExperimentOutlined, DollarOutlined, FileTextOutlined, UserOutlined, RightOutlined, DownloadOutlined, WalletOutlined, CheckCircleOutlined, CreditCardOutlined, HeartOutlined, PrinterOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import type { Cita, Receta, OrdenExamen, Cobro, HistorialEntry } from '../types';
import dayjs from 'dayjs';
import { generarPDF } from '../services/reportePDF';
import QRCode from 'qrcode';
import { buildEmvcoPayload, YAPE_NUMERO, isMobileDevice, buildYapeDeepLink } from '../utils/yape';

const { Title, Text } = Typography;

export default function Portal() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [tab, setTab] = useState('citas');
  const [pdfLoading, setPdfLoading] = useState(false);

  const { data: profile } = useQuery({
    queryKey: ['profile'],
    queryFn: async () => { const r = await api.get('/auth/profile'); return r.data; },
  });

  const pacienteId = profile?.pacienteId as number | undefined;

  const { data: paciente } = useQuery({
    queryKey: ['paciente', pacienteId],
    queryFn: async () => { const r = await api.get(`/pacientes/${pacienteId}`); return r.data; },
    enabled: !!pacienteId,
  });

  const [showPayment, setShowPayment] = useState(false);
  const [pagoPaso, setPagoPaso] = useState(1);
  const [recetaDetailOpen, setRecetaDetailOpen] = useState(false);
  const [examenDetailOpen, setExamenDetailOpen] = useState(false);
  const [selectedReceta, setSelectedReceta] = useState<any>(null);
  const [selectedExamen, setSelectedExamen] = useState<any>(null);
  const [selectedRecetas, setSelectedRecetas] = useState<number[]>([]);
  const [selectedExamenes, setSelectedExamenes] = useState<number[]>([]);
  const [selectedCobros, setSelectedCobros] = useState<number[]>([]);
  const [qrDataUrl, setQrDataUrl] = useState<string>('');
  const queryClient = useQueryClient();

  const { data: citas, isLoading: loadingCitas } = useQuery({
    queryKey: ['portal-citas', pacienteId],
    queryFn: async () => { const r = await api.get(`/citas/paciente/${pacienteId}`); return (r.data || []) as Cita[]; },
    enabled: !!pacienteId,
  });

  const { data: recetas, isLoading: loadingRecetas } = useQuery({
    queryKey: ['portal-recetas', pacienteId],
    queryFn: async () => { const r = await api.get(`/recetas/paciente/${pacienteId}`); return (r.data || []) as Receta[]; },
    enabled: !!pacienteId,
  });

  const { data: examenes, isLoading: loadingExamenes } = useQuery({
    queryKey: ['portal-examenes', pacienteId],
    queryFn: async () => { const r = await api.get(`/ordenes-examen/paciente/${pacienteId}`); return (r.data || []) as OrdenExamen[]; },
    enabled: !!pacienteId,
  });

  const { data: deudasRaw } = useQuery({
    queryKey: ['portal-deudas', pacienteId],
    queryFn: async () => { const r = await api.get(`/cobros/deudas/${pacienteId}`); return r.data; },
    enabled: !!pacienteId,
  });

  const deudasRecetas = Array.isArray(deudasRaw?.recetas) ? deudasRaw.recetas : [];
  const deudasExamenes = Array.isArray(deudasRaw?.examenes) ? deudasRaw.examenes : [];
  const deudasCobros = Array.isArray(deudasRaw?.cobros) ? deudasRaw.cobros : [];
  const totalPendiente = [...deudasCobros, ...deudasRecetas, ...deudasExamenes].reduce((s: number, i: any) => s + (i.monto || i.costo || 0), 0);

  const montoBase = () => {
    let t = 0;
    deudasRecetas.filter((r: any) => selectedRecetas.includes(r.id)).forEach((r: any) => t += r.costo || 0);
    deudasExamenes.filter((e: any) => selectedExamenes.includes(e.id)).forEach((e: any) => t += e.costo || 0);
    deudasCobros.filter((c: any) => selectedCobros.includes(c.id)).forEach((c: any) => t += c.monto || 0);
    return t;
  };
  const montoCents = () => Math.round(montoBase() * 100);

  useEffect(() => {
    if (pagoPaso !== 2 || montoBase() <= 0) return;
    const payload = buildEmvcoPayload(montoCents());
    QRCode.toDataURL(payload, { width: 200, margin: 1, color: { dark: '#000000', light: '#ffffff' } })
      .then(url => setQrDataUrl(url))
      .catch(() => setQrDataUrl(''));
  }, [pagoPaso, montoBase(), montoCents()]);

  const pagoMutation = useMutation({
    mutationFn: (values: any) => api.post('/cobros/pago-unico', values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['portal-deudas'] });
      setPagoPaso(3);
    },
    onError: () => message.error('Error al procesar pago'),
  });

  const { data: historial, isLoading: loadingHistorial } = useQuery({
    queryKey: ['portal-historial', pacienteId],
    queryFn: async () => { const r = await api.get(`/historial/paciente/${pacienteId}`); return (r.data || []) as HistorialEntry[]; },
    enabled: !!pacienteId,
  });

  const descargarPDF = async () => {
    if (!historial || !paciente) return;
    setPdfLoading(true);
    try {
      await generarPDF(historial, `${paciente.nombres} ${paciente.apellidoPaterno}`.trim(), paciente.dni);
      message.success('PDF descargado');
    } catch { message.error('Error al generar PDF'); }
    finally { setPdfLoading(false); }
  };

  const printRecetaPdf = (r: any) => {
    const win = window.open('', '_blank');
    if (!win) return;
    win.document.write(`<html><head><title>Receta #${r.id}</title><style>
      *{margin:0;padding:0;box-sizing:border-box}
      body{font-family:Arial,sans-serif;padding:40px;color:#1a1a2e}
      .header{text-align:center;border-bottom:2px solid #00D4AA;padding-bottom:16px;margin-bottom:24px}
      .header h1{color:#00D4AA;font-size:22px;margin-bottom:4px}
      .header p{color:#64748b;font-size:12px}
      .grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:20px}
      .grid div label{color:#64748b;font-size:11px;text-transform:uppercase;display:block;margin-bottom:2px}
      .grid div span{font-size:14px;font-weight:600}
      h2{font-size:15px;color:#00D4AA;margin-bottom:10px;border-bottom:1px solid #e2e8f0;padding-bottom:6px}
      .med-item{padding:6px 0;border-bottom:1px dashed #e2e8f0;font-size:13px}
      .ind{background:#f8fafc;padding:14px;border-radius:6px;font-size:13px;line-height:1.5;margin-bottom:20px}
      .footer{margin-top:30px;text-align:right;border-top:1px solid #e2e8f0;padding-top:16px}
      .firma{margin-top:30px;width:200px;border-top:1px solid #1a1a2e;text-align:center;font-size:12px;color:#64748b;padding-top:6px;margin-left:auto}
      @media print{body{padding:20px}}
    </style></head><body>
      <div class="header"><h1>RECETA MÉDICA</h1><p>Clínica - Sistema de Monitoreo de Pacientes</p></div>
      <div class="grid">
        <div><label>N° Receta</label><span>#${r.id}</span></div>
        <div><label>Fecha de Emisión</label><span>${r.fechaEmision ? dayjs(r.fechaEmision).format('DD/MM/YYYY') : '-'}</span></div>
        <div><label>Paciente</label><span>${paciente?.nombres || ''} ${paciente?.apellidoPaterno || ''}</span></div>
        ${r.fechaVigencia ? `<div><label>Vigencia Hasta</label><span>${dayjs(r.fechaVigencia).format('DD/MM/YYYY')}</span></div>` : ''}
      </div>
      <h2>Medicamentos</h2>
      ${(r.medicamentos || '').split(',').map((m: string) => `<div class="med-item">${m.trim()}</div>`).join('')}
      ${r.indicaciones ? `<h2 style="margin-top:16px">Indicaciones</h2><div class="ind">${r.indicaciones}</div>` : ''}
      <div class="footer"><div class="firma">Firma del Doctor</div></div>
      <p style="text-align:center;color:#94a3b8;font-size:10px;margin-top:24px">Documento generado electrónicamente</p>
      <script>window.print();window.close();<\/script>
    </body></html>`);
    win.document.close();
  };

  const printExamenPdf = (r: any) => {
    const win = window.open('', '_blank');
    if (!win) return;
    win.document.write(`<html><head><title>Resultado de Examen #${r.id}</title><style>
      *{margin:0;padding:0;box-sizing:border-box}
      body{font-family:Arial,sans-serif;padding:40px;color:#1a1a2e}
      .header{text-align:center;border-bottom:2px solid #8B5CF6;padding-bottom:16px;margin-bottom:24px}
      .header h1{color:#8B5CF6;font-size:22px;margin-bottom:4px}
      .header p{color:#64748b;font-size:12px}
      .grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:20px}
      .grid div label{color:#64748b;font-size:11px;text-transform:uppercase;display:block;margin-bottom:2px}
      .grid div span{font-size:14px;font-weight:600}
      h2{font-size:15px;color:#8B5CF6;margin-bottom:10px;border-bottom:1px solid #e2e8f0;padding-bottom:6px}
      .result-box{background:#f8fafc;padding:16px;border-radius:6px;font-size:13px;line-height:1.6;white-space:pre-wrap;border-left:4px solid #8B5CF6}
      .footer{margin-top:30px;text-align:right;border-top:1px solid #e2e8f0;padding-top:16px}
      .firma{margin-top:30px;width:200px;border-top:1px solid #1a1a2e;text-align:center;font-size:12px;color:#64748b;padding-top:6px;margin-left:auto}
      @media print{body{padding:20px}}
    </style></head><body>
      <div class="header"><h1>RESULTADO DE EXAMEN</h1><p>Clínica - Sistema de Monitoreo de Pacientes</p></div>
      <div class="grid">
        <div><label>N° Orden</label><span>#${r.id}</span></div>
        <div><label>Fecha de Orden</label><span>${r.fechaOrden ? dayjs(r.fechaOrden).format('DD/MM/YYYY HH:mm') : '-'}</span></div>
        <div><label>Paciente</label><span>${paciente?.nombres || ''} ${paciente?.apellidoPaterno || ''}</span></div>
        <div><label>Tipo</label><span>${r.tipo || '-'}</span></div>
        ${r.costo != null ? `<div><label>Costo</label><span>S/. ${r.costo.toFixed(2)}</span></div>` : ''}
        <div><label>Estado</label><span>${r.estado || 'PENDIENTE'}</span></div>
        ${r.fechaResultado ? `<div><label>Fecha Resultado</label><span>${dayjs(r.fechaResultado).format('DD/MM/YYYY HH:mm')}</span></div>` : ''}
      </div>
      <h2>Descripción</h2>
      <div class="result-box">${r.descripcion || 'Sin descripción'}</div>
      ${r.resultado ? `<h2 style="margin-top:16px">Resultado</h2><div class="result-box">${r.resultado}</div>` : '<h2 style="margin-top:16px">Resultado</h2><div class="result-box" style="color:#94a3b8">Pendiente</div>'}
      <div class="footer"><div class="firma">Firma del Doctor</div></div>
      <p style="text-align:center;color:#94a3b8;font-size:10px;margin-top:24px">Documento generado electrónicamente</p>
      <script>window.print();window.close();<\/script>
    </body></html>`);
    win.document.close();
  };

  const estadoTag = (estado: string) => {
    const colors: Record<string, string> = { PROGRAMADA: 'blue', CONFIRMADA: 'cyan', EN_CURSO: 'orange', COMPLETADA: 'green', CANCELADA: 'red', PENDIENTE: 'orange', PAGADO: 'green', ACTIVO: 'green', INACTIVO: 'default' };
    return <Tag color={colors[estado] || 'default'}>{estado}</Tag>;
  };

  if (profile && !pacienteId) return (
    <div style={{ textAlign: 'center', padding: 80 }}>
      <Empty description="Tu cuenta de paciente no está vinculada a un perfil de paciente. Contacta a administración." />
    </div>
  );
  if (!pacienteId) return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;

  const upcomingCitas = (citas || []).filter((c: any) => new Date(c.fechaHora) > new Date()).slice(0, 10);
  const pastCitas = (citas || []).filter((c: any) => new Date(c.fechaHora) <= new Date()).slice(0, 10);

  return (
    <div>
      <div className="portal-header">
        <div className="portal-header-left">
          <div className="portal-avatar">
            {paciente?.nombres?.[0] || <UserOutlined />}
          </div>
          <div>
            <Title level={4} style={{ margin: 0 }}>Mi Portal</Title>
            <Text style={{ color: 'var(--text-muted)', fontSize: 13 }}>
              {paciente ? `${paciente.nombres} ${paciente.apellidoPaterno}`.trim() : ''}
            </Text>
          </div>
        </div>
      </div>

      <Tabs activeKey={tab} onChange={setTab} className="portal-tabs" size="small"
        items={[
          {
            key: 'citas', label: <span><CalendarOutlined /> Citas</span>,
            children: loadingCitas ? <Spin style={{ display: 'block', padding: 40 }} /> : !citas?.length ? <Empty description="Sin citas registradas" /> : (
              <div className="portal-card-list">
                {upcomingCitas.length > 0 && <>
                  <Text strong style={{ color: 'var(--text-primary)', display: 'block', marginBottom: 8 }}>Próximas</Text>
                  {upcomingCitas.map((c: any) => (
                    <Card key={c.id} className="portal-card" size="small" onClick={() => navigate('/citas')}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                          <Text strong style={{ color: 'var(--text-primary)' }}>{c.motivo || 'Cita'}</Text>
                          <br /><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{dayjs(c.fechaHora).format('DD/MM/YYYY h:mm A')}</Text>
                          {c.doctorNombre && <><br /><Text style={{ color: 'var(--text-muted)', fontSize: 11 }}>Dr. {c.doctorNombre}</Text></>}
                        </div>
                        <Space direction="vertical" align="end">
                          {estadoTag(c.estado)}
                          <RightOutlined style={{ color: 'var(--text-muted)' }} />
                        </Space>
                      </div>
                    </Card>
                  ))}
                  {pastCitas.length > 0 && <Divider style={{ margin: '12px 0' }} />}
                </>}
                {pastCitas.length > 0 && <>
                  <Text strong style={{ color: 'var(--text-primary)', display: 'block', marginBottom: 8 }}>Anteriores</Text>
                  {pastCitas.map((c: any) => (
                    <Card key={c.id} className="portal-card" size="small">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                          <Text style={{ color: 'var(--text-primary)' }}>{c.motivo || 'Cita'}</Text>
                          <br /><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{dayjs(c.fechaHora).format('DD/MM/YYYY h:mm A')}</Text>
                        </div>
                        {estadoTag(c.estado)}
                      </div>
                    </Card>
                  ))}
                </>}
              </div>
            ),
          },
          {
            key: 'recetas', label: <span><MedicineBoxOutlined /> Recetas</span>,
            children: loadingRecetas ? <Spin style={{ display: 'block', padding: 40 }} /> : !recetas?.length ? <Empty description="Sin recetas" /> : (
              <div className="portal-card-list">
                {recetas.map((r: any) => (
                  <Card key={r.id} className="portal-card" size="small" hoverable
                    onClick={() => { setSelectedReceta(r); setRecetaDetailOpen(true); }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <div>
                        <Text strong style={{ color: 'var(--text-primary)' }}>{r.descripcion || `Receta #${r.id}`}</Text>
                        <br /><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{r.fechaEmision ? dayjs(r.fechaEmision).format('DD/MM/YYYY') : '-'}</Text>
                        {r.medicamentos && <><br /><Text style={{ color: 'var(--text-secondary)', fontSize: 11 }}>{r.medicamentos}</Text></>}
                      </div>
                      <Tag color="purple">Receta</Tag>
                    </div>
                  </Card>
                ))}
              </div>
            ),
          },
          {
            key: 'resultados', label: <span><ExperimentOutlined /> Resultados</span>,
            children: loadingExamenes ? <Spin style={{ display: 'block', padding: 40 }} /> : !examenes?.length ? <Empty description="Sin exámenes" /> : (
              <div className="portal-card-list">
                {examenes.map((e: any) => (
                  <Card key={e.id} className="portal-card" size="small" hoverable
                    onClick={() => { setSelectedExamen(e); setExamenDetailOpen(true); }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <div>
                        <Text strong style={{ color: 'var(--text-primary)' }}>{e.descripcion || e.tipo || 'Examen'}</Text>
                        <br /><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{dayjs(e.fechaOrden).format('DD/MM/YYYY')}</Text>
                        {e.resultado && <><br /><Text style={{ color: 'var(--text-secondary)', fontSize: 11 }}>{String(e.resultado).substring(0, 80)}</Text></>}
                      </div>
                      <Tag color={e.estado === 'COMPLETADO' ? 'green' : 'orange'}>{e.estado || 'PENDIENTE'}</Tag>
                    </div>
                  </Card>
                ))}
              </div>
            ),
          },
          {
            key: 'pagos', label: <span><DollarOutlined /> Pagos</span>,
            children: totalPendiente <= 0 ? <Empty description="Sin deudas pendientes" /> : (
              <div>
                <div className="portal-card-list">
                  {deudasCobros.map((c: any) => (
                    <Card key={c.id} className="portal-card" size="small">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                          <Text style={{ color: 'var(--text-primary)' }}>{c.descripcion || c.tipo || 'Cobro'}</Text>
                          <br /><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{c.fechaCobro ? dayjs(c.fechaCobro).format('DD/MM/YYYY') : '-'}</Text>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <Text style={{ color: c.estado === 'PENDIENTE' ? '#EF4444' : '#00D4AA', fontWeight: 700, fontSize: 16 }}>S/{Number(c.monto || 0).toFixed(2)}</Text>
                          <br />{estadoTag(c.estado)}
                        </div>
                      </div>
                    </Card>
                  ))}
                  {deudasRecetas.map((r: any) => (
                    <Card key={`receta-${r.id}`} className="portal-card" size="small">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                          <Text style={{ color: 'var(--text-primary)' }}>{r.descripcion || `Receta #${r.id}`}</Text>
                          <br /><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{r.fechaCreacion ? dayjs(r.fechaCreacion).format('DD/MM/YYYY') : '-'}</Text>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <Text style={{ color: '#EF4444', fontWeight: 700, fontSize: 16 }}>S/{Number(r.costo || 0).toFixed(2)}</Text>
                          <br /><Tag color="orange">PENDIENTE</Tag>
                        </div>
                      </div>
                    </Card>
                  ))}
                  {deudasExamenes.map((e: any) => (
                    <Card key={`exam-${e.id}`} className="portal-card" size="small">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                          <Text style={{ color: 'var(--text-primary)' }}>{e.descripcion || e.tipo || 'Examen'}</Text>
                          <br /><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>{e.fechaOrden ? dayjs(e.fechaOrden).format('DD/MM/YYYY') : '-'}</Text>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <Text style={{ color: '#EF4444', fontWeight: 700, fontSize: 16 }}>S/{Number(e.costo || 0).toFixed(2)}</Text>
                          <br /><Tag color="orange">PENDIENTE</Tag>
                        </div>
                      </div>
                    </Card>
                  ))}
                </div>
                <div style={{ background: '#f0fdf4', borderRadius: 12, padding: '14px 18px', marginTop: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text style={{ fontWeight: 700, fontSize: 16, color: '#065f46' }}>Total Pendiente</Text>
                    <Text style={{ display: 'block', fontSize: 12, color: '#6b7280' }}>
                      {deudasCobros.length + deudasRecetas.length + deudasExamenes.length} item(s)
                    </Text>
                  </div>
                  <Button type="primary" icon={<WalletOutlined />} onClick={() => { setShowPayment(true); setPagoPaso(1); setSelectedRecetas([]); setSelectedExamenes([]); setSelectedCobros([]); }} style={{ background: '#00D4AA', borderColor: '#00D4AA' }}>
                    Pagar con Yape
                  </Button>
                </div>
              </div>
            ),
          },
          {
            key: 'historial', label: <span><FileTextOutlined /> Historial</span>,
            children: loadingHistorial ? <Spin style={{ display: 'block', padding: 40 }} /> : !historial?.length ? <Empty description="Sin historial" /> : (
              <>
                <div style={{ textAlign: 'right', marginBottom: 12 }}>
                  <Button type="primary" icon={<DownloadOutlined />} loading={pdfLoading} onClick={descargarPDF} size="small" style={{ background: '#00D4AA', borderColor: '#00D4AA' }}>
                    PDF
                  </Button>
                </div>
                <Timeline className="portal-timeline"
                  items={historial.slice(0, 20).map((h) => {
                    const colors: Record<string, string> = { CITA: '#3B82F6', TRIAJE: '#8B5CF6', CONSULTA: '#00D4AA', EXAMEN: '#F59E0B', RECETA: '#EF4444' };
                    return {
                      color: colors[h.tipo] || '#738195',
                      children: (
                        <div style={{ padding: '4px 0' }}>
                          <Space size={4}>
                            <Tag color={colors[h.tipo]} style={{ fontSize: 10, borderRadius: 4, margin: 0 }}>{h.tipo}</Tag>
                            <Text style={{ color: 'var(--text-muted)', fontSize: 11 }}>{dayjs(h.fecha).format('DD/MM/YYYY')}</Text>
                          </Space>
                          <div style={{ marginTop: 2 }}><Text style={{ color: 'var(--text-primary)', fontSize: 13 }}>{h.titulo}</Text></div>
                        </div>
                      ),
                    };
                  })}
                />
              </>
            ),
          },
        ]}
      />
      <Modal title={<Text style={{ fontWeight: 600 }}>Pagar con Yape</Text>}
        open={showPayment} onCancel={() => { setShowPayment(false); setPagoPaso(1); setSelectedRecetas([]); setSelectedExamenes([]); setSelectedCobros([]); }}
        width={520} destroyOnClose
        footer={pagoPaso === 2 ? [
          <Button key="back" onClick={() => setPagoPaso(1)}>Atrás</Button>,
          <Button key="pay" type="primary" icon={<CheckCircleOutlined />} size="large"
            loading={pagoMutation.isPending} onClick={() => pagoMutation.mutate({
              pacienteId, recetaIds: selectedRecetas, examenIds: selectedExamenes, cobroIds: selectedCobros,
              monto: montoBase(), tipoComprobante: 'BOLETA', numDocumento: paciente?.dni || '',
              descripcion: `Pago desde portal - ${selectedRecetas.length} receta(s), ${selectedExamenes.length} examen(es), ${selectedCobros.length} cobro(s)`,
            })} style={{ background: '#00D4AA', borderColor: '#00D4AA', height: 48, fontWeight: 600 }}>
            Ya pagué — Confirmar
          </Button>,
        ] : pagoPaso === 3 ? [
          <Button key="close" type="primary" onClick={() => { setShowPayment(false); setPagoPaso(1); setSelectedRecetas([]); setSelectedExamenes([]); setSelectedCobros([]); }}>Cerrar</Button>,
        ] : undefined}
        styles={{ body: { padding: '24px 28px', minHeight: 300 } }}>
        <Steps current={pagoPaso - 1} style={{ marginBottom: 24 }} items={[{ title: 'Seleccionar' }, { title: 'Yape QR' }, { title: 'Éxito' }]} />
        {pagoPaso === 1 && (
          <div>
            {(deudasRecetas.length + deudasExamenes.length + deudasCobros.length) === 0 && (
              <Text style={{ color: 'var(--text-muted)' }}>No tienes deudas pendientes.</Text>
            )}
            <div style={{ marginBottom: 12 }}>
              <Space>
                <Button size="small" onClick={() => { setSelectedRecetas(deudasRecetas.map((r: any) => r.id)); setSelectedExamenes(deudasExamenes.map((e: any) => e.id)); setSelectedCobros(deudasCobros.map((c: any) => c.id)); }}>Seleccionar Todo</Button>
                <Button size="small" onClick={() => { setSelectedRecetas([]); setSelectedExamenes([]); setSelectedCobros([]); }}>Deseleccionar</Button>
              </Space>
            </div>
            {deudasCobros.length > 0 && (
              <div style={{ marginBottom: 12 }}>
                <Text strong style={{ fontSize: 13, display: 'block', marginBottom: 4 }}>Cobros pendientes</Text>
                {deudasCobros.map((c: any) => (
                  <div key={c.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '3px 0' }}>
                    <input type="checkbox" checked={selectedCobros.includes(c.id)} onChange={() => setSelectedCobros(prev => prev.includes(c.id) ? prev.filter((id: number) => id !== c.id) : [...prev, c.id])} />
                    <Text style={{ fontSize: 13 }}>{c.descripcion || c.tipo}</Text>
                    <Tag style={{ marginLeft: 'auto', borderRadius: 4 }}>S/. {c.monto.toFixed(2)}</Tag>
                  </div>
                ))}
              </div>
            )}
            {deudasRecetas.length > 0 && (
              <div style={{ marginBottom: 12 }}>
                <Text strong style={{ fontSize: 13, display: 'block', marginBottom: 4 }}>Recetas pendientes</Text>
                {deudasRecetas.map((r: any) => (
                  <div key={r.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '3px 0' }}>
                    <input type="checkbox" checked={selectedRecetas.includes(r.id)} onChange={() => setSelectedRecetas(prev => prev.includes(r.id) ? prev.filter((id: number) => id !== r.id) : [...prev, r.id])} />
                    <Text style={{ fontSize: 13 }}>{r.descripcion || `Receta #${r.id}`}</Text>
                    <Tag style={{ marginLeft: 'auto', borderRadius: 4 }}>S/. {(r.costo || 0).toFixed(2)}</Tag>
                  </div>
                ))}
              </div>
            )}
            {deudasExamenes.length > 0 && (
              <div style={{ marginBottom: 12 }}>
                <Text strong style={{ fontSize: 13, display: 'block', marginBottom: 4 }}>Exámenes pendientes</Text>
                {deudasExamenes.map((e: any) => (
                  <div key={e.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '3px 0' }}>
                    <input type="checkbox" checked={selectedExamenes.includes(e.id)} onChange={() => setSelectedExamenes(prev => prev.includes(e.id) ? prev.filter((id: number) => id !== e.id) : [...prev, e.id])} />
                    <Text style={{ fontSize: 13 }}>{e.descripcion || e.tipo || 'Examen'}</Text>
                    <Tag style={{ marginLeft: 'auto', borderRadius: 4 }}>S/. {(e.costo || 0).toFixed(2)}</Tag>
                  </div>
                ))}
              </div>
            )}
            {(deudasRecetas.length + deudasExamenes.length + deudasCobros.length) > 0 && (
              <div style={{ background: '#f0fdf4', padding: '12px 16px', borderRadius: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Text style={{ fontWeight: 600, color: '#065f46' }}>Total a pagar</Text>
                  <Text style={{ fontWeight: 700, fontSize: 16, color: '#059669' }}>S/. {montoBase().toFixed(2)}</Text>
                </div>
              </div>
            )}
            <div style={{ marginTop: 16 }}>
              <Button type="primary" disabled={montoBase() <= 0} onClick={() => setPagoPaso(2)}
                style={isMobileDevice() ? { background: '#7C3AED', borderColor: '#7C3AED', boxShadow: '0 4px 12px rgba(124,58,237,0.3)' } : { background: '#00D4AA', borderColor: '#00D4AA' }}>
                {isMobileDevice() ? 'Pagar con Yape' : 'Generar QR Yape'}
              </Button>
            </div>
          </div>
        )}
        {pagoPaso === 2 && (
          <div style={{ textAlign: 'center' }}>
            {isMobileDevice() ? (
              <div>
                <div style={{ background: 'linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%)', borderRadius: 16, padding: 24, marginBottom: 16, border: '2px solid #7C3AED' }}>
                  <Text style={{ display: 'block', fontSize: 13, color: '#6b7280', marginBottom: 4 }}>Total a pagar</Text>
                  <Text style={{ display: 'block', fontSize: 36, fontWeight: 700, color: '#7C3AED', marginBottom: 24 }}>
                    S/. {montoBase().toFixed(2)}
                  </Text>

                  <Button
                    type="primary"
                    size="large"
                    onClick={() => {
                      const payload = buildEmvcoPayload(montoCents());
                      window.location.href = buildYapeDeepLink(payload);
                    }}
                    style={{
                      height: 56, borderRadius: 16, fontSize: 18, fontWeight: 700,
                      background: '#7C3AED', border: 'none', boxShadow: '0 4px 16px rgba(124,58,237,0.3)',
                      display: 'inline-flex', alignItems: 'center', gap: 12, padding: '0 32px',
                    }}
                  >
                    <span style={{
                      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                      width: 32, height: 32, borderRadius: 8, background: '#fff', color: '#7C3AED',
                      fontSize: 18, fontWeight: 900,
                    }}>Y</span>
                    Pagar con Yape
                  </Button>

                  <div style={{ marginTop: 12 }}>
                    <Button type="link" style={{ color: '#7C3AED', fontSize: 12 }}
                      onClick={() => {
                        const payload = buildEmvcoPayload(montoCents());
                        QRCode.toDataURL(payload, { width: 200, margin: 1, color: { dark: '#000000', light: '#ffffff' } })
                          .then(url => setQrDataUrl(url))
                          .catch(() => {});
                      }}>
                      Ver código QR como respaldo
                    </Button>
                    {qrDataUrl && (
                      <div style={{ marginTop: 8 }}>
                        <img src={qrDataUrl} alt="QR Yape" style={{ width: 140, height: 140, borderRadius: 8 }} />
                      </div>
                    )}
                  </div>
                </div>

                <div style={{ background: '#fefce8', borderRadius: 12, padding: 12, textAlign: 'left' }}>
                  <Text style={{ fontSize: 12, color: '#92400e' }}>
                    <HeartOutlined style={{ marginRight: 6 }} />
                    Presiona "Pagar con Yape" para abrir la app. Confirma el monto y yapea. Luego presiona "Ya pagué".
                  </Text>
                </div>
              </div>
            ) : (
              <div>
                <div style={{ background: 'linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%)', borderRadius: 16, padding: 24, marginBottom: 16, border: '2px solid #00D4AA' }}>
                  <Text style={{ display: 'block', fontSize: 13, color: '#6b7280', marginBottom: 4 }}>Total a pagar</Text>
                  <Text style={{ display: 'block', fontSize: 36, fontWeight: 700, color: '#059669', marginBottom: 20 }}>S/. {montoBase().toFixed(2)}</Text>
                  <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 16 }}>
                    <div style={{ padding: 8, background: '#fff', borderRadius: 16, boxShadow: '0 4px 16px rgba(0,212,170,0.15)', border: '2px solid #00D4AA' }}>
                      <img src={qrDataUrl || undefined} alt="QR Yape" style={{ width: 180, height: 180, borderRadius: 8, display: 'block' }} />
                    </div>
                  </div>
                  <div style={{ background: '#d1fae5', borderRadius: 12, padding: 16, maxWidth: 320, margin: '0 auto' }}>
                    <Space>
                      <CreditCardOutlined style={{ fontSize: 24, color: '#059669' }} />
                      <div>
                        <Text style={{ display: 'block', fontWeight: 600, fontSize: 16, color: '#065f46' }}>Yape: +51 {YAPE_NUMERO}</Text>
                      </div>
                    </Space>
                  </div>
                </div>
                <div style={{ background: '#fefce8', borderRadius: 12, padding: 12, textAlign: 'left' }}>
                  <Text style={{ fontSize: 12, color: '#92400e' }}>
                    <HeartOutlined style={{ marginRight: 6 }} />
                    Abre Yape, escanea el código QR y confirma el pago. Luego presiona "Ya pagué".
                  </Text>
                </div>
              </div>
            )}
          </div>
        )}
        {pagoPaso === 3 && (
          <div style={{ textAlign: 'center', padding: '20px 0' }}>
            <div style={{ width: 80, height: 80, borderRadius: '50%', background: '#d1fae5', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
              <CheckCircleOutlined style={{ fontSize: 40, color: '#00D4AA' }} />
            </div>
            <Title level={4} style={{ margin: 0, color: '#059669' }}>¡Pago registrado!</Title>
            <Text style={{ color: 'var(--text-muted)', display: 'block', marginTop: 8 }}>Tu pago ha sido procesado correctamente.</Text>
          </div>
        )}
      </Modal>

      <Modal title={<Text style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Detalle de Receta #{selectedReceta?.id}</Text>}
        open={recetaDetailOpen} onCancel={() => { setRecetaDetailOpen(false); setSelectedReceta(null); }} footer={null} width={650} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        {!selectedReceta ? <Spin /> : (
          <>
            <Descriptions bordered column={1} size="small" labelStyle={{ fontWeight: 500, color: 'var(--text-secondary)', background: 'rgba(0,0,0,0.02)' }} contentStyle={{ color: 'var(--text-primary)' }}>
              <Descriptions.Item label="Paciente">{paciente?.nombres || ''} {paciente?.apellidoPaterno || ''}</Descriptions.Item>
              <Descriptions.Item label="Fecha Emisión">{selectedReceta.fechaEmision ? dayjs(selectedReceta.fechaEmision).format('DD/MM/YYYY') : '-'}</Descriptions.Item>
              <Descriptions.Item label="Vigencia Hasta">{selectedReceta.fechaVigencia ? dayjs(selectedReceta.fechaVigencia).format('DD/MM/YYYY') : '-'}</Descriptions.Item>
              <Descriptions.Item label="Dispensada">{selectedReceta.dispensada ? 'Sí' : 'No'}</Descriptions.Item>
            </Descriptions>

            <Divider />
            <Text strong style={{ display: 'block', marginBottom: 8, color: 'var(--text-primary)' }}>Medicamentos</Text>
            <div style={{ padding: 12, background: 'rgba(0,212,170,0.05)', borderRadius: 8, border: '1px solid rgba(0,212,170,0.15)', marginBottom: 16 }}>
              {(selectedReceta.medicamentos || '').split(',').map((m: string, i: number) => (
                <div key={i} style={{ padding: '4px 0', borderBottom: i < (selectedReceta.medicamentos || '').split(',').length - 1 ? '1px dashed rgba(0,0,0,0.06)' : 'none', fontSize: 13 }}>{m.trim()}</div>
              ))}
            </div>

            {selectedReceta.indicaciones && (
              <>
                <Text strong style={{ display: 'block', marginBottom: 8, color: 'var(--text-primary)' }}>Indicaciones</Text>
                <div style={{ padding: 12, background: 'rgba(59,130,246,0.05)', borderRadius: 8, border: '1px solid rgba(59,130,246,0.15)', marginBottom: 16, fontSize: 13, lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
                  {selectedReceta.indicaciones}
                </div>
              </>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
              <Button onClick={() => { setRecetaDetailOpen(false); setSelectedReceta(null); }}>Cerrar</Button>
              <Button type="primary" icon={<PrinterOutlined />} style={{ background: '#00D4AA', borderColor: '#00D4AA' }} onClick={() => printRecetaPdf(selectedReceta)}>
                Descargar PDF
              </Button>
            </div>
          </>
        )}
      </Modal>

      <Modal title={<Text style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Detalle de Examen #{selectedExamen?.id}</Text>}
        open={examenDetailOpen} onCancel={() => { setExamenDetailOpen(false); setSelectedExamen(null); }} footer={null} width={650} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        {!selectedExamen ? <Spin /> : (
          <>
            <Descriptions bordered column={1} size="small" labelStyle={{ fontWeight: 500, color: 'var(--text-secondary)', background: 'rgba(0,0,0,0.02)' }} contentStyle={{ color: 'var(--text-primary)' }}>
              <Descriptions.Item label="Paciente">{paciente?.nombres || ''} {paciente?.apellidoPaterno || ''}</Descriptions.Item>
              <Descriptions.Item label="Tipo">{selectedExamen.tipo || '-'}</Descriptions.Item>
              <Descriptions.Item label="Estado">
                <Tag color={selectedExamen.estado === 'COMPLETADO' ? 'green' : 'orange'}>{selectedExamen.estado || 'PENDIENTE'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Fecha Orden">{selectedExamen.fechaOrden ? dayjs(selectedExamen.fechaOrden).format('DD/MM/YYYY HH:mm') : '-'}</Descriptions.Item>
              <Descriptions.Item label="Fecha Resultado">{selectedExamen.fechaResultado ? dayjs(selectedExamen.fechaResultado).format('DD/MM/YYYY HH:mm') : '-'}</Descriptions.Item>
              <Descriptions.Item label="Costo">{selectedExamen.costo != null ? `S/. ${selectedExamen.costo.toFixed(2)}` : '-'}</Descriptions.Item>
            </Descriptions>

            <Divider />
            <Text strong style={{ display: 'block', marginBottom: 8, color: 'var(--text-primary)' }}>Descripción</Text>
            <div style={{ padding: 12, background: 'rgba(139,92,246,0.05)', borderRadius: 8, border: '1px solid rgba(139,92,246,0.15)', marginBottom: 16, fontSize: 13, lineHeight: 1.6 }}>
              {selectedExamen.descripcion || 'Sin descripción'}
            </div>

            <Text strong style={{ display: 'block', marginBottom: 8, color: 'var(--text-primary)' }}>Resultado</Text>
            <div style={{ padding: 12, background: selectedExamen.resultado ? 'rgba(0,212,170,0.05)' : 'rgba(245,158,11,0.05)', borderRadius: 8, border: selectedExamen.resultado ? '1px solid rgba(0,212,170,0.15)' : '1px solid rgba(245,158,11,0.15)', marginBottom: 16, fontSize: 13, lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
              {selectedExamen.resultado || <span style={{ color: 'var(--text-muted)' }}>Pendiente — resultado no ingresado aún</span>}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
              <Button onClick={() => { setExamenDetailOpen(false); setSelectedExamen(null); }}>Cerrar</Button>
              <Button type="primary" icon={<PrinterOutlined />} style={{ background: '#8B5CF6', borderColor: '#8B5CF6' }} onClick={() => printExamenPdf(selectedExamen)}>
                Descargar PDF
              </Button>
            </div>
          </>
        )}
      </Modal>
    </div>
  );
}


