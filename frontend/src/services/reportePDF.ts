import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';
import type { HistorialEntry } from '../types';

function formatFecha(fecha: string): string {
  const d = new Date(fecha);
  return d.toLocaleDateString('es-PE', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false,
  });
}

function tipoLabel(tipo: string): string {
  const map: Record<string, string> = {
    CITA: 'Cita Médica',
    TRIAJE: 'Triaje',
    CONSULTA: 'Consulta',
    EXAMEN: 'Examen de Laboratorio',
    RECETA: 'Receta Médica',
  };
  return map[tipo] || tipo;
}

export async function generarPDF(
  entries: HistorialEntry[],
  pacienteNombre: string,
  pacienteDni: string,
) {
  const reportEl = document.createElement('div');
  reportEl.style.cssText = 'width: 800px; padding: 40px; background: white; font-family: Arial, sans-serif; position: absolute; left: -9999px; top: 0;';
  document.body.appendChild(reportEl);

  reportEl.innerHTML = `
    <div style="border-bottom: 3px solid #00D4AA; padding-bottom: 16px; margin-bottom: 24px;">
      <h1 style="font-size: 22px; color: #1a1a2e; margin: 0 0 4px 0;">Historial Clínico</h1>
      <p style="color: #666; margin: 0; font-size: 13px;">Sistema de Monitoreo de Pacientes</p>
    </div>
    <div style="margin-bottom: 24px; padding: 16px; background: #f8f9fa; border-radius: 6px; border: 1px solid #e5e7eb;">
      <p style="margin: 0 0 4px 0; font-size: 15px; color: #1a1a2e;"><strong>${pacienteNombre}</strong></p>
      <p style="margin: 0; color: #666; font-size: 13px;">DNI: ${pacienteDni}</p>
    </div>
    <table style="width: 100%; border-collapse: collapse; font-size: 12px;">
      <thead>
        <tr style="background: #00D4AA; color: white;">
          <th style="padding: 8px 10px; text-align: left; border: 1px solid #d1d5db;">Tipo</th>
          <th style="padding: 8px 10px; text-align: left; border: 1px solid #d1d5db;">Fecha</th>
          <th style="padding: 8px 10px; text-align: left; border: 1px solid #d1d5db;">Detalle</th>
          <th style="padding: 8px 10px; text-align: left; border: 1px solid #d1d5db;">Diagnóstico</th>
        </tr>
      </thead>
      <tbody>
        ${entries.map(e => `
          <tr style="border-bottom: 1px solid #e5e7eb;">
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; color: #374151;">
              <strong>${tipoLabel(e.tipo)}</strong>
            </td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; color: #374151;">
              ${formatFecha(e.fecha)}
            </td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; color: #374151;">
              <strong>${e.titulo}</strong><br/>
              <span style="color: #6b7280; font-size: 11px;">${e.descripcion}</span>
            </td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; color: #374151;">
              ${e.data && e.tipo === 'CONSULTA' && e.data.diagnostico ? String(e.data.diagnostico) :
                e.tipo === 'TRIAJE' && e.data.sintomas ? String(e.data.sintomas) : '-'}
            </td>
          </tr>
        `).join('')}
      </tbody>
    </table>
    <div style="margin-top: 24px; padding-top: 12px; border-top: 1px solid #e5e7eb; font-size: 10px; color: #9ca3af; text-align: center;">
      Generado el ${new Date().toLocaleDateString('es-PE')} - Sistema de Monitoreo de Pacientes
    </div>
  `;

  await document.fonts.ready;

  const canvas = await html2canvas(reportEl, {
    scale: 2,
    useCORS: true,
    backgroundColor: '#ffffff',
  });

  document.body.removeChild(reportEl);

  const imgData = canvas.toDataURL('image/jpeg', 0.95);
  const pdf = new jsPDF('p', 'mm', 'a4');
  const pdfW = 190;
  const pdfH = (canvas.height * pdfW) / canvas.width;
  let y = 0;

  while (y < pdfH) {
    if (y > 0) pdf.addPage();
    const h = Math.min(pdfH - y, 277);
    pdf.addImage(imgData, 'JPEG', 10, 10, pdfW, h, undefined, 'FAST', 0);
    y += 277;
  }

  pdf.save(`historial_${pacienteDni}.pdf`);
}
