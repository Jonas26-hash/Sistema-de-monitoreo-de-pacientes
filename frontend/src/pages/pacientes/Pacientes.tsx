import { useState } from 'react';
import { Table, Button, Modal, Form, Input, Select, DatePicker, Space, Input as SearchInput, Typography, Divider } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, TeamOutlined, EyeOutlined } from '@ant-design/icons';
import PhoneInput from '../../components/common/PhoneInput';
import { useCrud } from '../../hooks/useCrud';
import type { Paciente } from '../../types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function Pacientes() {
  const [form] = Form.useForm();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailPaciente, setDetailPaciente] = useState<Paciente | null>(null);

  const handleOpenDetail = (p: Paciente) => {
    setDetailPaciente(p);
    setDetailOpen(true);
  };

  const { search, setSearch, data, loading, page, setPage, modalOpen, editing, openCreate, openEdit, closeModal, handleSave, handleDelete } = useCrud<Paciente>({
    key: 'pacientes',
    endpoint: '/pacientes',
    dateFields: ['fechaNacimiento', 'vigenciaSeguro'],
  });

  const columns = [
    { title: 'Nº', key: 'index', width: 60, render: (_v: unknown, _r: unknown, i: number) => <Text style={{ color: 'var(--text-muted)' }}>{i + 1}</Text> },
    { title: 'Paciente', key: 'nombre', render: (_: unknown, r: Paciente) => <Text style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{r.nombres} {r.apellidoPaterno}{r.apellidoMaterno ? ' ' + r.apellidoMaterno : ''}</Text> },
    { title: 'DNI', dataIndex: 'dni', key: 'dni', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Email', dataIndex: 'email', key: 'email', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    { title: 'Teléfono', dataIndex: 'telefono', key: 'telefono', render: (v: string) => <Text style={{ color: 'var(--text-secondary)' }}>{v}</Text> },
    {
      title: 'Acciones', key: 'acciones', width: 140,
      render: (_: unknown, r: Paciente) => (
        <Space>
          <Button type="text" icon={<EyeOutlined />} style={{ color: '#00D4AA' }} onClick={() => handleOpenDetail(r)} />
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
              <Text style={{ color: 'var(--text-muted)' }}>Gestión de pacientes de la clínica</Text>
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

      <Modal title={<Text style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Detalle del Paciente</Text>}
        open={detailOpen} onCancel={() => setDetailOpen(false)} footer={[<Button onClick={() => setDetailOpen(false)}>Cerrar</Button>]}
        width={680} destroyOnClose styles={{ body: { padding: '24px 28px' } }}>
        {detailPaciente && <>
          <Title level={5} style={{ color: 'var(--text-primary)', marginBottom: 16 }}>📋 Datos Personales</Title>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px 24px', marginBottom: 24 }}>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Nombre Completo</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.nombres} {detailPaciente.apellidoPaterno}{detailPaciente.apellidoMaterno ? ' ' + detailPaciente.apellidoMaterno : ''}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>DNI</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.dni}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Email</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.email || '-'}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Teléfono</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.telefono || '-'}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Fecha de Nacimiento</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.fechaNacimiento ? dayjs(detailPaciente.fechaNacimiento).format('DD/MM/YYYY') : '-'}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Género</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.genero ? (detailPaciente.genero === 'MASCULINO' ? 'Masculino' : detailPaciente.genero === 'FEMENINO' ? 'Femenino' : detailPaciente.genero) : '-'}</Text></div>
            <div style={{ gridColumn: '1 / -1' }}><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Dirección</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.direccion || '-'}</Text></div>
          </div>

          <Divider style={{ margin: '16px 0' }} />
          <Title level={5} style={{ color: 'var(--text-primary)', marginBottom: 16 }}>🩺 Información Médica</Title>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px 24px', marginBottom: 24 }}>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Alergias</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.alergias || 'Ninguna registrada'}</Text></div>
            <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Condiciones Preexistentes</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.condiciones || 'Ninguna registrada'}</Text></div>
            <div style={{ gridColumn: '1 / -1' }}><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Antecedentes Familiares</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.antecedentesFamiliares || 'Ninguno registrado'}</Text></div>
            <div style={{ gridColumn: '1 / -1' }}><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Medicamentos Actuales</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.medicamentosActual || 'Ninguno registrado'}</Text></div>
          </div>

          <Divider style={{ margin: '16px 0' }} />
          <Title level={5} style={{ color: 'var(--text-primary)', marginBottom: 16 }}>🔒 Información del Seguro</Title>
          {detailPaciente.nombreSeguro ? (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px 24px' }}>
              <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Nombre del Seguro</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.nombreSeguro}</Text></div>
              <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>N° Póliza</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.numeroPoliza || '-'}</Text></div>
              <div><Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>Vigencia</Text><br /><Text style={{ color: 'var(--text-primary)' }}>{detailPaciente.vigenciaSeguro ? dayjs(detailPaciente.vigenciaSeguro).format('DD/MM/YYYY') : '-'}</Text></div>
            </div>
          ) : (
            <Text style={{ color: 'var(--text-muted)' }}>Sin seguro registrado</Text>
          )}
        </>}
      </Modal>

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
              <DatePicker style={{ width: '100%' }} />
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
              <PhoneInput />
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
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </div>
          </details>
        </Form>
      </Modal>
    </div>
  );
}