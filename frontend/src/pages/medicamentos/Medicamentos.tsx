import { Table, Button, Modal, Form, Input, InputNumber, Tag, Space, Input as SearchInput, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, ShoppingCartOutlined } from '@ant-design/icons';
import { useCrud } from '../../hooks/useCrud';
import type { Medicamento } from '../../types';

const { Title, Text } = Typography;

export default function Medicamentos() {
  const [form] = Form.useForm();
  const { search, setSearch, data, loading, page, setPage, modalOpen, editing, openCreate, openEdit, closeModal, handleSave, handleDelete } = useCrud<Medicamento>({
    key: 'medicamentos',
    endpoint: '/medicamentos',
  });

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60, render: (v: number) => <Text style={{ color: 'var(--text-muted)' }}>{v}</Text> },
    { title: 'Código', dataIndex: 'codigo', key: 'codigo', render: (v: string) => <Tag style={{ borderRadius: 4 }}>{v}</Tag> },
    { title: 'Nombre', dataIndex: 'nombre', key: 'nombre', render: (v: string) => <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{v}</Text> },
    { title: 'Presentación', dataIndex: 'presentacion', key: 'presentacion', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    {
      title: 'Stock', dataIndex: 'stock', key: 'stock',
      render: (v: number, r: Medicamento) => {
        const color = v > (r.stockMinimo || 0) * 2 ? '#00D4AA' : v > (r.stockMinimo || 0) ? '#F59E0B' : '#EF4444';
        return <Tag color={color} style={{ borderRadius: 4 }}>{v}</Tag>;
      },
    },
    {
      title: 'Precio', dataIndex: 'precio', key: 'precio',
      render: (v: number) => v != null ? <Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>S/. {v.toFixed(2)}</Text> : <Text style={{ color: 'var(--text-muted)' }}>—</Text>,
    },
    {
      title: '', key: 'acciones', width: 100,
      render: (_: unknown, r: Medicamento) => (
        <Space>
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
              <ShoppingCartOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Medicamentos</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Inventario de medicamentos</Text>
            </div>
          </Space>
        </div>
        <Space>
          <SearchInput.Search placeholder="Buscar medicamentos..." allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nuevo Medicamento</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'hidden' }}>
        <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={loading}
          pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false }} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{editing ? 'Editar Medicamento' : 'Nuevo Medicamento'}</Text>}
        open={modalOpen} onCancel={closeModal} onOk={() => form.submit()} okText={editing ? 'Actualizar' : 'Crear'} width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={handleSave} initialValues={editing || {}} preserve={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="codigo" label="Código" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="MED-001" />
            </Form.Item>
            <Form.Item name="nombre" label="Nombre" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Paracetamol" />
            </Form.Item>
          </div>
          <Form.Item name="descripcion" label="Descripción">
            <Input.TextArea rows={2} placeholder="Descripción del medicamento" />
          </Form.Item>
          <Form.Item name="presentacion" label="Presentación">
            <Input placeholder="Tabletas x 30 / Jarabe 120ml" style={{ width: '100%' }} />
          </Form.Item>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="stock" label="Stock" rules={[{ required: true }]} style={{ width: '33%' }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="stockMinimo" label="Stock Mínimo" style={{ width: '33%' }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="precio" label="Precio (S/.)" style={{ width: '33%' }}>
              <InputNumber min={0} step={0.01} prefix="S/. " style={{ width: '100%' }} />
            </Form.Item>
          </div>
          <Form.Item name="contraindicaciones" label="Contraindicaciones">
            <Input.TextArea rows={2} placeholder="Contraindicaciones del medicamento" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}