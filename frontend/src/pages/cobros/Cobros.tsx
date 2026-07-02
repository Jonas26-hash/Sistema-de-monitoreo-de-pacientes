import { Table, Button, Modal, Form, Input, InputNumber, Select, DatePicker, Tag, Space, Input as SearchInput, Typography, message, Steps, Divider } from 'antd';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, DollarOutlined, PrinterOutlined, WalletOutlined, CheckCircleOutlined, UserOutlined, CreditCardOutlined, HeartOutlined, CloseOutlined } from '@ant-design/icons';
import { useState, useMemo, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useCrud } from '../../hooks/useCrud';
import { showCrudSuccess } from '../../utils/notifications';
import type { Cobro, Paciente, OrdenExamen, Receta, Servicio, Campania } from '../../types';
import api from '../../services/api';
import PacienteSearchByDni from '../../components/paciente/PacienteSearchByDni';
import dayjs from 'dayjs';
import QRCode from 'qrcode';
import { buildEmvcoPayload, YAPE_NUMERO, isMobileDevice, buildYapeDeepLink } from '../../utils/yape';

const { Title, Text } = Typography;
const statusColors: Record<string, string> = { PENDIENTE: '#F59E0B', PAGADO: '#00D4AA', ANULADO: '#EF4444' };

export default function Cobros() {
  const [form] = Form.useForm();
  const [pagoForm] = Form.useForm();
  const queryClient = useQueryClient();
  const [printModal, setPrintModal] = useState<{ open: boolean; cobro: Cobro | null }>({ open: false, cobro: null });
  const [printForm] = Form.useForm();
  const [pagoUnicoOpen, setPagoUnicoOpen] = useState(false);
  const [selectedPaciente, setSelectedPaciente] = useState<Paciente | null>(null);
  const [selectedRecetas, setSelectedRecetas] = useState<number[]>([]);
  const [selectedExamenes, setSelectedExamenes] = useState<number[]>([]);
  const [pagoPaso, setPagoPaso] = useState(0);
  const [qrDataUrl, setQrDataUrl] = useState<string>('');
  const { search, setSearch, data, loading, page, setPage, modalOpen, editing, openCreate, openEdit, closeModal, handleSave, handleDelete } = useCrud<Cobro>({
    key: 'cobros',
    endpoint: '/cobros',
    dateFields: ['fechaCobro'],
  });

  const { data: pacientes } = useQuery({
    queryKey: ['pacientes-lista'],
    queryFn: async () => { const r = await api.get('/pacientes'); return r.data as Paciente[]; },
  });

  const { data: servicios } = useQuery({
    queryKey: ['servicios-lista'],
    queryFn: async () => { const r = await api.get('/servicios'); return r.data as Servicio[]; },
  });

  const { data: nombreClinica } = useQuery({
    queryKey: ['hospital-name'],
    queryFn: async () => { const r = await api.get('/auth/config'); return (r.data?.hospitalName as string) || 'MedTrack'; },
    staleTime: 300000,
  });

  const { data: campaniasActivas } = useQuery({
    queryKey: ['campanias-activas'],
    queryFn: async () => { const r = await api.get('/campanias/activas'); return r.data as Campania[]; },
  });

  const precioReceta = servicios?.find(s => s.tipo === 'RECETA')?.precio ?? 0;
  const serviciosExamen = servicios?.filter(s => s.tipo === 'EXAMEN') || [];
  const descuentoGlobal = campaniasActivas?.reduce((max, c) => Math.max(max, c.descuentoPorcentaje), 0) || 0;

  const pacienteMap = new Map(pacientes?.map(p => [p.id, p]) || []);

  const [selectedCobros, setSelectedCobros] = useState<number[]>([]);

  const { data: deudasData, refetch: refetchDeudas } = useQuery({
    queryKey: ['deudas-paciente', selectedPaciente?.id],
    queryFn: async () => {
      if (!selectedPaciente) return { recetas: [], examenes: [], cobros: [] };
      const res = await api.get(`/cobros/deudas/${selectedPaciente.id}`);
      return res.data as { recetas: Receta[]; examenes: OrdenExamen[]; cobros: Cobro[] };
    },
    enabled: !!selectedPaciente,
  });

  const pagoUnicoMutation = useMutation({
    mutationFn: (values: { pacienteId: number; recetaIds: number[]; examenIds: number[]; cobroIds: number[]; monto: number; tipoComprobante: string; numDocumento: string; descripcion: string }) =>
      api.post('/cobros/pago-unico', values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cobros'] });
      setPagoPaso(4);
      setTimeout(() => {
        setPagoUnicoOpen(false);
        setPagoPaso(0);
        setSelectedPaciente(null);
        setSelectedRecetas([]);
        setSelectedExamenes([]);
        setSelectedCobros([]);
        pagoForm.resetFields();
      }, 3000);
    },
    onError: () => Modal.error({ title: 'Error', content: 'Error al procesar pago único' }),
  });

  const getExamenPrecio = (examen: OrdenExamen) => {
    if (examen.costo && examen.costo > 0) return examen.costo;
    const match = serviciosExamen.find(s =>
      examen.tipo?.toLowerCase().includes(s.nombre?.toLowerCase()) ||
      s.nombre?.toLowerCase().includes(examen.tipo?.toLowerCase()) ||
      examen.descripcion?.toLowerCase().includes(s.nombre?.toLowerCase())
    );
    return match?.precio || 0;
  };

  const montoBase = () => {
    let total = 0;
    const recetas = deudasData?.recetas || [];
    const examenes = deudasData?.examenes || [];
    const cobros = deudasData?.cobros || [];
    selectedRecetas.forEach(id => { const r = recetas.find(r => r.id === id); if (r) total += (r.costo ?? precioReceta); });
    selectedExamenes.forEach(id => { const e = examenes.find(e => e.id === id); if (e) total += getExamenPrecio(e); });
    selectedCobros.forEach(id => { const c = cobros.find(c => c.id === id); if (c) total += c.monto; });
    return total;
  };

  const montoDescuento = () => descuentoGlobal > 0 ? montoBase() * descuentoGlobal / 100 : 0;
  const montoFinal = () => montoBase() - montoDescuento();
  const montoCents = () => Math.round(montoFinal() * 100);

  useEffect(() => {
    if (pagoPaso !== 2 || montoFinal() <= 0) return;
    const payload = buildEmvcoPayload(montoCents());
    QRCode.toDataURL(payload, { width: 200, margin: 1, color: { dark: '#000000', light: '#ffffff' } })
      .then(url => setQrDataUrl(url))
      .catch(() => setQrDataUrl(''));
  }, [pagoPaso, montoFinal(), montoCents()]);

  const selectAll = () => {
    const recetas = deudasData?.recetas || [];
    const examenes = deudasData?.examenes || [];
    const cobros = deudasData?.cobros || [];
    setSelectedRecetas(recetas.map(r => r.id!));
    setSelectedExamenes(examenes.map(e => e.id!));
    setSelectedCobros(cobros.map(c => c.id!));
  };

  const deselectAll = () => {
    setSelectedRecetas([]);
    setSelectedExamenes([]);
    setSelectedCobros([]);
  };

  const generateComprobante = (c: Cobro, tipo: 'BOLETA' | 'FACTURA', numDoc: string) => {
    const paciente = pacienteMap.get(c.pacienteId);
    const nombre = paciente ? `${paciente.nombres} ${paciente.apellidoPaterno}${paciente.apellidoMaterno ? ' ' + paciente.apellidoMaterno : ''}` : `#${c.pacienteId}`;
    const rucClinica = '20457698321';
    const direccion = 'Av. La Salud 456, Lima';
    const serie = tipo === 'BOLETA' ? 'B001' : 'F001';
    const docLabel = tipo === 'BOLETA' ? 'DNI' : 'RUC';

    const win = window.open('', '_blank');
    if (!win) return;
    win.document.write(`<html><head><title>${tipo === 'BOLETA' ? 'Boleta' : 'Factura'} #${c.id}</title><style>
      *{margin:0;padding:0;box-sizing:border-box}
      body{font-family:'Courier New',Courier,monospace;padding:30px;color:#000;font-size:12px;line-height:1.4}
      .header{text-align:center;border-bottom:2px solid #000;padding-bottom:12px;margin-bottom:16px}
      .header .logo{font-size:18px;font-weight:700;letter-spacing:2px}
      .header .ruc{font-size:11px;margin-top:2px}
      .header .dir{font-size:10px;color:#555}
      .doc-title{text-align:center;font-size:16px;font-weight:700;padding:8px 0;border-bottom:1px dashed #000;margin-bottom:12px;letter-spacing:3px}
      .info-grid{display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-bottom:12px;font-size:11px}
      .info-grid .label{font-weight:600}
      table{width:100%;border-collapse:collapse;margin-bottom:12px;font-size:11px}
      table th{border-bottom:1px solid #000;padding:4px 2px;text-align:left;font-weight:600}
      table td{padding:4px 2px;border-bottom:1px dashed #ccc}
      table td.right{text-align:right}
      .totals{margin-left:auto;width:250px;font-size:11px}
      .totals div{display:flex;justify-content:space-between;padding:2px 0}
      .totals .grand{border-top:2px solid #000;font-weight:700;font-size:13px;padding-top:4px;margin-top:4px}
      .footer{text-align:center;margin-top:20px;padding-top:12px;border-top:1px dashed #000;font-size:10px;color:#555}
      .qr{margin:8px auto;width:80px;height:80px;border:1px solid #ccc;display:flex;align-items:center;justify-content:center;font-size:8px;color:#999}
      @media print{body{padding:15px}@page{margin:10mm}}
    </style></head><body>
      <div class="header">
        <div class="logo">${nombreClinica || 'MedTrack'}</div>
        <div class="ruc">RUC: ${rucClinica}</div>
        <div class="dir">${direccion}</div>
      </div>
      <div class="doc-title">${tipo === 'BOLETA' ? 'BOLETA DE VENTA' : 'FACTURA'}</div>
      <div class="info-grid">
        <div><span class="label">${docLabel}:</span> ${numDoc || '-'}</div>
        <div><span class="label">Cliente:</span> ${nombre}</div>
        <div><span class="label">Serie-N°:</span> ${serie}-${String(c.id).padStart(6,'0')}</div>
        <div><span class="label">Fecha:</span> ${c.fechaCobro ? dayjs(c.fechaCobro).format('DD/MM/YYYY') : dayjs().format('DD/MM/YYYY')}</div>
        <div><span class="label">Tipo:</span> ${c.tipo}</div>
        <div><span class="label">Estado:</span> ${c.estado}</div>
      </div>
      <table>
        <thead><tr><th>Cant.</th><th>Descripción</th><th class="right">Precio</th><th class="right">Total</th></tr></thead>
        <tbody>
          <tr><td>1</td><td>${c.descripcion || c.tipo}</td><td class="right">S/. ${c.monto.toFixed(2)}</td><td class="right">S/. ${c.monto.toFixed(2)}</td></tr>
        </tbody>
      </table>
      <div class="totals">
        <div><span>Sub Total:</span><span>S/. ${(c.monto / 1.18).toFixed(2)}</span></div>
        <div><span>I.G.V. (18%):</span><span>S/. ${(c.monto - c.monto / 1.18).toFixed(2)}</span></div>
        <div class="grand"><span>Total:</span><span>S/. ${c.monto.toFixed(2)}</span></div>
      </div>
      ${c.referenciaId ? `<p style="font-size:10px;margin-top:8px">Ref: #${c.referenciaId}</p>` : ''}
      <div class="qr">Código QR</div>
      <div class="footer">
        <p>Representación impresa del comprobante electrónico</p>
        <p>¡Gracias por su preferencia!</p>
      </div>
      <script>window.print();window.close();<\/script>
    </body></html>`);
    win.document.close();
  };

  const openPrintDialog = (c: Cobro) => {
    const paciente = pacienteMap.get(c.pacienteId);
    printForm.setFieldsValue({
      tipo: c.tipoComprobante || 'BOLETA',
      numDoc: c.numDocumento || paciente?.dni || '',
    });
    setPrintModal({ open: true, cobro: c });
  };

  const handlePrintConfirm = () => {
    const cobro = printModal.cobro;
    if (!cobro) return;
    printForm.validateFields().then(values => {
      generateComprobante(cobro, values.tipo, values.numDoc);
      setPrintModal({ open: false, cobro: null });
    }).catch(() => {});
  };

  const abrirPagoUnico = () => {
    setPagoPaso(0);
    setSelectedPaciente(null);
    setSelectedRecetas([]);
    setSelectedExamenes([]);
    setSelectedCobros([]);
    pagoForm.resetFields();
    setPagoUnicoOpen(true);
  };

  const seleccionarPaciente = (p: Paciente) => {
    setSelectedPaciente(p);
    setSelectedRecetas([]);
    setSelectedExamenes([]);
    setSelectedCobros([]);
    setPagoPaso(1);
    setTimeout(() => refetchDeudas(), 100);
  };

  const handleProcesarPago = () => {
    pagoForm.validateFields().then(values => {
      if (!selectedPaciente || montoFinal() <= 0) return;
      pagoUnicoMutation.mutate({
        pacienteId: selectedPaciente.id,
        recetaIds: selectedRecetas,
        examenIds: selectedExamenes,
        cobroIds: selectedCobros,
        monto: montoFinal(),
        tipoComprobante: values.tipoComprobante,
        numDocumento: values.numDocumento,
        descripcion: `Pago único - ${selectedRecetas.length} receta(s), ${selectedExamenes.length} examen(es), ${selectedCobros.length} cobro(s) pendiente(s)`,
      });
    }).catch(() => {});
  };

  const columns = [
    { title: 'Nº', key: 'index', width: 60, render: (_v: unknown, _r: unknown, i: number) => <Text style={{ color: 'var(--text-muted)' }}>{i + 1}</Text> },
    {
      title: 'Paciente', key: 'paciente', dataIndex: 'pacienteId',
      render: (v: number) => { const p = pacienteMap.get(v); return <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{p ? `${p.nombres} ${p.apellidoPaterno}` : `#${v}`}</Text>; },
    },
    { title: 'Tipo', dataIndex: 'tipo', key: 'tipo', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Monto', dataIndex: 'monto', key: 'monto', render: (v: number) => <Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>S/. {v?.toFixed(2)}</Text> },
    { title: 'Comprobante', key: 'comprobante', render: (_: unknown, r: Cobro) =>
      r.tipoComprobante ? <Text style={{ color: 'var(--text-muted)' }}>{r.tipoComprobante} {r.numDocumento}</Text> : '-' },
    { title: 'Fecha', dataIndex: 'fechaCobro', key: 'fechaCobro', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v ? dayjs(v).format('DD/MM/YYYY') : '-'}</Text> },
    { title: 'Estado', dataIndex: 'estado', key: 'estado', render: (v: string) => <Tag color={statusColors[v]} style={{ borderRadius: 4 }}>{v}</Tag> },
    {
      title: '', key: 'acciones', width: 140,
      render: (_: unknown, r: Cobro) => (
        <Space>
          <Button type="text" icon={<PrinterOutlined />} style={{ color: '#8B5CF6' }} onClick={() => openPrintDialog(r)} title="Imprimir Comprobante" />
          <Button type="text" icon={<EditOutlined />} style={{ color: '#3B82F6' }} onClick={() => openEdit(r)} />
          <Button type="text" icon={<DeleteOutlined />} style={{ color: '#EF4444' }} onClick={() => handleDelete(r.id!)} />
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(139,92,246,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#8B5CF6', fontSize: 20 }}>
              <DollarOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Cobros</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Gestión de cobros y pagos</Text>
            </div>
          </Space>
        </div>
        <Space>
          <SearchInput.Search placeholder="Buscar por DNI o nombre del paciente" allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button icon={<WalletOutlined />} onClick={abrirPagoUnico} style={{ borderColor: '#00D4AA', color: '#00D4AA' }}>Pago Único</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nuevo Cobro</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'auto' }}>
        <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={loading} scroll={{ x: 650 }}
          pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false }} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{editing ? 'Editar Cobro' : 'Nuevo Cobro'}</Text>}
        open={modalOpen} onCancel={closeModal} onOk={() => form.submit()} okText={editing ? 'Actualizar' : 'Crear'} width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={handleSave} initialValues={editing || { estado: 'PENDIENTE' }} preserve={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="pacienteId" label="Paciente" rules={[{ required: true }]} style={{ width: '50%' }}>
              <PacienteSearchByDni pacientes={pacientes || []} onSelect={(p) => form.setFieldValue('pacienteId', p.id)} autoFocus />
            </Form.Item>
            <Form.Item name="referenciaId" label="ID Referencia" style={{ width: '50%' }}>
              <InputNumber min={1} style={{ width: '100%' }} placeholder="ID de consulta/factura" />
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="monto" label="Monto (S/.)" rules={[{ required: true }]} style={{ width: '50%' }}>
              <InputNumber min={0} step={0.01} prefix="S/. " style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="tipo" label="Tipo" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Select placeholder="Tipo de cobro">
                <Select.Option value="CONSULTA">Consulta</Select.Option>
                <Select.Option value="MEDICAMENTO">Medicamento</Select.Option>
                <Select.Option value="PAGO_UNICO">Pago Único</Select.Option>
                <Select.Option value="OTRO">Otro</Select.Option>
              </Select>
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="fechaCobro" label="Fecha de Cobro" style={{ width: '50%' }}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="estado" label="Estado" style={{ width: '50%' }}>
              <Select>
                <Select.Option value="PENDIENTE">Pendiente</Select.Option>
                <Select.Option value="PAGADO">Pagado</Select.Option>
                <Select.Option value="ANULADO">Anulado</Select.Option>
              </Select>
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="tipoComprobante" label="Comprobante" style={{ width: '50%' }}>
              <Select placeholder="Tipo">
                <Select.Option value="BOLETA">Boleta</Select.Option>
                <Select.Option value="FACTURA">Factura</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item name="numDocumento" label="N° Documento" style={{ width: '50%' }}>
              <Input placeholder="DNI/RUC" />
            </Form.Item>
          </div>
          <Form.Item name="descripcion" label="Descripción">
            <Input.TextArea rows={2} placeholder="Descripción del cobro" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Pago Único</Text>}
        open={pagoUnicoOpen} onCancel={() => { setPagoUnicoOpen(false); setPagoPaso(0); setSelectedPaciente(null); setSelectedRecetas([]); setSelectedExamenes([]); setSelectedCobros([]); }} width={640} destroyOnClose
        footer={pagoPaso === 2 ? [
          <Button key="submit" type="primary" icon={<CheckCircleOutlined />} size="large"
            loading={pagoUnicoMutation.isPending} onClick={() => setPagoPaso(3)}
            style={{ background: 'linear-gradient(135deg, #00D4AA, #059669)', border: 'none', height: 48, fontWeight: 600 }}>
            Cliente pagó — Confirmar Pago S/. {montoFinal().toFixed(2)}
          </Button>,
        ] : pagoPaso === 3 ? [
          <Button key="back" onClick={() => setPagoPaso(2)}>Atrás</Button>,
          <Button key="submit" type="primary" loading={pagoUnicoMutation.isPending} onClick={handleProcesarPago}>
            Procesar Pago
          </Button>,
        ] : pagoPaso === 4 ? [
          <Button key="close" type="primary" onClick={() => {
            setPagoUnicoOpen(false); setPagoPaso(0); setSelectedPaciente(null);
            setSelectedRecetas([]); setSelectedExamenes([]); setSelectedCobros([]); pagoForm.resetFields();
          }}>Cerrar</Button>,
        ] : undefined}
        styles={{ body: { padding: '24px 28px', minHeight: 350 } }}>
        <Steps current={pagoPaso} style={{ marginBottom: 24 }} items={[
          { title: 'Paciente' }, { title: 'Deudas' }, { title: 'Yape QR' }, { title: 'Comprobante' }, { title: 'Éxito' },
        ]} />

        {pagoPaso === 0 && (
          <div>
            <Text style={{ display: 'block', marginBottom: 12 }}>Buscar paciente por DNI:</Text>
            <PacienteSearchByDni pacientes={pacientes || []} onSelect={seleccionarPaciente} placeholder="Escriba DNI para buscar..." autoFocus />
          </div>
        )}

        {pagoPaso === 1 && selectedPaciente && (
          <div>
            <div style={{ padding: '8px 12px', background: 'rgba(0,212,170,0.08)', borderRadius: 8, marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Space><UserOutlined /><Text strong>{selectedPaciente.nombres} {selectedPaciente.apellidoPaterno}</Text><Tag style={{ borderRadius: 4 }}>{selectedPaciente.dni}</Tag></Space>
              <Button type="text" icon={<CloseOutlined />} onClick={() => { setSelectedPaciente(null); setPagoPaso(0); setSelectedRecetas([]); setSelectedExamenes([]); setSelectedCobros([]); }} style={{ color: '#EF4444' }} title="Quitar paciente" />
            </div>

            {(deudasData?.recetas || []).length === 0 && (deudasData?.examenes || []).length === 0 && (deudasData?.cobros || []).length === 0 && (
              <Text style={{ color: 'var(--text-muted)' }}>No tiene deudas pendientes.</Text>
            )}

            {(deudasData?.recetas || []).length > 0 || (deudasData?.examenes || []).length > 0 || (deudasData?.cobros || []).length > 0 ? (
              <div style={{ marginBottom: 12 }}>
                <Space>
                  <Button size="small" onClick={selectAll}>Seleccionar Todo</Button>
                  <Button size="small" onClick={deselectAll}>Deseleccionar Todo</Button>
                </Space>
              </div>
            ) : null}

            {deudasData?.recetas && deudasData.recetas.length > 0 && (
              <div style={{ marginBottom: 16 }}>
                <Text strong style={{ display: 'block', marginBottom: 8 }}>Recetas Pendientes</Text>
                {deudasData.recetas.map(r => {
                  const recetaPrecio = r.costo ?? precioReceta;
                  return (
                  <div key={r.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
                    <input type="checkbox" checked={selectedRecetas.includes(r.id!)}
                      onChange={() => setSelectedRecetas(prev =>
                        prev.includes(r.id!) ? prev.filter(id => id !== r.id) : [...prev, r.id!]
                      )} />
                    <Text>#{r.id} - {r.medicamentos?.substring(0, 60)}...</Text>
                    {recetaPrecio > 0 && <Tag style={{ marginLeft: 'auto', borderRadius: 4 }}>S/. {recetaPrecio.toFixed(2)}</Tag>}
                  </div>
                  );
                })}
              </div>
            )}

            {deudasData?.examenes && deudasData.examenes.length > 0 && (
              <div style={{ marginBottom: 16 }}>
                <Text strong style={{ display: 'block', marginBottom: 8 }}>Exámenes Pendientes</Text>
                {deudasData.examenes.map(e => (
                  <div key={e.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
                    <input type="checkbox" checked={selectedExamenes.includes(e.id!)}
                      onChange={() => setSelectedExamenes(prev =>
                        prev.includes(e.id!) ? prev.filter(id => id !== e.id) : [...prev, e.id!]
                      )} />
                    <Text>#{e.id} - {e.descripcion?.substring(0, 60)}...</Text>
                    <Tag style={{ marginLeft: 'auto', borderRadius: 4 }}>S/. {getExamenPrecio(e).toFixed(2)}</Tag>
                  </div>
                ))}
              </div>
            )}

            {deudasData?.cobros && deudasData.cobros.length > 0 && (
              <div style={{ marginBottom: 16 }}>
                <Text strong style={{ display: 'block', marginBottom: 8 }}>Cobros Pendientes</Text>
                {deudasData.cobros.map(c => (
                  <div key={c.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
                    <input type="checkbox" checked={selectedCobros.includes(c.id!)}
                      onChange={() => setSelectedCobros(prev =>
                        prev.includes(c.id!) ? prev.filter(id => id !== c.id) : [...prev, c.id!]
                      )} />
                    <Text>#{c.id} - {c.descripcion || c.tipo}</Text>
                    <Tag style={{ marginLeft: 'auto', borderRadius: 4 }}>S/. {c.monto.toFixed(2)}</Tag>
                  </div>
                ))}
              </div>
            )}

            {(deudasData?.recetas || []).length > 0 || (deudasData?.examenes || []).length > 0 || (deudasData?.cobros || []).length > 0 ? (
              <div style={{ background: '#f0fdf4', padding: '12px 16px', borderRadius: 8 }}>
                <Text style={{ fontWeight: 600 }}>Resumen:</Text>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4 }}>
                  <Text style={{ color: 'var(--text-secondary)' }}>Subtotal ({selectedRecetas.length + selectedExamenes.length + selectedCobros.length} items)</Text>
                  <Text style={{ fontWeight: 600 }}>S/. {montoBase().toFixed(2)}</Text>
                </div>
                {descuentoGlobal > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Text style={{ color: '#059669' }}>Descuento campaña ({descuentoGlobal}%)</Text>
                    <Text style={{ color: '#059669', fontWeight: 600 }}>- S/. {montoDescuento().toFixed(2)}</Text>
                  </div>
                )}
                <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid #d1fae5', marginTop: 4, paddingTop: 4 }}>
                  <Text style={{ fontWeight: 700, fontSize: 16, color: '#065f46' }}>Total a pagar</Text>
                  <Text style={{ fontWeight: 700, fontSize: 16, color: '#059669' }}>S/. {montoFinal().toFixed(2)}</Text>
                </div>
              </div>
            ) : null}

            <div style={{ marginTop: 16 }}>
              <Button type="primary" disabled={montoFinal() <= 0} onClick={() => setPagoPaso(2)}
                style={isMobileDevice() ? { background: '#7C3AED', borderColor: '#7C3AED', boxShadow: '0 4px 12px rgba(124,58,237,0.3)' } : {}}>
                {isMobileDevice() ? 'Pagar con Yape' : 'Continuar al Pago'}
              </Button>
            </div>
          </div>
        )}

        {pagoPaso === 2 && selectedPaciente && (
          <div style={{ textAlign: 'center' }}>
            <div style={{ padding: '8px 12px', background: 'rgba(0,212,170,0.08)', borderRadius: 8, marginBottom: 16, textAlign: 'left', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Space><UserOutlined /><Text strong>{selectedPaciente.nombres} {selectedPaciente.apellidoPaterno}</Text><Tag style={{ borderRadius: 4 }}>{selectedPaciente.dni}</Tag></Space>
              <Button type="text" icon={<CloseOutlined />} onClick={() => { setSelectedPaciente(null); setPagoPaso(0); setSelectedRecetas([]); setSelectedExamenes([]); setSelectedCobros([]); }} style={{ color: '#EF4444' }} title="Cambiar paciente" />
            </div>

            {isMobileDevice() ? (
              <div>
                <div style={{ background: 'linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%)', borderRadius: 16, padding: 24, marginBottom: 16, border: '2px solid #7C3AED' }}>
                  <Text style={{ display: 'block', fontSize: 13, color: '#6b7280', marginBottom: 4 }}>Total a pagar</Text>
                  {descuentoGlobal > 0 && (
                    <Text style={{ display: 'block', fontSize: 14, color: '#7C3AED', marginBottom: 4 }}>
                      {descuentoGlobal}% desc. campaña aplicado
                    </Text>
                  )}
                  <Text style={{ display: 'block', fontSize: 36, fontWeight: 700, color: '#7C3AED', marginBottom: 24 }}>
                    S/. {montoFinal().toFixed(2)}
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
                    Presiona "Pagar con Yape" para abrir la app. Confirma el monto y yapea. Luego vuelve y presiona "Siguiente".
                  </Text>
                </div>
              </div>
            ) : (
              <div>
                <div style={{ background: 'linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%)', borderRadius: 16, padding: 24, marginBottom: 16, border: '2px solid #00D4AA', position: 'relative', overflow: 'hidden' }}>
                  <div style={{ position: 'absolute', top: -30, right: -30, width: 100, height: 100, borderRadius: 20, background: 'rgba(0,212,170,0.05)', transform: 'rotate(45deg)' }} />
                  <div style={{ position: 'absolute', bottom: -20, left: -20, width: 60, height: 60, borderRadius: 12, background: 'rgba(0,212,170,0.04)', transform: 'rotate(25deg)' }} />

                  <Text style={{ display: 'block', fontSize: 13, color: '#6b7280', marginBottom: 4 }}>Total a pagar</Text>
                  {descuentoGlobal > 0 && (
                    <Text style={{ display: 'block', fontSize: 14, color: '#059669', marginBottom: 4 }}>
                      {descuentoGlobal}% desc. campaña aplicado
                    </Text>
                  )}
                  <Text style={{ display: 'block', fontSize: 36, fontWeight: 700, color: '#059669', marginBottom: 20 }}>
                    S/. {montoFinal().toFixed(2)}
                  </Text>

                  <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 16 }}>
                    <div style={{ padding: 8, background: '#fff', borderRadius: 16, boxShadow: '0 4px 16px rgba(0,212,170,0.15)', border: '2px solid #00D4AA' }}>
                      <img src={qrDataUrl || undefined}
                        alt="QR Yape" style={{ width: 180, height: 180, borderRadius: 8, display: 'block' }} />
                    </div>
                  </div>

                  <div style={{ background: '#d1fae5', borderRadius: 12, padding: 16, maxWidth: 320, margin: '0 auto' }}>
                    <Space>
                      <CreditCardOutlined style={{ fontSize: 24, color: '#059669' }} />
                      <div>
                        <Text style={{ display: 'block', fontWeight: 600, fontSize: 16, color: '#065f46' }}>Yape: +51 {YAPE_NUMERO}</Text>
                        <Text style={{ fontSize: 12, color: '#065f46' }}>{nombreClinica || 'MedTrack'}</Text>
                      </div>
                    </Space>
                  </div>

                  <Divider style={{ margin: '16px 0' }} />

                  <Text style={{ display: 'block', fontSize: 13, color: '#6b7280' }}>
                    <HeartOutlined style={{ color: '#059669', marginRight: 6 }} />
                    El paciente escanea el QR, paga con Yape y muestra su comprobante a atención al cliente
                  </Text>
                </div>

                <div style={{ background: '#fefce8', borderRadius: 12, padding: 12, marginBottom: 16, textAlign: 'left' }}>
                  <Text style={{ fontSize: 12, color: '#92400e' }}>
                    <strong>Instrucciones:</strong> Confirma el pago solo después de verificar en el celular del paciente que el pago fue exitoso.
                  </Text>
                </div>
              </div>
            )}
          </div>
        )}

        {pagoPaso === 3 && selectedPaciente && (
          <div>
            <div style={{ padding: '8px 12px', background: 'rgba(0,212,170,0.08)', borderRadius: 8, marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Space><UserOutlined /><Text strong>{selectedPaciente.nombres} {selectedPaciente.apellidoPaterno}</Text><Tag style={{ borderRadius: 4 }}>{selectedPaciente.dni}</Tag></Space>
              <Button type="text" icon={<CloseOutlined />} onClick={() => { setSelectedPaciente(null); setPagoPaso(0); setSelectedRecetas([]); setSelectedExamenes([]); setSelectedCobros([]); }} style={{ color: '#EF4444' }} title="Cambiar paciente" />
            </div>
            <Form form={pagoForm} layout="vertical" preserve={false} initialValues={{ tipoComprobante: 'BOLETA', numDocumento: '' }}
              style={{ width: '100%' }}>
              <div style={{ display: 'flex', gap: 16 }}>
                <Form.Item name="tipoComprobante" label="Tipo de Comprobante" rules={[{ required: true }]} style={{ width: '50%' }}>
                  <Select>
                    <Select.Option value="BOLETA">Boleta de Venta (DNI)</Select.Option>
                    <Select.Option value="FACTURA">Factura (RUC)</Select.Option>
                  </Select>
                </Form.Item>
                <Form.Item noStyle shouldUpdate={(prev, cur) => prev.tipoComprobante !== cur.tipoComprobante}>
                  {({ getFieldValue }) => {
                    const tipo = getFieldValue('tipoComprobante');
                    return (
                      <Form.Item name="numDocumento" label={tipo === 'BOLETA' ? 'DNI' : 'RUC'}
                        rules={[
                          { required: true, message: 'Ingrese documento' },
                          { min: tipo === 'BOLETA' ? 8 : 11, message: tipo === 'BOLETA' ? 'DNI: 8 dígitos' : 'RUC: 11 dígitos' },
                          { max: tipo === 'BOLETA' ? 8 : 11, message: tipo === 'BOLETA' ? 'DNI: 8 dígitos' : 'RUC: 11 dígitos' },
                        ]} style={{ width: '50%' }}>
                        <Input placeholder={tipo === 'BOLETA' ? '12345678' : '20123456789'} maxLength={tipo === 'BOLETA' ? 8 : 11} />
                      </Form.Item>
                    );
                  }}
                </Form.Item>
              </div>
              <div style={{ background: '#f0fdf4', padding: '14px 18px', borderRadius: 8, marginTop: 8 }}>
                <Text style={{ fontSize: 14 }}>Resumen:</Text>
                <Text style={{ display: 'block', fontSize: 13, color: 'var(--text-secondary)' }}>{selectedRecetas.length} receta(s) + {selectedExamenes.length} examen(es) + {selectedCobros.length} cobro(s) pendiente(s)</Text>
                {descuentoGlobal > 0 && (
                  <Text style={{ display: 'block', fontSize: 12, color: '#059669' }}>Descuento campaña {descuentoGlobal}%</Text>
                )}
                <Text style={{ display: 'block', fontSize: 18, fontWeight: 700, color: '#059669', marginTop: 4 }}>Total: S/. {montoFinal().toFixed(2)}</Text>
              </div>
            </Form>
          </div>
        )}

        {pagoPaso === 4 && (
          <div style={{ textAlign: 'center', padding: '20px 0' }}>
            <DotLottieReact
              src="https://lottie.host/7e7dc288-cab2-443b-9b27-cef98b3f0b1b/Xng1hEKqZ3.lottie"
              autoplay loop style={{ width: 280, height: 280, margin: '0 auto' }} />
            <Title level={4} style={{ marginTop: 8, color: '#059669' }}>¡Pago procesado con éxito!</Title>
            <Text style={{ color: 'var(--text-muted)' }}>El comprobante se ha generado correctamente</Text>
          </div>
        )}
      </Modal>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Tipo de Comprobante</Text>}
        open={printModal.open} onCancel={() => setPrintModal({ open: false, cobro: null })} onOk={handlePrintConfirm}
        okText="Generar e Imprimir" width={440} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={printForm} layout="vertical" preserve={false} initialValues={{ tipo: 'BOLETA', numDoc: '' }}
          style={{ width: '100%' }}>
          {printModal.cobro && (() => {
            const p = pacienteMap.get(printModal.cobro.pacienteId);
            return p ? (
              <div style={{ padding: '8px 12px', background: 'rgba(139,92,246,0.08)', borderRadius: 8, marginBottom: 16 }}>
                <Space><UserOutlined /><Text strong>{p.nombres} {p.apellidoPaterno}</Text><Tag style={{ borderRadius: 4 }}>{p.dni}</Tag></Space>
              </div>
            ) : null;
          })()}
          <Form.Item name="tipo" label="Tipo de Documento" rules={[{ required: true }]}>
            <Select>
              <Select.Option value="BOLETA">Boleta de Venta (DNI)</Select.Option>
              <Select.Option value="FACTURA">Factura (RUC)</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(prev, cur) => prev.tipo !== cur.tipo}>
            {({ getFieldValue }) => {
              const tipo = getFieldValue('tipo');
              return (
                <Form.Item name="numDoc" label={tipo === 'BOLETA' ? 'DNI del Cliente' : 'RUC del Cliente'}>
                  <Input placeholder={tipo === 'BOLETA' ? '12345678' : '20123456789'} maxLength={tipo === 'BOLETA' ? 8 : 11} style={{ width: '100%' }} />
                </Form.Item>
              );
            }}
          </Form.Item>
          <div style={{ background: '#f0fdf4', padding: '10px 14px', borderRadius: 8, marginTop: 8 }}>
            <Text style={{ fontSize: 12, color: '#059669' }}>
              Se generará un comprobante electrónico con los datos de la clínica y el paciente.
            </Text>
          </div>
        </Form>
      </Modal>
    </div>
  );
}