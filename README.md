# Sistema de Monitoreo de Pacientes - Arquitectura de Microservicios

## Arquitectura General

```
                              ┌─────────────────┐
                              │   API Gateway    │
                              │    Puerto 8080   │
                              └────────┬────────┘
                                       │
           ┌───────────┬───────────────┼───────────────┬───────────┐
           ▼           ▼               ▼               ▼           ▼
    ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
    │  Config  │ │ Registry │ │ Pacientes│ │ Atencion │ │  Recetas │
    │ Server   │ │ Server   │ │  Puerto  │ │  Puerto  │ │  Puerto  │
    │ Puerto   │ │ Puerto   │ │   8081   │ │   8082   │ │   8083   │
    │  8888    │ │  8761    │ └──────────┘ └──────────┘ └──────────┘
    └──────────┘ └──────────┘
           ▲           ▲
           │           │
           └───────────┘
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

## Tecnologias

- **Backend**: Quarkus (microservicios), Spring Boot (infraestructura)
- **Base de datos**: PostgreSQL
- **Configuracion**: Spring Cloud Config Server
- **Registro**: Netflix Eureka
- **Comunicacion**: REST Client (equivalente a OpenFeign)
- **Contenedores**: Docker, Docker Compose

## Ejecucion con Docker

Esta es la forma recomendada para levantar el proyecto completo. No es necesario ejecutar
`mvnw.cmd` desde el IDE si se usa Docker Compose.

### Requisitos

- Docker Desktop instalado y en ejecucion.
- Git instalado.
- Puertos libres: `8080`, `8081`, `8082`, `8083`, `8084`, `8085`, `8086`, `8761`, `8888`, `5432` a `5437`.

### Clonar y levantar por primera vez

```powershell
git clone <https://github.com/Jonas26-hash/Sistema-de-monitoreo-de-pacientes.git>
cd Sistema-de-monitoreo-de-pacientes
docker compose up -d --build
```

El primer build puede tardar varios minutos porque descarga dependencias Maven e imagenes Docker.

Despues de levantar, esperar unos segundos y verificar:

```powershell
docker compose ps
```

Todos los servicios principales deben aparecer en estado `Up`.

### Probar que el sistema responde

El punto de entrada principal es el API Gateway:

```text
http://localhost:8080
```

Prueba rapida:

```http
GET http://localhost:8080
```

### Crear el primer usuario ADMIN

Cuando la base de datos esta vacia, el primer usuario se registra sin token y se crea como `ADMIN`.

```http
POST http://localhost:8080/auth/register
Content-Type: application/json
```

Body:

```json
{
  "username": "admin",
  "password": "admin123",
  "email": "admin@hospital.com",
  "nombres": "Administrador",
  "apellidos": "Sistema"
}
```

Luego iniciar sesion:

```http
POST http://localhost:8080/auth/login
Content-Type: application/json
```

Body:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

La respuesta devuelve un token JWT. Para endpoints protegidos usar:

```text
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


## Ejecucion manual para desarrollo

Usar este modo solo si se quiere desarrollar desde el IDE. En ese caso se recomienda levantar
solo las bases de datos con Docker y ejecutar los servicios manualmente en terminales separadas.

```powershell
docker compose up -d db-pacientes db-atencion db-recetas db-farmacia db-cobros db-notificaciones
```

Luego iniciar cada servicio en una terminal separada:

```powershell
cd ms-config-server
.\mvnw.cmd spring-boot:run
```

```powershell
cd ms-registry-server
.\mvnw.cmd spring-boot:run
```

```powershell
cd ms-pacientes
.\mvnw.cmd quarkus:dev
```

```powershell
cd ms-atencion
.\mvnw.cmd quarkus:dev
```

```powershell
cd ms-recetas
.\mvnw.cmd quarkus:dev
```

```powershell
cd ms-farmacia
.\mvnw.cmd quarkus:dev
```

```powershell
cd ms-cobros
.\mvnw.cmd quarkus:dev
```

```powershell
cd ms-notificaciones
.\mvnw.cmd quarkus:dev
```

```powershell
cd api-gateway
.\mvnw.cmd quarkus:dev
```


## Endpoints Principales

### Autenticacion
- `POST /auth/login` - Iniciar sesion
- `POST /auth/register` - Registrar usuario

### Pacientes
- `GET /pacientes` - Listar pacientes
- `POST /pacientes` - Crear paciente
- `GET /pacientes/{id}` - Obtener paciente

### Atencion
- `GET /citas` - Listar citas
- `POST /citas` - Crear cita
- `GET /citas/{id}/paciente` - Obtener datos del paciente via OpenFeign

### Recetas
- `GET /recetas` - Listar recetas
- `POST /recetas` - Crear receta

### Farmacia
- `GET /medicamentos` - Listar medicamentos
- `POST /dispensaciones` - Dispensar medicamento

## Comunicacion entre Microservicios

### Ejemplo: ms-atencion llama a ms-pacientes (OpenFeign/REST Client)

```java
@Path("/pacientes")
@RegisterRestClient(configKey = "paciente-api")
public interface PacienteClient {
    @GET
    @Path("/{id}")
    PacienteDTO getPaciente(@PathParam("id") Long id);
}
```

## Configuracion Centralizada

Los archivos de configuracion estan en `central-config/config-properties/`:

- `common.properties` - Configuracion compartida
- `ms-pacientes.properties` - Config especifica de pacientes
- `ms-atencion.properties` - Config especifica de atencion
- etc.

## Roles del Sistema

| Rol              | Descripcion                                           |
|------------------|-------------------------------------------------------|
| PACIENTE         | Puede ver su perfil, agendar citas, ver recetas       |
| DOCTOR           | Puede ver historial, gestionar citas, generar recetas |
| FARMACEUTICO     | Puede gestionar inventario, dispensar medicamentos    |
| ATENCION_CLIENTE | Puede ayudar con datos de pacientes, gestionar citas  |
| CAJERO           | Puede procesar pagos de consultas                     |
| ADMIN            | Acceso total al sistema                               |
