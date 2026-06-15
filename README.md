# Sistema de Monitoreo de Pacientes - Plataforma Clínica Integral

Plataforma completa de gestión hospitalaria con **frontend React** + **backend microservicios** (Quarkus).  
Login funcional, JWT asimétrico, disponibilidad de doctores en tiempo real, y formularios basados en DNI.

## Arquitectura General

```
                   ┌──────────────────────┐
                   │  Frontend (React)     │
                   │  nginx - Puerto 3000  │
                   └──────────┬───────────┘
                              │
                              ▼
                   ┌──────────────────────┐
                   │   API Gateway        │
                   │    Puerto 8080       │
                   └──────┬───────────────┘
                          │
        ┌─────┬──────┬────┼────┬─────┬──────┬──────┐
        ▼     ▼      ▼    ▼    ▼     ▼      ▼      ▼
      Config Registry Pacientes Atencion Recetas Farmacia Cobros Notificaciones
      8888   8761     8081      8082     8083    8084     8085    8086
```

## Microservicios

| Servicio | Puerto | Descripcion |
|----------|--------|-------------|
| **ms-config-server** | 8888 | Servidor de configuracion centralizada |
| **ms-registry-server** | 8761 | Servidor de registro de servicios (Eureka) |
| **ms-pacientes** | 8081 | Datos personales, historial, seguros |
| **ms-atencion** | 8082 | Citas meddicas, consultas |
| **ms-recetas** | 8083 | Emision y vigencia de recetas |
| **ms-farmacia** | 8084 | Stock, dispensacion de medicamentos |
| **ms-cobros** | 8085 | Facturacion |
| **ms-notificaciones** | 8086 | Recordatorios, alertas |
| **api-gateway** | 8080 | Punto de entrada unico |

## Tecnologías

### Frontend
- **React 18** + TypeScript + Vite
- **Ant Design 5** - UI components
- **React Router v6** - Client-side routing
- **TanStack React Query** - Data fetching & caching
- **Recharts** - Visualización de datos
- **Axios** - HTTP client
- **Lottie Animations** - Animaciones vía CDN (`@lottiefiles/dotlottie-react`)
- **Tema claro/oscuro** - ThemeContext con CSS variables
- **Lazy loading** - `React.lazy()` en todas las páginas excepto Login
- **nginx** - Reverse proxy + DNS dinámico (evita 502 Bad Gateway)

### Backend
- **Quarkus** - Framework Java cloud-native (microservicios)
- **Spring Boot** - Config Server y Eureka Registry (infraestructura)
- **PostgreSQL** - Base de datos
- **Spring Cloud Config Server** - Configuración centralizada
- **Netflix Eureka** - Service discovery
- **REST Client** - Comunicación entre microservicios
- **Quarkus Mailer** - SMTP para notificaciones por correo
- **JWT RSA-2048** - Autenticación asimétrica
- **Docker & Docker Compose** - Contenedores

### Almacenamiento
- **JWT keys shared volume** - Claves en `jwt-keys/` (Docker volume)
- **PostgreSQL volumes** - Persistencia de datos


## Estructura del Proyecto

```
Sistema-de-monitoreo-de-pacientes/
├── backend/
│   ├── api-gateway/              # Punto de entrada único
│   ├── ms-pacientes/             # Gestión de pacientes, usuarios, autenticación
│   ├── ms-atencion/              # Citas, consultas, triaje, órdenes examen
│   ├── ms-recetas/               # Emisión y vigencia de recetas
│   ├── ms-farmacia/              # Inventario, dispensaciones de medicamentos
│   ├── ms-cobros/                # Facturación, tarifario, campañas
│   ├── ms-notificaciones/        # Email, recordatorios, alertas
│   ├── ms-config-server/         # Configuración centralizada
│   ├── ms-registry-server/       # Service discovery (Eureka)
│   └── central-config/           # Archivos de configuración (.properties)
├── frontend/
│   ├── src/
│   │   ├── pages/                # 17 páginas (Login, Dashboard, Citas, Triaje, Consultas, etc.)
│   │   ├── components/           # Componentes reutilizables (Header, Sidebar, etc.)
│   │   ├── context/              # AuthContext, ThemeContext
│   │   ├── services/             # API calls (axios + React Query)
│   │   ├── styles/               # CSS global + tema
│   │   └── utils/                # Helpers, notificaciones
│   ├── Dockerfile                # Build Node.js → production
│   ├── nginx.conf                # Reverse proxy + DNS dinámico
│   └── vite.config.ts            # Chunk splitting (antd-vendor, chart-vendor, lottie-vendor)
├── docker-compose.yml            # 16 contenedores (frontend + backend + DBs)
├── postman-collection.json       # Colección completa de APIs
└── README.md                      # Este archivo
```

