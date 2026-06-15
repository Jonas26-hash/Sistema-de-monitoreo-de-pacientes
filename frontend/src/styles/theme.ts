import { theme, type ThemeConfig } from 'antd';

export type ThemeMode = 'light' | 'dark';

const palette = {
  primary: '#0EA5A4',
  primaryDark: '#14D6B0',
  info: '#2563EB',
  success: '#10B981',
  warning: '#F59E0B',
  error: '#EF4444',
};

export const getThemeConfig = (mode: ThemeMode): ThemeConfig => {
  const dark = mode === 'dark';

  return {
    algorithm: dark ? theme.darkAlgorithm : theme.defaultAlgorithm,
    token: {
      colorPrimary: dark ? palette.primaryDark : palette.primary,
      colorInfo: palette.info,
      colorSuccess: palette.success,
      colorWarning: palette.warning,
      colorError: palette.error,
      colorBgBase: dark ? '#0B1120' : '#F5F7FB',
      colorBgLayout: dark ? '#080D18' : '#EEF3F8',
      colorBgContainer: dark ? '#111827' : '#FFFFFF',
      colorBgElevated: dark ? '#172033' : '#FFFFFF',
      colorTextBase: dark ? '#F8FAFC' : '#102033',
      colorBorder: dark ? '#243044' : '#DDE6EF',
      borderRadius: 8,
      fontFamily: "'Plus Jakarta Sans', Inter, system-ui, -apple-system, sans-serif",
    },
    components: {
      Button: {
        borderRadius: 8,
        controlHeight: 40,
      },
      Card: {
        borderRadiusLG: 8,
        paddingLG: 22,
      },
      Table: {
        headerBg: dark ? '#151F31' : '#F3F7FB',
        headerColor: dark ? '#E5EEF9' : '#475569',
        rowHoverBg: dark ? '#182236' : '#F6FAFD',
        borderColor: dark ? '#243044' : '#E2E8F0',
      },
      Menu: {
        itemBg: 'transparent',
        itemSelectedBg: dark ? 'rgba(20,214,176,0.14)' : 'rgba(14,165,164,0.10)',
        itemSelectedColor: dark ? palette.primaryDark : palette.primary,
        itemHoverBg: dark ? 'rgba(255,255,255,0.06)' : 'rgba(15,23,42,0.05)',
        itemColor: dark ? '#A7B4C7' : '#526173',
        itemBorderRadius: 8,
      },
      Input: {
        borderRadius: 8,
        activeBorderColor: dark ? palette.primaryDark : palette.primary,
        hoverBorderColor: dark ? palette.primaryDark : palette.primary,
      },
      Select: {
        optionSelectedBg: dark ? 'rgba(20,214,176,0.16)' : 'rgba(14,165,164,0.12)',
      },
      DatePicker: {
        borderRadius: 8,
      },
      Tag: {
        borderRadius: 6,
      },
      Tabs: {
        inkBarColor: dark ? palette.primaryDark : palette.primary,
        itemSelectedColor: dark ? palette.primaryDark : palette.primary,
        itemHoverColor: dark ? palette.primaryDark : palette.primary,
      },
      Modal: {
        borderRadiusLG: 8,
      },
      Notification: {
        borderRadiusLG: 8,
      },
      Alert: {
        borderRadiusLG: 8,
      },
    },
  };
};

export const LAYOUT = {
  sidebarWidth: 260,
  headerHeight: 64,
  collapsedSidebarWidth: 80,
};
