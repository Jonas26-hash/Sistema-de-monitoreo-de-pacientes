import { Table, Button, Modal, Form, Input, InputNumber, Select, DatePicker, Tag, Space, Input as SearchInput, Typography, message, Steps, Divider } from 'antd';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, DollarOutlined, PrinterOutlined, WalletOutlined, CheckCircleOutlined, UserOutlined, CreditCardOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useCrud } from '../../hooks/useCrud';
import { showCrudSuccess } from '../../utils/notifications';
import type { Cobro, Paciente, OrdenExamen, Receta, Servicio, Campania } from '../../types';
import api from '../../services/api';
import PacienteSearchByDni from '../../components/paciente/PacienteSearchByDni';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const statusColors: Record<string, string> = { PENDIENTE: '#F59E0B', PAGADO: '#00D4AA', ANULADO: '#EF4444' };

const YAPE_NUMERO = '987654321';

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

  const { data: campaniasActivas } = useQuery({
    queryKey: ['campanias-activas'],
    queryFn: async () => { const r = await api.get('/campanias/activas'); return r.data as Campania[]; },
  });

  const precioReceta = servicios?.find(s => s.tipo === 'RECETA')?.precio ?? 0;
  const serviciosExamen = servicios?.filter(s => s.tipo === 'EXAMEN') || [];
  const descuentoGlobal = campaniasActivas?.reduce((max, c) => Math.max(max, c.descuentoPorcentaje), 0) || 0;

  const pacienteMap = new Map(pacientes?.map(p => [p.id, p]) || []);

  const { data: deudasData, refetch: refetchDeudas } = useQuery({
    queryKey: ['deudas-paciente', selectedPaciente?.id],
    queryFn: async () => {
      if (!selectedPaciente) return { recetas: [], examenes: [] };
      const res = await api.get(`/cobros/deudas/${selectedPaciente.id}`);
      return res.data as { recetas: Receta[]; examenes: OrdenExamen[] };
    },
    enabled: !!selectedPaciente,
  });

  const pagoUnicoMutation = useMutation({
    mutationFn: (values: { pacienteId: number; recetaIds: number[]; examenIds: number[]; monto: number; tipoComprobante: string; numDocumento: string; descripcion: string }) =>
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
        pagoForm.resetFields();
      }, 3000);
    },
    onError: () => Modal.error({ title: 'Error', content: 'Error al procesar pago único' }),
  });

  const getExamenPrecio = (examen: OrdenExamen) => {
    if (examen.costo && examen.costo > 0) return examen.costo;
    const match = serviciosExamen[0];
    return match?.precio || 0;
  };

  const montoBase = () => {
    let total = 0;
    const recetas = deudasData?.recetas || [];
    const examenes = deudasData?.examenes || [];
    selectedRecetas.forEach(id => { const r = recetas.find(r => r.id === id); if (r) total += precioReceta; });
    selectedExamenes.forEach(id => { const e = examenes.find(e => e.id === id); if (e) total += getExamenPrecio(e); });
    return total;
  };

  const montoDescuento = () => descuentoGlobal > 0 ? montoBase() * descuentoGlobal / 100 : 0;
  const montoFinal = () => montoBase() - montoDescuento();

  const selectAll = () => {
    const recetas = deudasData?.recetas || [];
    const examenes = deudasData?.examenes || [];
    setSelectedRecetas(recetas.map(r => r.id!));
    setSelectedExamenes(examenes.map(e => e.id!));
  };

  const deselectAll = () => {
    setSelectedRecetas([]);
    setSelectedExamenes([]);
  };

  const generateComprobante = (c: Cobro, tipo: 'BOLETA' | 'FACTURA', numDoc: string) => {
    const paciente = pacienteMap.get(c.pacienteId);
    const nombre = paciente ? `${paciente.nombres} ${paciente.apellidoPaterno}${paciente.apellidoMaterno ? ' ' + paciente.apellidoMaterno : ''}` : `#${c.pacienteId}`;
    const rucClinica = '20457698321';
    const nombreClinica = 'Clínica San Pablo E.I.R.L.';
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
        <div class="logo">${nombreClinica}</div>
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
    printForm.resetFields();
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
    pagoForm.resetFields();
    setPagoUnicoOpen(true);
  };

  const seleccionarPaciente = (p: Paciente) => {
    setSelectedPaciente(p);
    setSelectedRecetas([]);
    setSelectedExamenes([]);
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
        monto: montoFinal(),
        tipoComprobante: values.tipoComprobante,
        numDocumento: values.numDocumento,
        descripcion: `Pago único - ${selectedRecetas.length} receta(s), ${selectedExamenes.length} examen(es)`,
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
          <SearchInput.Search placeholder="Buscar cobros..." allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button icon={<WalletOutlined />} onClick={abrirPagoUnico} style={{ borderColor: '#00D4AA', color: '#00D4AA' }}>Pago Único</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nuevo Cobro</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'hidden' }}>
        <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={loading}
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
        open={pagoUnicoOpen} onCancel={() => { setPagoUnicoOpen(false); setPagoPaso(0); setSelectedPaciente(null); }} width={640} destroyOnClose
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
            setSelectedRecetas([]); setSelectedExamenes([]); pagoForm.resetFields();
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
            <div style={{ padding: '8px 12px', background: 'rgba(0,212,170,0.08)', borderRadius: 8, marginBottom: 16 }}>
              <Space><UserOutlined /><Text strong>{selectedPaciente.nombres} {selectedPaciente.apellidoPaterno}</Text><Tag style={{ borderRadius: 4 }}>{selectedPaciente.dni}</Tag></Space>
            </div>

            {(deudasData?.recetas || []).length === 0 && (deudasData?.examenes || []).length === 0 && (
              <Text style={{ color: 'var(--text-muted)' }}>No tiene deudas pendientes.</Text>
            )}

            {(deudasData?.recetas || []).length > 0 || (deudasData?.examenes || []).length > 0 ? (
              <div style={{ marginBottom: 12 }}>
                <Space>
                  <Button size="small" onClick={selectAll}>Seleccionar Todo</Button>
                  <Button size="small" onClick={deselectAll}>Deseleccionar Todo</Button>
                </Space>
              </div>
            ) : null}

            {deudasData?.recetas && deudasData.recetas.length > 0 && (
              <div style={{ marginBottom: 16 }}>
                <Text strong style={{ display: 'block', marginBottom: 8 }}>Recetas Pendientes {precioReceta > 0 ? `(S/. ${precioReceta.toFixed(2)} c/u)` : ''}</Text>
                {deudasData.recetas.map(r => (
                  <div key={r.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
                    <input type="checkbox" checked={selectedRecetas.includes(r.id!)}
                      onChange={() => setSelectedRecetas(prev =>
                        prev.includes(r.id!) ? prev.filter(id => id !== r.id) : [...prev, r.id!]
                      )} />
                    <Text>#{r.id} - {r.medicamentos?.substring(0, 60)}...</Text>
                    {precioReceta > 0 && <Tag style={{ marginLeft: 'auto', borderRadius: 4 }}>S/. {precioReceta.toFixed(2)}</Tag>}
                  </div>
                ))}
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

            {(deudasData?.recetas || []).length > 0 || (deudasData?.examenes || []).length > 0 ? (
              <div style={{ background: '#f0fdf4', padding: '12px 16px', borderRadius: 8 }}>
                <Text style={{ fontWeight: 600 }}>Resumen:</Text>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4 }}>
                  <Text style={{ color: 'var(--text-secondary)' }}>Subtotal ({selectedRecetas.length + selectedExamenes.length} items)</Text>
                  <Text style={{ fontWeight: 600 }}>S/. {montoBase().toFixed(2)}</Text>
                </div>
                {descuentoGlobal > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Text style={{ color: '#059669' }}>Descuento campaña ({descuentoGlobal}%)</Text>
                    <Text style={{ color: '#059669', fontWeight: 600 }}>- S/. {montoDescuento().toFixed(2)}</Text>
                  </div>
                )}
                <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid #d1fae5', marginTop: 4, paddingTop: 4 }}>
                  <Text style={{ fontWeight: 700, fontSize: 16 }}>Total a pagar</Text>
                  <Text style={{ fontWeight: 700, fontSize: 16, color: '#059669' }}>S/. {montoFinal().toFixed(2)}</Text>
                </div>
              </div>
            ) : null}

            <div style={{ marginTop: 16 }}>
              <Button type="primary" disabled={montoFinal() <= 0} onClick={() => setPagoPaso(2)}>
                Continuar al Pago
              </Button>
            </div>
          </div>
        )}

        {pagoPaso === 2 && selectedPaciente && (
          <div style={{ textAlign: 'center' }}>
            <div style={{ padding: '8px 12px', background: 'rgba(0,212,170,0.08)', borderRadius: 8, marginBottom: 16, textAlign: 'left' }}>
              <Space><UserOutlined /><Text strong>{selectedPaciente.nombres} {selectedPaciente.apellidoPaterno}</Text><Tag style={{ borderRadius: 4 }}>{selectedPaciente.dni}</Tag></Space>
            </div>

            <div style={{ background: '#fff', borderRadius: 16, padding: 24, marginBottom: 16, border: '2px solid #00D4AA' }}>
              <Text style={{ display: 'block', fontSize: 13, color: 'var(--text-muted)', marginBottom: 4 }}>Total a pagar</Text>
              {descuentoGlobal > 0 && (
                <Text style={{ display: 'block', fontSize: 14, color: '#059669', marginBottom: 4 }}>
                  {descuentoGlobal}% desc. campaña aplicado
                </Text>
              )}
              <Text style={{ display: 'block', fontSize: 36, fontWeight: 700, color: '#059669', marginBottom: 20 }}>
                S/. {montoFinal().toFixed(2)}
              </Text>

              <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 16 }}>
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=YAPE%3A${YAPE_NUMERO}%7CS%2F.${montoFinal().toFixed(2)}`}
                  alt="QR Yape"
                  style={{ width: 200, height: 200, borderRadius: 12, border: '1px solid #e5e7eb' }}
                  onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
                />
              </div>

              <div style={{ background: '#f0fdf4', borderRadius: 12, padding: 16, maxWidth: 320, margin: '0 auto' }}>
                <Space>
                  <CreditCardOutlined style={{ fontSize: 24, color: '#059669' }} />
                  <div>
                    <Text style={{ display: 'block', fontWeight: 600, fontSize: 16 }}>Yape: {YAPE_NUMERO}</Text>
                    <Text style={{ fontSize: 12, color: 'var(--text-muted)' }}>Clínica San Pablo E.I.R.L.</Text>
                  </div>
                </Space>
              </div>

              <Divider style={{ margin: '16px 0' }} />

              <Text style={{ display: 'block', fontSize: 13, color: 'var(--text-muted)' }}>
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

        {pagoPaso === 3 && selectedPaciente && (
          <div>
            <div style={{ padding: '8px 12px', background: 'rgba(0,212,170,0.08)', borderRadius: 8, marginBottom: 16 }}>
              <Space><UserOutlined /><Text strong>{selectedPaciente.nombres} {selectedPaciente.apellidoPaterno}</Text><Tag style={{ borderRadius: 4 }}>{selectedPaciente.dni}</Tag></Space>
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
                <Text style={{ display: 'block', fontSize: 13, color: 'var(--text-secondary)' }}>{selectedRecetas.length} receta(s) + {selectedExamenes.length} examen(es)</Text>
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
                <Form.Item name="numDoc" label={tipo === 'BOLETA' ? 'DNI del Cliente' : 'RUC del Cliente'}
                  rules={[
                    { required: true, message: 'Ingrese el número de documento' },
                    { min: tipo === 'BOLETA' ? 8 : 11, message: tipo === 'BOLETA' ? 'DNI: 8 dígitos' : 'RUC: 11 dígitos' },
                    { max: tipo === 'BOLETA' ? 8 : 11, message: tipo === 'BOLETA' ? 'DNI: 8 dígitos' : 'RUC: 11 dígitos' },
                  ]}>
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