## Ejecucion con Docker

Esta es la forma recomendada para levantar el proyecto completo. No es necesario ejecutar
`mvnw.cmd` desde el IDE si se usa Docker Compose.

### Requisitos

- Docker Desktop instalado y en ejecucion.
- Git instalado.
- Puertos libres: `3000` (frontend nginx), `8080` (API Gateway), `8081`-`8086` (microservicios), `8761`, `8888`, `5432`-`5437` (DBs).
- **Importante**: El frontend se compila localmente con `npm run build` antes de levantar Docker (se monta como bind volume en nginx).

### Clonar, construir y levantar por primera vez

**Paso 1:** Clonar el repositorio
```powershell
git clone https://github.com/Jonas26-hash/Sistema-de-monitoreo-de-pacientes.git
cd Sistema-de-monitoreo-de-pacientes
```

**Paso 2:** Compilar el frontend
```powershell
cd frontend
npm install
npm run build
cd ..
```

**Paso 3:** Levantar todos los contenedores
```powershell
docker compose up -d --build
```

El primer build puede tardar **3-5 minutos** (descarga dependencias Java, Node, imágenes Docker).

**Paso 4:** Verificar que todos los servicios estén UP
```powershell
docker compose ps
```

Deberías ver 16 contenedores en estado `Up`:
- 1 frontend (nginx)
- 8 microservicios (api-gateway, ms-pacientes, ms-atencion, ms-recetas, ms-farmacia, ms-cobros, ms-notificaciones, ms-config-server, ms-registry-server)
- 6 bases de datos PostgreSQL
- 1 volumen compartido (jwt-keys)

### Acceso a la plataforma

**Frontend (Interfaz de usuario):**
```
http://localhost:3000
```

**Backend (API Gateway):**
```
http://localhost:8080
```

**Prueba rápida del backend:**
```http
GET http://localhost:8080
```

**Credenciales por defecto (seed data):**
```
username: admin
password: admin123
```

Verifica que puedas hacer login y el JWT se guarde en localStorage.

### Seed Data (Usuarios de Prueba)

El sistema carga automáticamente 5 usuarios al iniciar si la BD está vacía:

| Username | Password | Rol | Email |
|----------|----------|-----|-------|
| `admin` | `admin123` | ADMIN | admin@clinica.com |
| `medico` | `medico123` | DOCTOR | medico@clinica.com |
| `farmaceutico` | `farm123` | FARMACEUTICO | farm@clinica.com |
| `atencion` | `aten123` | ATENCION_CLIENTE | atencion@clinica.com |
| `paciente1` | `paciente123` | PACIENTE | paciente@clinica.com |

Puedes hacer login con cualquiera de estas credenciales en http://localhost:3000

### Registrar nuevos usuarios (si necesario)

Para registrar un usuario nuevo:

```http
POST http://localhost:8080/auth/register
Content-Type: application/json
```

Body:
```json
{
  "username": "nuevousuario",
  "password": "MiPassword123!",
  "email": "nuevo@clinica.com",
  "nombres": "Juan",
  "apellidos": "Pérez",
  "dni": "12345678",
  "telefono": "987654321"
}
```

Luego hacer login:

```http
POST http://localhost:8080/auth/login
Content-Type: application/json
```

Body:
```json
{
  "username": "nuevousuario",
  "password": "MiPassword123!"
}
```

Respuesta:
```json
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "usuario": {
    "id": 6,
    "username": "nuevousuario",
    "email": "nuevo@clinica.com",
    "roles": ["PACIENTE"]
  }
}
```

Para endpoints protegidos, usa el header:
```
Authorization: Bearer <TOKEN>
```


### Uso diario

Si ya se construyeron las imagenes y no hay cambios nuevos:

```powershell
docker compose up -d
docker compose ps
```

Si se hizo `git pull` o hubo cambios de codigo:

```powershell
git pull
docker compose up -d --build
docker compose ps
```

### Ver logs y diagnosticar errores

Ver todos los logs:

```powershell
docker compose logs -f
```

Ver logs de un servicio especifico:

```powershell
docker compose logs --tail=200 api-gateway
docker compose logs --tail=200 ms-pacientes
```

Ver todos los contenedores, incluso los que fallaron:

```powershell
docker compose ps -a
```

Si un microservicio aparece como `Exited`, revisar sus logs:

```powershell
docker compose logs --tail=200 ms-pacientes
```


