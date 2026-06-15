import { Select, Space, Typography } from 'antd';
import { SearchOutlined, UserOutlined } from '@ant-design/icons';
import type { Paciente } from '../../types';

const { Text } = Typography;

interface Props {
  pacientes: Paciente[];
  loading?: boolean;
  onSelect: (paciente: Paciente) => void;
  placeholder?: string;
  value?: number | null;
  onChange?: (value: number | null) => void;
  autoFocus?: boolean;
}

export default function PacienteSearchByDni({ pacientes, loading, onSelect, placeholder, value, onChange, autoFocus }: Props) {
  return (
    <Select
      showSearch
      value={value ?? undefined}
      placeholder={placeholder ?? 'Ingresa DNI del paciente'}
      loading={loading}
      autoFocus={autoFocus}
      onChange={(val) => {
        const p = pacientes.find(x => x.id === val);
        if (p) onSelect(p);
        onChange?.(val ?? null);
      }}
      filterOption={(input, option) => {
        const label = option?.label?.toString() ?? '';
        return label.toLowerCase().includes(input.toLowerCase());
      }}
      style={{ width: '100%' }}
      notFoundContent={loading ? 'Cargando...' : 'No se encontraron pacientes'}
      options={pacientes.map(p => ({
        label: (
          <Space size={8}>
            <UserOutlined style={{ color: 'var(--text-muted)', fontSize: 13 }} />
            <Text style={{ fontWeight: 500 }}>{p.nombres} {p.apellidoPaterno}</Text>
            <Text style={{ color: 'var(--text-muted)', fontSize: 12 }}>DNI: {p.dni}</Text>
          </Space>
        ),
        value: p.id,
      }))}
    />
  );
}
