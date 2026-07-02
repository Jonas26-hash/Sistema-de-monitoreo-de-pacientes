import { Table, Button, Modal, Form, Input, InputNumber, DatePicker, Switch, Tag, Space, Input as SearchInput, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, MedicineBoxOutlined, PrinterOutlined, UserOutlined } from '@ant-design/icons';
import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useCrud } from '../../hooks/useCrud';
import { useAuth } from '../../context/AuthContext';
import type { Receta, Paciente, Consulta, User } from '../../types';
import api from '../../services/api';
import PacienteSearchByDni from '../../components/paciente/PacienteSearchByDni';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function Recetas() {
  const [form] = Form.useForm();
  const { user } = useAuth();
  const { search, setSearch, data, loading, page, setPage, modalOpen, editing, openCreate, openEdit, closeModal, handleSave, handleDelete } = useCrud<Receta>({
    key: 'recetas',
    endpoint: '/recetas',
    dateFields: ['fechaEmision', 'fechaVigencia'],
  });
  const [selectedPacienteId, setSelectedPacienteId] = useState<number | null>(null);

  const { data: pacientes } = useQuery({
    queryKey: ['pacientes-lista'],
    queryFn: async () => { const r = await api.get('/pacientes'); return r.data as Paciente[]; },
  });
  const pacienteMap = new Map(pacientes?.map(p => [p.id, p]) || []);

  const { data: usuarios } = useQuery({
    queryKey: ['usuarios-todos'],
    queryFn: async () => { const r = await api.get('/auth/usuarios', { params: { size: 100 } }); return r.data.content as User[]; },
  });
  const currentUserId = usuarios?.find(u => u.username === user?.username)?.id;

  const { data: consultasPaciente } = useQuery({
    queryKey: ['consultas-paciente', selectedPacienteId],
    queryFn: async () => { const r = await api.get(`/consultas/paciente/${selectedPacienteId}`); return r.data as Consulta[]; },
    enabled: !!selectedPacienteId,
  });

  useEffect(() => {
    if (editing || !selectedPacienteId) return;
    const latest = consultasPaciente
      ?.sort((a, b) => new Date(b.fechaConsulta).getTime() - new Date(a.fechaConsulta).getTime())[0];
    if (latest && !form.getFieldValue('consultaId')) {
      form.setFieldValue('consultaId', latest.id);
    }
  }, [consultasPaciente, selectedPacienteId, editing, form]);

  const handlePatientSelect = (p: Paciente) => {
    setSelectedPacienteId(p.id);
    form.setFieldValue('pacienteId', p.id);
    if (!editing && currentUserId) form.setFieldValue('doctorId', currentUserId);
  };

  const printReceta = (r: Receta) => {
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
        <div><label>Fecha de Emisión</label><span>${dayjs(r.fechaEmision).format('DD/MM/YYYY')}</span></div>
        <div><label>Paciente</label><span>${(() => { const p = pacienteMap.get(r.pacienteId); return p ? `${p.nombres} ${p.apellidoPaterno}` : `#${r.pacienteId}`; })()}</span></div>
        <div><label>Doctor</label><span>${(() => { const d = usuarios?.find(u => u.id === r.doctorId); return d ? `Dr. ${d.nombres} ${d.apellidos}` : `#${r.doctorId}`; })()}</span></div>
        ${r.fechaVigencia ? `<div><label>Vigencia Hasta</label><span>${dayjs(r.fechaVigencia).format('DD/MM/YYYY')}</span></div>` : ''}
      </div>
      <h2>Medicamentos</h2>
      ${r.medicamentos.split(',').map((m: string) => `<div class="med-item">${m.trim()}</div>`).join('')}
      ${r.indicaciones ? `<h2 style="margin-top:16px">Indicaciones</h2><div class="ind">${r.indicaciones}</div>` : ''}
      <div class="footer"><div class="firma">Firma del Doctor</div></div>
      <p style="text-align:center;color:#94a3b8;font-size:10px;margin-top:24px">Documento generado electrónicamente</p>
      <script>window.print();window.close();<\/script>
    </body></html>`);
    win.document.close();
  };

  const columns = [
    { title: 'Nº', key: 'index', width: 60, render: (_v: unknown, _r: unknown, i: number) => <Text style={{ color: 'var(--text-muted)' }}>{i + 1}</Text> },
    { title: 'Paciente', key: 'pacienteId', render: (v: unknown, r: Receta) => { const p = pacienteMap.get(r.pacienteId); return p ? <Space><UserOutlined style={{ color: '#00D4AA' }} /><Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{p.nombres} {p.apellidoPaterno}</Text><Tag style={{ borderRadius: 4, fontSize: 11 }}>{p.dni}</Tag></Space> : <Text style={{ color: 'var(--text-secondary)' }}>#{r.pacienteId}</Text>; } },
    { title: 'Doctor', key: 'doctorId', render: (v: unknown, r: Receta) => { const d = usuarios?.find(u => u.id === r.doctorId); return d ? <Space><MedicineBoxOutlined style={{ color: '#3B82F6' }} /><Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>Dr. {d.nombres} {d.apellidos}</Text></Space> : <Text style={{ color: 'var(--text-secondary)' }}>#{r.doctorId}</Text>; } },
    { title: 'Medicamentos', dataIndex: 'medicamentos', key: 'medicamentos', ellipsis: true, render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Emisión', dataIndex: 'fechaEmision', key: 'fechaEmision', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{dayjs(v).format('DD/MM/YYYY')}</Text> },
    { title: 'Vence', dataIndex: 'fechaVigencia', key: 'fechaVigencia', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v ? dayjs(v).format('DD/MM/YYYY') : '-'}</Text> },
    {
      title: 'Dispensada', dataIndex: 'dispensada', key: 'dispensada',
      render: (v: boolean) => <Tag color={v ? '#00D4AA' : '#F59E0B'} style={{ borderRadius: 4 }}>{v ? 'Sí' : 'No'}</Tag>,
    },
    {
      title: '', key: 'acciones', width: 140,
      render: (_: unknown, r: Receta) => (
        <Space>
          <Button type="text" icon={<PrinterOutlined />} style={{ color: '#00D4AA' }} onClick={() => printReceta(r)} title="Imprimir Receta" />
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
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(245,158,11,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#F59E0B', fontSize: 20 }}>
              <MedicineBoxOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Recetas</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Gestión de recetas médicas</Text>
            </div>
          </Space>
        </div>
        <Space>
          <SearchInput.Search placeholder="Buscar por DNI o nombre del paciente" allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nueva Receta</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'auto' }}>
        <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={loading} scroll={{ x: 650 }}

          pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false }} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{editing ? 'Editar Receta' : 'Nueva Receta'}</Text>}
        open={modalOpen} onCancel={closeModal} onOk={() => form.submit()} okText={editing ? 'Actualizar' : 'Crear'} width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={handleSave} initialValues={editing || {}} preserve={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="pacienteId" label="Paciente" rules={[{ required: true }]} style={{ width: '50%' }}>
              <PacienteSearchByDni pacientes={pacientes || []} onSelect={handlePatientSelect} value={selectedPacienteId} onChange={(v) => setSelectedPacienteId(v)} />
            </Form.Item>
            <Form.Item name="consultaId" label="ID de Consulta" rules={[{ required: true }]} style={{ width: '50%' }}>
              <InputNumber min={1} style={{ width: '100%' }} placeholder="Auto" disabled />
            </Form.Item>
          </div>
          <Form.Item name="doctorId" hidden><Input /></Form.Item>
          <Form.Item name="medicamentos" label="Medicamentos" rules={[{ required: true }]}>
            <Input placeholder="Paracetamol 500mg, Ibuprofeno 400mg" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="indicaciones" label="Indicaciones">
            <Input.TextArea rows={2} placeholder="Tomar cada 8 horas por 7 días" />
          </Form.Item>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="fechaEmision" label="Fecha de Emisión" rules={[{ required: true }]} style={{ width: '50%' }}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="fechaVigencia" label="Vigencia Hasta" style={{ width: '50%' }}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </div>
          <Form.Item name="dispensada" label="Dispensada" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>


    </div>
  );
}
