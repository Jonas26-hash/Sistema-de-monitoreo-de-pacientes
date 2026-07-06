export interface User {
  id: number;
  username: string;
  email: string;
  nombres: string;
  apellidos: string;
  roles: string[];
  activo?: boolean;
  dni?: string;
  telefono?: string;
  especialidad?: string;
  pacienteId?: number;
  password?: string;
  rol?: string;
}

export interface Paciente {
  id: number;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno?: string;
  dni: string;
  email?: string;
  telefono?: string;
  fechaNacimiento?: string;
  genero?: string;
  direccion?: string;
  alergias?: string;
  antecedentesFamiliares?: string;
  condiciones?: string;
  medicamentosActual?: string;
  nombreSeguro?: string;
  numeroPoliza?: string;
  vigenciaSeguro?: string;
  activo?: boolean;
  solicitaCuenta?: boolean;
}

export interface Cita {
  id: number;
  pacienteId: number;
  doctorId: number;
  fechaHora: string;
  estado: 'PROGRAMADA' | 'CONFIRMADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA';
  motivo: string;
  observaciones?: string;
  precio?: number;
}

export interface Consulta {
  id: number;
  citaId: number;
  pacienteId: number;
  doctorId: number;
  fechaConsulta: string;
  sintomas?: string;
  diagnostico: string;
  tratamiento: string;
  observaciones?: string;
}

export interface Receta {
  id: number;
  consultaId: number;
  pacienteId: number;
  doctorId: number;
  fechaEmision: string;
  fechaVigencia?: string;
  medicamentos: string;
  indicaciones?: string;
  dispensada?: boolean;
  fechaDispensacion?: string;
  pagado?: boolean;
  costo?: number;
}

export interface Medicamento {
  id: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  presentacion?: string;
  stock: number;
  stockMinimo?: number;
  contraindicaciones?: string;
  precio?: number;
}

export interface Dispensacion {
  id: number;
  recetaId: number;
  medicamentoId: number;
  cantidad: number;
  fechaDispensacion: string;
  farmaceuticoId: number;
  observaciones?: string;
}

export interface Triaje {
  id: number;
  pacienteId: number;
  citaId?: number;
  enfermeroId?: number;
  fechaTriaje: string;
  peso?: number;
  talla?: number;
  presionSistolica?: number;
  presionDiastolica?: number;
  temperatura?: number;
  frecuenciaCardiaca?: number;
  spo2?: number;
  frecuenciaRespiratoria?: number;
  motivoConsulta?: string;
  observaciones?: string;
}

export interface OrdenExamen {
  id: number;
  pacienteId: number;
  citaId?: number;
  doctorId?: number;
  tipo: string;
  descripcion?: string;
  resultado?: string;
  costo?: number;
  estado: string;
  fechaOrden: string;
  fechaResultado?: string;
  pagado?: boolean;
}

export interface Cobro {
  id: number;
  pacienteId: number;
  tipo: string;
  referenciaId?: number;
  monto: number;
  estado: 'PENDIENTE' | 'PAGADO' | 'ANULADO' | 'PENDIENTE_VERIFICACION' | 'VERIFICADO' | 'RECHAZADO';
  fechaCobro?: string;
  descripcion?: string;
  tipoComprobante?: string;
  numDocumento?: string;
  codigoVerificacion?: string;
}

export interface Servicio {
  id: number;
  codigo: string;
  nombre: string;
  tipo: string;
  precio: number;
  activo?: boolean;
}

export interface Campania {
  id: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  descuentoPorcentaje: number;
  fechaInicio: string;
  fechaFin: string;
  activo?: boolean;
}

export interface Notificacion {
  id: number;
  pacienteId: number;
  tipo: string;
  mensaje: string;
  fechaEnvio?: string;
  enviada: boolean;
  canal?: string;
  leida: boolean;
  remitenteId?: number;
  remitenteTipo?: string;
  remitenteNombre?: string;
  destinatario?: string;
}

export interface HistorialEntry {
  tipo: 'CITA' | 'TRIAJE' | 'CONSULTA' | 'EXAMEN' | 'RECETA';
  fecha: string;
  titulo: string;
  descripcion: string;
  data: Record<string, unknown>;
}

export interface AuditLog {
  id: number;
  username: string;
  accion: string;
  recurso: string;
  statusCode?: number;
  tiempoMs?: number;
  ip?: string;
  userAgent?: string;
  requestId?: string;
  createdAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  email: string;
  roles: string[];
  avatar?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ApiError {
  error?: string;
  mensaje?: string;
  message?: string;
  code?: number;
}