## Desarrollo Local - Frontend

### Levantar solo el backend con Docker

Si solo quieres desarrollar el frontend localmente con hot reload:

```powershell
# Levantar solo las bases de datos
docker compose up -d db-pacientes db-atencion db-recetas db-farmacia db-cobros db-notificaciones

# También levantar el Config Server y Registry
docker compose up -d ms-config-server ms-registry-server
```

Espera ~30 segundos a que los servidores de infraestructura estén listos, luego en otra terminal levanta los microservicios.

### Levantar microservicios manualmente

En **3 terminales separadas**:

**Terminal 1:** Config Server + Registry
```powershell
cd backend/ms-config-server
.\mvnw.cmd spring-boot:run

# En otra terminal:
cd backend/ms-registry-server
.\mvnw.cmd spring-boot:run
```

**Terminal 2:** Microservicios (elige los que necesites)
```powershell
cd backend/ms-pacientes
.\mvnw.cmd quarkus:dev

# En otras terminales:
cd backend/ms-atencion && .\mvnw.cmd quarkus:dev
cd backend/ms-farmacia && .\mvnw.cmd quarkus:dev
cd backend/ms-cobros && .\mvnw.cmd quarkus:dev
```

**Terminal 3:** Gateway
```powershell
cd backend/api-gateway
.\mvnw.cmd quarkus:dev
```

### Desarrollo del Frontend (Hot Reload)

En una **nueva terminal**:

```powershell
cd frontend
npm install
npm run dev
```

Abre http://localhost:5173 en tu navegador. Los cambios en `frontend/src/` se reflejan automáticamente.

**Nota:** El frontend en desarrollo apunta a `http://localhost:8080` (API Gateway). Asegúrate de que el gateway está corriendo.


## Páginas del Frontend (17 total)

| Página | Ruta | Descripción | Roles |
|--------|------|-------------|-------|
| **Login** | `/login` | Autenticación con username/password | Public |
| **Registro** | `/register` | Pre-registro de nuevos usuarios | Public |
| **Verificación** | `/verificacion` | Validar código por correo | Public |
| **Dashboard** | `/dashboard` | Inicio con stats y actividad reciente | Autenticados |
| **Mi Perfil** | `/perfil` | Editar perfil, cambiar contraseña | Autenticados |
| **Pacientes** | `/pacientes` | CRUD de pacientes (admin/doctor) | DOCTOR, ADMIN |
| **Citas** | `/citas` | Agendar citas con búsqueda por DNI + disponibilidad de doctores | ATENCION_CLIENTE, PACIENTE |
| **Triaje** | `/triaje` | Signos vitales, CRUD triaje | ENFERMERO |
| **Consultas** | `/consultas` | Consultas médicas, diagnóstico | DOCTOR |
| **Exámenes** | `/examenes` | Órdenes de examen con resultados | DOCTOR |
| **Recetas** | `/recetas` | Emisión de recetas médicas | DOCTOR |
| **Dispensaciones** | `/dispensaciones` | Entrega de medicamentos con email | FARMACEUTICO |
| **Medicamentos** | `/medicamentos` | CRUD de inventario farmacéutico | FARMACEUTICO |
| **Cobros** | `/cobros` | Pago único con QR Yape | CAJERO |
| **Tarifario** | `/tarifario` | Catálogo de servicios y precios | ADMIN |
| **Campañas** | `/campanias` | Campañas con descuentos % | ADMIN |
| **Auditoría** | `/auditoria` | Log de todas las acciones del sistema | ADMIN |
| **Usuarios** | `/usuarios` | Gestión de usuarios y roles | ADMIN |
| **Notificaciones** | `/notificaciones` | Recordatorios y alertas | Autenticados |
| **Configuración** | `/configuracion` | Tema claro/oscuro, idioma | Autenticados |

## Endpoints Principales del Backend

### 🔐 Autenticación (`/auth`)
- `POST /auth/register` - Registrar usuario (pre-registro)
- `POST /auth/register-init` - Bootstrap: crear primer ADMIN
- `POST /auth/login` - Iniciar sesión (devuelve JWT)
- `PUT /auth/change-password` - Cambiar contraseña (requiere JWT)
- `PUT /auth/profile` - Actualizar perfil (requiere JWT)
- `POST /auth/pre-registro` - Pre-registro por email
- `GET /auth/usuarios/rol/{rol}` - Listar usuarios por rol (ej: DOCTOR)

