import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/register/Register';
import AppLayout from './components/layout/AppLayout';
import ProtectedRoute from './components/common/ProtectedRoute';
import LoadingAnimation from './components/common/LoadingAnimation';

const Dashboard = lazy(() => import('./pages/Dashboard'));
const Usuarios = lazy(() => import('./pages/usuarios/Usuarios'));
const Pacientes = lazy(() => import('./pages/pacientes/Pacientes'));
const Citas = lazy(() => import('./pages/citas/Citas'));
const Triaje = lazy(() => import('./pages/triaje/Triaje'));
const Consultas = lazy(() => import('./pages/consultas/Consultas'));
const Examenes = lazy(() => import('./pages/examenes/Examenes'));
const Recetas = lazy(() => import('./pages/recetas/Recetas'));
const Medicamentos = lazy(() => import('./pages/medicamentos/Medicamentos'));
const Dispensaciones = lazy(() => import('./pages/dispensaciones/Dispensaciones'));
const Cobros = lazy(() => import('./pages/cobros/Cobros'));
const Notificaciones = lazy(() => import('./pages/notificaciones/Notificaciones'));
const Auditoria = lazy(() => import('./pages/auditoria/Auditoria'));
const Perfil = lazy(() => import('./pages/perfil/Perfil'));
const Configuracion = lazy(() => import('./pages/configuracion/Configuracion'));
const Tarifario = lazy(() => import('./pages/tarifario/Tarifario'));
const Campanias = lazy(() => import('./pages/campanias/Campanias'));
const Verificacion = lazy(() => import('./pages/verificacion/Verificacion'));
const NotFound404 = lazy(() => import('./pages/NotFound404'));
const Offline = lazy(() => import('./pages/Offline'));

export default function App() {
  return (
    <Suspense fallback={<LoadingAnimation />}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/verificacion" element={<Verificacion />} />
        <Route path="/offline" element={<Offline />} />
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<Dashboard />} />
          <Route path="/usuarios" element={<ProtectedRoute roles={['ADMIN']}><Usuarios /></ProtectedRoute>} />
          <Route path="/pacientes" element={<Pacientes />} />
          <Route path="/citas" element={<Citas />} />
          <Route path="/triaje" element={<ProtectedRoute roles={['ADMIN','ENFERMERO']}><Triaje /></ProtectedRoute>} />
          <Route path="/consultas" element={<ProtectedRoute roles={['ADMIN','DOCTOR']}><Consultas /></ProtectedRoute>} />
          <Route path="/examenes" element={<ProtectedRoute roles={['ADMIN','DOCTOR','ATENCION_CLIENTE']}><Examenes /></ProtectedRoute>} />
          <Route path="/recetas" element={<ProtectedRoute roles={['ADMIN','DOCTOR']}><Recetas /></ProtectedRoute>} />
          <Route path="/medicamentos" element={<ProtectedRoute roles={['ADMIN','FARMACEUTICO']}><Medicamentos /></ProtectedRoute>} />
          <Route path="/dispensaciones" element={<ProtectedRoute roles={['ADMIN','FARMACEUTICO']}><Dispensaciones /></ProtectedRoute>} />
          <Route path="/cobros" element={<ProtectedRoute roles={['ADMIN','ATENCION_CLIENTE']}><Cobros /></ProtectedRoute>} />
          <Route path="/notificaciones" element={<ProtectedRoute roles={['ADMIN','ATENCION_CLIENTE','PACIENTE']}><Notificaciones /></ProtectedRoute>} />
          <Route path="/auditoria" element={<ProtectedRoute roles={['ADMIN']}><Auditoria /></ProtectedRoute>} />
          <Route path="/perfil" element={<Perfil />} />
          <Route path="/configuracion" element={<ProtectedRoute roles={['ADMIN']}><Configuracion /></ProtectedRoute>} />
          <Route path="/tarifario" element={<ProtectedRoute roles={['ADMIN','ATENCION_CLIENTE']}><Tarifario /></ProtectedRoute>} />
          <Route path="/campanias" element={<ProtectedRoute roles={['ADMIN','ATENCION_CLIENTE']}><Campanias /></ProtectedRoute>} />
        </Route>
        <Route path="*" element={<NotFound404 />} />
      </Routes>
    </Suspense>
  );
}

