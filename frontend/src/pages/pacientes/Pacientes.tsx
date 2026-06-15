import { Table, Button, Modal, Form, Input, Select, DatePicker, Space, Input as SearchInput, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, TeamOutlined } from '@ant-design/icons';
import { useCrud } from '../../hooks/useCrud';
import type { Paciente } from '../../types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function Pacientes() {
  const [form] = Form.useForm();
  const { search, setSearch, data, loading, page, setPage, modalOpen, editing, openCreate, openEdit, closeModal, handleSave, handleDelete } = useCrud<Paciente>({
    key: 'pacientes',
    endpoint: '/pacientes',
    dateFields: ['fechaNacimiento', 'vigenciaSeguro'],
  });

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60, render: (v: number) => <Text style={{ color: 'var(--text-muted)' }}>{v}</Text> },
    { title: 'Paciente', key: 'nombre', render: (_: unknown, r: Paciente) => <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{r.nombres} {r.apellidoPaterno}{r.apellidoMaterno ? ' ' + r.apellidoMaterno : ''}</Text> },
    { title: 'DNI', dataIndex: 'dni', key: 'dni', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Email', dataIndex: 'email', key: 'email', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Teléfono', dataIndex: 'telefono', key: 'telefono', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    {
      title: 'Acciones', key: 'acciones', width: 100,
      render: (_: unknown, r: Paciente) => (
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
            <div style={{ width: 40, height: 40, borderRadius: 10, background: 'rgba(0,212,170,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#00D4AA', fontSize: 20 }}>
              <TeamOutlined />
            </div>
            <div>
              <Title level={4} style={{ margin: 0 }}>Pacientes</Title>
              <Text style={{ color: 'var(--text-muted)' }}>Gestión de pacientes del hospital</Text>
            </div>
          </Space>
        </div>
        <Space>
          <SearchInput.Search placeholder="Buscar pacientes..." allowClear onSearch={setSearch} prefix={<SearchOutlined />} style={{ width: 240 }} />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Nuevo Paciente</Button>
        </Space>
      </div>

      <div className="glass" style={{ borderRadius: 16, overflow: 'hidden' }}>
        <Table columns={columns} dataSource={data?.content || []} rowKey="id" loading={loading}
          pagination={{ current: page + 1, total: data?.totalElements || 0, onChange: (p) => setPage(p - 1), showSizeChanger: false }} />
      </div>

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{editing ? 'Editar Paciente' : 'Nuevo Paciente'}</Text>}
        open={modalOpen} onCancel={closeModal} onOk={() => form.submit()} okText={editing ? 'Actualizar' : 'Crear'} width={640} destroyOnClose
        styles={{ body: { padding: '24px 28px' } }}>
        <Form form={form} layout="vertical" onFinish={handleSave} initialValues={editing || {}} preserve={false}
          style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="nombres" label="Nombres" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Nombres" />
            </Form.Item>
            <Form.Item name="apellidoPaterno" label="Apellido Paterno" rules={[{ required: true }]} style={{ width: '50%' }}>
              <Input placeholder="Apellido paterno" />
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="apellidoMaterno" label="Apellido Materno" style={{ width: '50%' }}>
              <Input placeholder="Apellido materno" />
            </Form.Item>
            <Form.Item name="dni" label="DNI" rules={[{ required: true, pattern: /^\d{8}$/, message: 'DNI debe tener exactamente 8 dígitos numéricos' }]} style={{ width: '50%' }}>
              <Input placeholder="12345678" maxLength={8} />
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="fechaNacimiento" label="Fecha de Nacimiento" style={{ width: '50%' }}>
              <DatePicker style={{ width: '100%' }} onChange={(d) => { if (d) form.setFieldValue('fechaNacimiento', d.toISOString()); }} />
            </Form.Item>
            <Form.Item name="genero" label="Género" style={{ width: '50%' }}>
              <Select placeholder="Seleccionar">
                <Select.Option value="MASCULINO">Masculino</Select.Option>
                <Select.Option value="FEMENINO">Femenino</Select.Option>
                <Select.Option value="OTRO">Otro</Select.Option>
              </Select>
            </Form.Item>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="email" label="Email" rules={[{ type: 'email' }]} style={{ width: '50%' }}>
              <Input placeholder="email@ejemplo.com" />
            </Form.Item>
            <Form.Item name="telefono" label="Teléfono" style={{ width: '50%' }}>
              <Input placeholder="+51 999 999 999" />
            </Form.Item>
          </div>
          <Form.Item name="direccion" label="Dirección">
            <Input.TextArea rows={2} placeholder="Dirección completa" />
          </Form.Item>
          <details style={{ marginBottom: 16, cursor: 'pointer', color: 'var(--text-secondary)' }}>
            <summary style={{ fontSize: 13, fontWeight: 600 }}>Información médica adicional</summary>
            <div style={{ marginTop: 12 }}>
              <Form.Item name="alergias" label="Alergias">
                <Input placeholder="Alergias conocidas" />
              </Form.Item>
              <div style={{ display: 'flex', gap: 16 }}>
                <Form.Item name="antecedentesFamiliares" label="Antecedentes Familiares" style={{ width: '50%' }}>
                  <Input.TextArea rows={2} placeholder="Antecedentes familiares" />
                </Form.Item>
                <Form.Item name="condiciones" label="Condiciones Preexistentes" style={{ width: '50%' }}>
                  <Input placeholder="Condiciones médicas" />
                </Form.Item>
              </div>
              <Form.Item name="medicamentosActual" label="Medicamentos Actuales">
                <Input placeholder="Medicamentos que toma actualmente" />
              </Form.Item>
            </div>
          </details>
          <details style={{ marginBottom: 16, cursor: 'pointer', color: 'var(--text-secondary)' }}>
            <summary style={{ fontSize: 13, fontWeight: 600 }}>Información del Seguro</summary>
            <div style={{ marginTop: 12 }}>
              <div style={{ display: 'flex', gap: 16 }}>
                <Form.Item name="nombreSeguro" label="Nombre del Seguro" style={{ width: '50%' }}>
                  <Input placeholder="Nombre del seguro" />
                </Form.Item>
                <Form.Item name="numeroPoliza" label="N° Póliza" style={{ width: '50%' }}>
                  <Input placeholder="Número de póliza" />
                </Form.Item>
              </div>
              <Form.Item name="vigenciaSeguro" label="Vigencia del Seguro">
                <DatePicker style={{ width: '100%' }} onChange={(d) => { if (d) form.setFieldValue('vigenciaSeguro', d.toISOString()); }} />
              </Form.Item>
            </div>
          </details>
        </Form>
      </Modal>
    </div>
  );
}