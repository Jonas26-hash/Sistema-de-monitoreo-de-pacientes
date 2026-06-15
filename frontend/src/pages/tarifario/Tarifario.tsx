import { useState } from 'react';
import { Table, Button, Modal, Form, Input, InputNumber, Select, Tag, Space, Typography, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, DollarOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { showCrudSuccess } from '../../utils/notifications';
import api from '../../services/api';
import type { Servicio } from '../../types';

const { Title, Text } = Typography;

const TIPOS_PREDEFINIDOS = ['CONSULTA', 'EXAMEN', 'RECETA'];

const tipoColors: Record<string, string> = {
  CONSULTA: '#3B82F6', EXAMEN: '#8B5CF6', RECETA: '#F59E0B', OTRO: '#6B7280',
};

export default function Tarifario() {
  const [form] = Form.useForm();
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Servicio | null>(null);
  const [tipoCustom, setTipoCustom] = useState('');
  const [tipoSelect, setTipoSelect] = useState<string>('CONSULTA');
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['servicios'],
    queryFn: async () => { const r = await api.get('/servicios'); return r.data as Servicio[]; },
  });

  const createMutation = useMutation({
    mutationFn: async (values: Servicio) => {
      if (editing) {
        const r = await api.put(`/servicios/${editing.id}`, values);
        return r.data;
      }
      const r = await api.post('/servicios', values);
      return r.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['servicios'] });
      showCrudSuccess(editing ? 'actualizado' : 'creado', 'Servicio');
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
    },
    onError: () => message.error('Error al guardar servicio'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/servicios/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['servicios'] });
      showCrudSuccess('eliminado', 'Servicio');
    },
  });

  const openCreate = () => {
    setEditing(null);
    setTipoSelect('CONSULTA');
    setTipoCustom('');
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (s: Servicio) => {
    setEditing(s);
    const isPredefined = TIPOS_PREDEFINIDOS.includes(s.tipo);
    setTipoSelect(isPredefined ? s.tipo : 'OTRO');
    setTipoCustom(isPredefined ? '' : s.tipo);
    form.setFieldsValue({ ...s, tipo: isPredefined ? s.tipo : 'OTRO' });
    setModalOpen(true);
  };

  const handleTipoChange = (value: string) => {
    setTipoSelect(value);
    if (value !== 'OTRO') {
      setTipoCustom('');
      form.setFieldValue('tipo', value);
    } else {
      form.setFieldValue('tipo', tipoCustom || '');
    }
  };

  const handleCustomTipoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setTipoCustom(e.target.value);
    form.setFieldValue('tipo', e.target.value);
  };

  const columns = [
    { title: 'Código', dataIndex: 'codigo', key: 'codigo', render: (v: string) => <Tag style={{ borderRadius: 4, fontFamily: 'monospace' }}>{v}</Tag> },
    { title: 'Nombre', dataIndex: 'nombre', key: 'nombre', render: (v: string) => <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{v}</Text> },
    { title: 'Tipo', dataIndex: 'tipo', key: 'tipo', render: (v: string) => <Tag color={tipoColors[v] || '#6B7280'} style={{ borderRadius: 4 }}>{v}</Tag> },
    { title: 'Precio', dataIndex: 'precio', key: 'precio', render: (v: number) => <Text style={{ color: 'var(--text-primary)', fontWeight: 600, fontSize: 15 }}>S/. {v?.toFixed(2)}</Text> },
    {
      title: '', key: 'acciones', width: 100,
      render: (_: unknown, r: Servicio) => (
        <Space>
          <Button type="text" icon={<EditOutlined />} style={{ color: '#3B82F6' }} onClick={() => openEdit(r)} />
          <Button type="text" icon={<DeleteOutlined />} style={{ color: '#EF4444' }} onClick={() => deleteMutation.mutate(r.id!)} />
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className="crud-page-header">
        <div>
          <Space align="center" size={12}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(0,212,170,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#00D4AA', fontSize: 20 }}>
              <DollarOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Tarifario</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Catálogo de servicios con tarifas preestablecidas</Text>
            </div>
          </Space>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nuevo Servicio</Button>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'hidden' }}>
        <Table columns={columns} dataSource={data || []} rowKey="id" loading={isLoading} pagination={false} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{editing ? 'Editar Servicio' : 'Nuevo Servicio'}</Text>}
        open={modalOpen} onCancel={() => { setModalOpen(false); setEditing(null); }} onOk={() => form.submit()}
        okText={editing ? 'Actualizar' : 'Crear'} width={520} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={(v) => createMutation.mutate(v)} preserve={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="codigo" label="Código" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Ej: CONS-01" />
            </Form.Item>
            <Form.Item name="tipo" label="Tipo" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Select placeholder="Seleccionar tipo" value={tipoSelect} onChange={handleTipoChange}>
                {TIPOS_PREDEFINIDOS.map(t => <Select.Option key={t} value={t}>{t.charAt(0) + t.slice(1).toLowerCase()}</Select.Option>)}
                <Select.Option value="OTRO">Otro</Select.Option>
              </Select>
            </Form.Item>
          </div>
          {tipoSelect === 'OTRO' && (
            <Form.Item label="Especificar tipo" required>
              <Input placeholder="Ej: CIRUGIA, TERAPIA, TRIAJE" value={tipoCustom} onChange={handleCustomTipoChange} autoFocus />
            </Form.Item>
          )}
          <Form.Item name="nombre" label="Nombre del Servicio" rules={[{ required: true }]}>
            <Input placeholder="Ej: Consulta General" />
          </Form.Item>
          <Form.Item name="precio" label="Precio (S/.)" rules={[{ required: true }]}>
            <InputNumber min={0} step={0.01} prefix="S/. " style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}