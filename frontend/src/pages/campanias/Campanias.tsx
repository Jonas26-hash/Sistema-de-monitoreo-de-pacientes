import { useState } from 'react';
import { Table, Button, Modal, Form, Input, InputNumber, DatePicker, Tag, Space, Typography, message, Switch } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, TagsOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { showCrudSuccess } from '../../utils/notifications';
import api from '../../services/api';
import type { Campania } from '../../types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const statusColors: Record<string, string> = { true: '#00D4AA', false: '#EF4444' };

export default function Campanias() {
  const [form] = Form.useForm();
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Campania | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['campanias'],
    queryFn: async () => { const r = await api.get('/campanias'); return r.data as Campania[]; },
  });

  const createMutation = useMutation({
    mutationFn: async (values: Partial<Campania>) => {
      const payload = {
        ...values,
        fechaInicio: dayjs(values.fechaInicio).format('YYYY-MM-DD'),
        fechaFin: dayjs(values.fechaFin).format('YYYY-MM-DD'),
      };
      if (editing) {
        const r = await api.put(`/campanias/${editing.id}`, payload);
        return r.data;
      }
      const r = await api.post('/campanias', payload);
      return r.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['campanias'] });
      showCrudSuccess(editing ? 'actualizada' : 'creada', 'Campaña');
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
    },
    onError: () => message.error('Error al guardar campaña'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/campanias/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['campanias'] });
      showCrudSuccess('eliminada', 'Campaña');
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (id: number) => api.put(`/campanias/${id}/toggle`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['campanias'] });
      showCrudSuccess('actualizado', 'Estado de campaña');
    },
    onError: () => message.error('Error al cambiar estado'),
  });

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (c: Campania) => {
    setEditing(c);
    form.setFieldsValue({
      ...c,
      rango: [dayjs(c.fechaInicio), dayjs(c.fechaFin)],
    });
    setModalOpen(true);
  };

  const columns = [
    { title: 'Código', dataIndex: 'codigo', key: 'codigo', render: (v: string) => <Tag style={{ borderRadius: 4, fontFamily: 'monospace' }}>{v}</Tag> },
    { title: 'Nombre', dataIndex: 'nombre', key: 'nombre', render: (v: string) => <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{v}</Text> },
    { title: 'Descuento', dataIndex: 'descuentoPorcentaje', key: 'descuentoPorcentaje', render: (v: number) => <Tag color="green" style={{ borderRadius: 4, fontWeight: 600 }}>{v}%</Tag> },
    { title: 'Vigencia', key: 'vigencia', render: (_: unknown, r: Campania) => (
      <Text style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{dayjs(r.fechaInicio).format('DD/MM/YYYY')} - {dayjs(r.fechaFin).format('DD/MM/YYYY')}</Text>
    )},
    { title: 'Activo', dataIndex: 'activo', key: 'activo', render: (v: boolean, r: Campania) => <Switch checked={v} onChange={() => toggleMutation.mutate(r.id!)} size="small" /> },
    {
      title: '', key: 'acciones', width: 100,
      render: (_: unknown, r: Campania) => (
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
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(139,92,246,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#8B5CF6', fontSize: 20 }}>
              <TagsOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Campañas</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Precios reducidos temporales para campañas de salud</Text>
            </div>
          </Space>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nueva Campaña</Button>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'hidden' }}>
        <Table columns={columns} dataSource={data || []} rowKey="id" loading={isLoading} pagination={false} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{editing ? 'Editar Campaña' : 'Nueva Campaña'}</Text>}
        open={modalOpen} onCancel={() => { setModalOpen(false); setEditing(null); }} onOk={() => form.submit()}
        okText={editing ? 'Actualizar' : 'Crear'} width={560} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={(v) => {
          const payload = { ...v, fechaInicio: v.rango[0].format('YYYY-MM-DD'), fechaFin: v.rango[1].format('YYYY-MM-DD') };
          delete payload.rango;
          createMutation.mutate(payload);
        }} preserve={false} style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="codigo" label="Código" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Ej: CAMP-01" />
            </Form.Item>
            <Form.Item name="descuentoPorcentaje" label="Descuento (%)" rules={[{ required: true }]} style={{ width: '50%' }}>
              <InputNumber min={0} max={100} suffix="%" style={{ width: '100%' }} />
            </Form.Item>
          </div>
          <Form.Item name="nombre" label="Nombre de la Campaña" rules={[{ required: true }]}>
            <Input placeholder="Ej: Campaña de Salud Preventiva" />
          </Form.Item>
          <Form.Item name="descripcion" label="Descripción">
            <Input.TextArea rows={2} placeholder="Descripción opcional" />
          </Form.Item>
          <Form.Item name="rango" label="Período de Vigencia" rules={[{ required: true, message: 'Seleccione fecha inicio y fin' }]}>
            <RangePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}