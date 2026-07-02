import { Select, Space, Typography } from 'antd';
import { UserOutlined } from '@ant-design/icons';
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
        return pacientes.some(p =>
          p.id === option?.value &&
          (`${p.nombres} ${p.apellidoPaterno} ${p.dni}`.toLowerCase().includes(input.toLowerCase()))
        );
      }}
      style={{ width: '100%' }}
      notFoundContent={loading ? 'Cargando...' : 'No se encontraron pacientes'}
      options={pacientes.map(p => ({
        label: `${p.nombres} ${p.apellidoPaterno} - DNI: ${p.dni}`,
        value: p.id,
      }))}
      optionRender={(option) => (
        <Space size={8}>
          <UserOutlined style={{ color: 'var(--text-muted)', fontSize: 13 }} />
          <Text style={{ fontWeight: 500 }}>{option.data.label}</Text>
        </Space>
      )}
    />
  );
}
