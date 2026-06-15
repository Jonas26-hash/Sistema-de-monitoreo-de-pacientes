import { Modal } from 'antd';

type ActionType = 'creado' | 'creada' | 'actualizado' | 'actualizada' | 'eliminado' | 'eliminada' | 'completada' | 'procesado';

export function showCrudSuccess(tipo: ActionType, recurso: string = 'Registro') {
  const svgMap = {
    creado: '/lottie/Registro.svg',
    creada: '/lottie/Registro.svg',
    actualizado: '/lottie/Untitled file.svg',
    actualizada: '/lottie/Untitled file.svg',
    eliminado: '/lottie/Untitled file.svg',
    eliminada: '/lottie/Untitled file.svg',
    completada: '/lottie/Untitled file.svg',
    procesado: '/lottie/Registro.svg',
  };
  const titleMap = {
    creado: `${recurso} creado exitosamente`,
    creada: `${recurso} creada exitosamente`,
    actualizado: `${recurso} actualizado exitosamente`,
    actualizada: `${recurso} actualizada exitosamente`,
    eliminado: `${recurso} eliminado exitosamente`,
    eliminada: `${recurso} eliminada exitosamente`,
    completada: `${recurso} completada exitosamente`,
    procesado: `${recurso} procesado exitosamente`,
  };

  const modal = Modal.info({
    icon: null,
    title: null,
    content: (
      <div style={{ textAlign: 'center', padding: '12px 0' }}>
        <object data={svgMap[tipo]} type="image/svg+xml" style={{ width: 220, height: 220, margin: '0 auto', display: 'block' }}>
          <img src={svgMap[tipo]} alt="" style={{ width: 220, height: 220 }} />
        </object>
        <div style={{ marginTop: 16, fontSize: 16, fontWeight: 600, color: 'var(--text-primary)' }}>
          {titleMap[tipo]}
        </div>
      </div>
    ),
    footer: null,
    centered: true,
    maskClosable: true,
    width: 420,
    closable: false,
    maskStyle: { background: 'rgba(0,0,0,0.45)', backdropFilter: 'blur(4px)' },
  });

  setTimeout(() => modal.destroy(), 2500);
}