### 👥 Pacientes (`/pacientes`)
- `GET /pacientes` - Listar todos (paginado)
- `POST /pacientes` - Crear paciente
- `GET /pacientes/{id}` - Obtener por ID
- `PUT /pacientes/{id}` - Actualizar paciente
- `DELETE /pacientes/{id}` - Eliminar paciente
- `GET /pacientes/dni/{dni}` - Buscar por DNI (8 dígitos)
- `GET /pacientes/{id}/citas` - Citas de un paciente

### 📅 Citas (`/citas`)
- `GET /citas` - Listar todas (paginado)
- `POST /citas` - Crear cita
- `GET /citas/{id}` - Obtener cita por ID
- `PUT /citas/{id}` - Actualizar cita
- `DELETE /citas/{id}` - Cancelar cita
- `POST /citas/por-dni` - Crear cita por DNI (devuelve disponibilidad)
- `GET /citas/doctores-ocupados?fechaHora=2024-06-15T10:00` - Doctores no disponibles (±30 min)

### 🏥 Consultas (`/consultas`)
- `GET /consultas` - Listar todas
- `POST /consultas` - Crear consulta
- `GET /consultas/{id}` - Obtener consulta
- `PUT /consultas/{id}` - Actualizar diagnóstico
- `DELETE /consultas/{id}` - Eliminar consulta

### 📋 Triaje (`/triaje`)
- `GET /triaje` - Listar todos
- `POST /triaje` - Crear triaje (signos vitales)
- `GET /triaje/{id}` - Obtener triaje
- `PUT /triaje/{id}` - Actualizar signos vitales
- `DELETE /triaje/{id}` - Eliminar triaje

### 🔬 Exámenes (`/ordenes-examen`)
- `GET /ordenes-examen` - Listar órdenes
- `POST /ordenes-examen` - Crear orden de examen
- `GET /ordenes-examen/{id}` - Obtener orden
- `PUT /ordenes-examen/{id}` - Actualizar orden (agregar resultado)
- `DELETE /ordenes-examen/{id}` - Cancelar orden

### 💊 Recetas (`/recetas`)
- `GET /recetas` - Listar recetas
- `POST /recetas` - Emitir receta médica
- `GET /recetas/{id}` - Obtener receta
- `PUT /recetas/{id}` - Actualizar receta
- `DELETE /recetas/{id}` - Cancelar receta
- `GET /recetas/dni/{dni}` - Recetas de un paciente

### 🏪 Farmacia

**Medicamentos:**
- `GET /medicamentos` - Listar inventario
- `POST /medicamentos` - Crear medicamento
- `GET /medicamentos/{id}` - Obtener medicamento
- `PUT /medicamentos/{id}` - Actualizar stock/precio
- `DELETE /medicamentos/{id}` - Eliminar medicamento

**Dispensaciones:**
- `GET /dispensaciones` - Listar entregas
- `POST /dispensaciones` - Dispensar medicamento (con email)
- `GET /dispensaciones/{id}` - Obtener dispensación
- `PUT /dispensaciones/{id}` - Actualizar dispensación
- `DELETE /dispensaciones/{id}` - Eliminar dispensación

### 💰 Cobros (`/cobros`)
- `GET /cobros` - Listar cobros
- `POST /cobros` - Crear cobro/factura
- `GET /cobros/{id}` - Obtener cobro
- `PUT /cobros/{id}` - Actualizar estado (PAGADO)
- `DELETE /cobros/{id}` - Eliminar cobro (si no fue pagado)
- `POST /cobros/pago-unico` - Pago único con QR Yape

**Tarifario:**
- `GET /servicios` - Listar servicios/precios
- `POST /servicios` - Crear servicio
- `PUT /servicios/{id}` - Actualizar precio
- `DELETE /servicios/{id}` - Eliminar servicio

**Campañas:**
- `GET /campanias` - Listar campañas activas
- `POST /campanias` - Crear campaña (% descuento)
- `PUT /campanias/{id}` - Actualizar campaña
- `DELETE /campanias/{id}` - Cancelar campaña

### 📧 Notificaciones (`/notificaciones`)
- `GET /notificaciones` - Listar notificaciones
- `POST /notificaciones/enviar-correo-personalizado` - Enviar email personalizado
- `GET /notificaciones/{id}` - Obtener notificación
- `PUT /notificaciones/{id}` - Marcar como leída

### 🔍 Auditoría (`/audit`)
- `GET /audit` - Log de todas las acciones (GET, POST, PUT, DELETE)
- `GET /audit?size=10` - Log paginado (últimas 10 acciones)



## Características Principales

