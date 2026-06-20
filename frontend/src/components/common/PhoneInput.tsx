import { useMemo, useCallback } from 'react';
import { Select, Input } from 'antd';
import * as Flags from 'country-flag-icons/react/3x2';
import { countries, parseE164, getDefaultCountry } from '../../utils/countries';

interface PhoneInputProps {
  value?: string;
  onChange?: (value: string) => void;
  size?: 'small' | 'middle' | 'large';
  style?: React.CSSProperties;
}

const triggerStyle: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 6, padding: '0 4px',
};

const FlagRecord = Flags as unknown as Record<string, React.FC<React.SVGAttributes<SVGSVGElement>>>;

function FlagIcon({ code, ...props }: { code: string } & React.SVGAttributes<SVGSVGElement>) {
  const Cmp = FlagRecord[code];
  return Cmp ? <Cmp {...props} /> : null;
}

export default function PhoneInput({ value, onChange, size = 'middle', style }: PhoneInputProps) {
  const { country, localNumber } = useMemo(() => parseE164(value), [value]);

  const handleCountryChange = useCallback((code: string) => {
    const newCountry = countries.find(c => c.code === code) ?? getDefaultCountry();
    onChange?.(newCountry.dialCode);
  }, [onChange]);

  const handleNumberChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const digits = e.target.value.replace(/\D/g, '');
    onChange?.(country.dialCode + digits);
  }, [country.dialCode, onChange]);

  const isValid = useMemo(() => {
    if (localNumber.length === 0) return true;
    return country.pattern.test(localNumber);
  }, [country, localNumber]);

  return (
    <div style={{ display: 'flex', gap: 0, ...style }} className="phone-input-group">
      <Select
        value={country.code}
        onChange={handleCountryChange}
        size={size}
        showSearch
        filterOption={(input, option) =>
          (option?.label as string)?.toLowerCase().includes(input.toLowerCase()) ?? false
        }
        style={{ width: 130 }}
        labelRender={() => (
          <span style={triggerStyle}>
            <FlagIcon code={country.code} style={{ width: 22, height: 16, borderRadius: 2 }} />
            <span style={{ fontSize: 13, fontWeight: 500 }}>{country.dialCode}</span>
          </span>
        )}
        options={countries.map(c => ({
          value: c.code,
          label: `${c.name} ${c.dialCode}`,
          flag: c.code,
          dialCode: c.dialCode,
        }))}
        optionRender={(option) => (
          <span style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
            <FlagIcon code={option.data.flag} style={{ width: 24, height: 17, borderRadius: 2, flexShrink: 0 }} />
            <span style={{ flex: 1, fontSize: 14 }}>{option.data.label?.toString().replace(` ${option.data.dialCode}`, '')}</span>
            <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>{option.data.dialCode}</span>
          </span>
        )}
      />
      <Input
        value={localNumber}
        onChange={handleNumberChange}
        size={size}
        placeholder={country.placeholder}
        status={isValid && localNumber.length > 0 ? undefined : localNumber.length > 0 ? 'error' : undefined}
        style={{ flex: 1, borderTopLeftRadius: 0, borderBottomLeftRadius: 0 }}
      />
    </div>
  );
}
