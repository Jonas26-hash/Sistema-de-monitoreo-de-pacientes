export interface CountryData {
  code: string;
  name: string;
  dialCode: string;
  pattern: RegExp;
  placeholder: string;
}

const rawCountries: CountryData[] = [
  { code: 'PE', name: 'Perú', dialCode: '+51', pattern: /^9\d{8}$/, placeholder: '999 888 777' },
  { code: 'AR', name: 'Argentina', dialCode: '+54', pattern: /^\d{10}$/, placeholder: '11 1234 5678' },
  { code: 'AU', name: 'Australia', dialCode: '+61', pattern: /^\d{9}$/, placeholder: '412 345 678' },
  { code: 'BO', name: 'Bolivia', dialCode: '+591', pattern: /^\d{8}$/, placeholder: '71234567' },
  { code: 'BR', name: 'Brasil', dialCode: '+55', pattern: /^\d{10,11}$/, placeholder: '11 91234 5678' },
  { code: 'CL', name: 'Chile', dialCode: '+56', pattern: /^\d{9}$/, placeholder: '9 1234 5678' },
  { code: 'CN', name: 'China', dialCode: '+86', pattern: /^\d{11}$/, placeholder: '138 1234 5678' },
  { code: 'CO', name: 'Colombia', dialCode: '+57', pattern: /^\d{10}$/, placeholder: '300 123 4567' },
  { code: 'KR', name: 'Corea del Sur', dialCode: '+82', pattern: /^\d{10,11}$/, placeholder: '10 1234 5678' },
  { code: 'CR', name: 'Costa Rica', dialCode: '+506', pattern: /^\d{8}$/, placeholder: '8312 3456' },
  { code: 'CU', name: 'Cuba', dialCode: '+53', pattern: /^\d{8}$/, placeholder: '5 123 4567' },
  { code: 'EC', name: 'Ecuador', dialCode: '+593', pattern: /^\d{9}$/, placeholder: '99 123 4567' },
  { code: 'SV', name: 'El Salvador', dialCode: '+503', pattern: /^\d{8}$/, placeholder: '7123 4567' },
  { code: 'ES', name: 'España', dialCode: '+34', pattern: /^\d{9}$/, placeholder: '612 345 678' },
  { code: 'US', name: 'EE. UU.', dialCode: '+1', pattern: /^\d{10}$/, placeholder: '(212) 555 1234' },
  { code: 'CA', name: 'Canadá', dialCode: '+1', pattern: /^\d{10}$/, placeholder: '416 555 1234' },
  { code: 'FR', name: 'Francia', dialCode: '+33', pattern: /^\d{9}$/, placeholder: '6 12 34 56 78' },
  { code: 'GT', name: 'Guatemala', dialCode: '+502', pattern: /^\d{8}$/, placeholder: '4123 4567' },
  { code: 'HN', name: 'Honduras', dialCode: '+504', pattern: /^\d{8}$/, placeholder: '9123 4567' },
  { code: 'IN', name: 'India', dialCode: '+91', pattern: /^\d{10}$/, placeholder: '98765 43210' },
  { code: 'IT', name: 'Italia', dialCode: '+39', pattern: /^\d{9,10}$/, placeholder: '312 345 6789' },
  { code: 'JP', name: 'Japón', dialCode: '+81', pattern: /^\d{10,11}$/, placeholder: '90 1234 5678' },
  { code: 'MX', name: 'México', dialCode: '+52', pattern: /^\d{10}$/, placeholder: '55 1234 5678' },
  { code: 'NI', name: 'Nicaragua', dialCode: '+505', pattern: /^\d{8}$/, placeholder: '8123 4567' },
  { code: 'NO', name: 'Noruega', dialCode: '+47', pattern: /^\d{8}$/, placeholder: '412 34 567' },
  { code: 'NZ', name: 'Nueva Zelanda', dialCode: '+64', pattern: /^\d{8,10}$/, placeholder: '21 123 4567' },
  { code: 'NL', name: 'Países Bajos', dialCode: '+31', pattern: /^\d{9}$/, placeholder: '6 1234 5678' },
  { code: 'PA', name: 'Panamá', dialCode: '+507', pattern: /^\d{8}$/, placeholder: '6123 4567' },
  { code: 'PY', name: 'Paraguay', dialCode: '+595', pattern: /^\d{9}$/, placeholder: '981 123 456' },
  { code: 'PT', name: 'Portugal', dialCode: '+351', pattern: /^\d{9}$/, placeholder: '912 345 678' },
  { code: 'GB', name: 'Reino Unido', dialCode: '+44', pattern: /^\d{10}$/, placeholder: '7911 123456' },
  { code: 'DO', name: 'Rep. Dominicana', dialCode: '+1', pattern: /^\d{10}$/, placeholder: '809 555 1234' },
  { code: 'RU', name: 'Rusia', dialCode: '+7', pattern: /^\d{10}$/, placeholder: '912 345 67 89' },
  { code: 'UY', name: 'Uruguay', dialCode: '+598', pattern: /^\d{8}$/, placeholder: '99 123 456' },
  { code: 'VE', name: 'Venezuela', dialCode: '+58', pattern: /^\d{10}$/, placeholder: '412 123 4567' },
];

const dialCodePriority = (() => {
  const map = new Map<string, number>();
  rawCountries.forEach((c, i) => {
    if (!map.has(c.dialCode)) map.set(c.dialCode, i);
  });
  return map;
})();

export const countries: CountryData[] = rawCountries;

export function findCountryByDialCode(dialCode: string): CountryData | undefined {
  const idx = dialCodePriority.get(dialCode);
  return idx !== undefined ? rawCountries[idx] : undefined;
}

export function getDefaultCountry(): CountryData {
  return countries[0];
}

export function parseE164(value?: string): { country: CountryData; localNumber: string } {
  if (!value) return { country: getDefaultCountry(), localNumber: '' };
  const matched = countries.find(c => value.startsWith(c.dialCode));
  if (matched) return { country: matched, localNumber: value.slice(matched.dialCode.length) };
  return { country: getDefaultCountry(), localNumber: value.replace(/\D/g, '') };
}