### 🔐 Seguridad
- **JWT RSA-2048** - Autenticación asimétrica
- **Claves en Docker volume** - Compartidas entre microservicios (`jwt-keys/`)
- **`@RolesAllowed`** - Control de acceso por rol en todos los endpoints
- **Auditoría completa** - Todas las acciones se registran en `audit_log`
- **Validación de DNI** - Exactamente 8 dígitos en formularios

### 🎨 Frontend
- **Tema claro/oscuro** - Cambia con `antd.ThemeConfig` + CSS variables
- **Lazy loading** - `React.lazy()` en todas las páginas excepto Login
- **Chunk splitting** - Vite con 3 chunks optimizados: antd-vendor, chart-vendor, lottie-vendor
- **Animaciones Lottie** - Vía CDN (`@lottiefiles/dotlottie-react`)
- **Search por DNI** - Auto-fill de datos del paciente en formularios
- **Doctor availability** - Filtro de doctores sin citas ±30 minutos

### ⚡ Backend
- **Microservicios descentralizados** - 8 servicios independientes
- **Service discovery** - Eureka Registry automático
- **Configuración centralizada** - Spring Cloud Config Server
- **Saga pattern** - Transacciones distribuidas (EventOutbox)
- **Comunicación inter-microservicios** - REST Client + Jakarta HTTP
- **Email SMTP** - Quarkus Mailer con Cloudflare Email Sending (fallback LOGIN si falla OAUTH2)

### 📊 Flujos Clave
1. **Cita con disponibilidad en tiempo real** - DNI search → paciente auto-filled → doctor availability filter → crear cita
2. **Pago único** - Wizard 3 pasos → QR Yape → confirmación manual → email comprobante
3. **Dispensación con email** - Receta pagada → dispensar medicamento → enviar confirmación por email
4. **Triaje + Exámenes** - Signos vitales → orden examen → cargar resultado → ver en dashboard

## Troubleshooting

### 502 Bad Gateway (nginx → backend)
**Causa:** DNS caching en nginx apunta a IP antigua del contenedor.  
**Solución:** Se usa `resolver 127.0.0.11 valid=30s;` + variables en proxy_pass para DNS dinámico.

### Frontend no se actualiza con Docker
**Causa:** El `docker-compose.yml` monta `./frontend/dist` como bind volume.  
**Solución:** Ejecutar `npm run build` localmente ANTES de `docker compose up`.

### Auditoría no registra nada
**Causa:** `MS_PACIENTES_URL` mal configurada en el gateway.  
**Solución:** Verificar `docker-compose.yml` tiene `MS_PACIENTES_URL: http://ms-pacientes:8080` (no localhost).

### Emails no se envían
**Causa:** SMTP Gmail con App Password o OAUTH2 fallando.  
**Solución:** Ver `backend/ms-notificaciones/` application.properties. Si falla OAUTH2, fallback a LOGIN está configurado.

## Postman Collection

Importa `postman-collection.json` en Postman:
- **17 secciones** de endpoints (Auth, Pacientes, Citas, Triaje, Consultas, Exámenes, Recetas, Medicamentos, Dispensaciones, Cobros, Tarifario, Campañas, Notificaciones, Auditoría, Usuarios, Pre-Registro, Verificación)
- **CRUD completo** para cada módulo (GET, POST, PUT, DELETE)
- **Test scripts** automáticos (validaciones de status, tiempo respuesta, JSON)
- **Ejemplos de respuestas** (201, 200, 204)
- **Variables** (baseUrl, tokens, IDs) que se guardan automáticamente

**Bootstrap:** Ejecuta "Registrar primer ADMIN" (si BD vacía) → "Login ADMIN" → tokens se guardan automáticamente para los siguientes requests.

## Roles del Sistema

| Rol | Permisos |
|-----|----------|
| **PACIENTE** | Ver su perfil, agendar citas, ver recetas, ver notificaciones |
| **DOCTOR** | Ver pacientes, gestionar citas, crear consultas, emitir recetas, ver órdenes examen |
| **ENFERMERO** | Realizar triaje (signos vitales) |
| **FARMACEUTICO** | Gestionar medicamentos, dispensar recetas, ver inventario |
| **ATENCION_CLIENTE** | Agendar citas, gestionar datos de pacientes |
| **CAJERO** | Procesar cobros, generar facturas |
| **ADMIN** | Acceso total (CRUD usuarios, roles, tarifario, campañas, auditoría) |

## Contacto & Soporte

**Repositorio:** https://github.com/Jonas26-hash/Sistema-de-monitoreo-de-pacientes  
**Issues:** Reporta bugs o sugerencias en GitHub Issues
