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

## Ejecucion

### Docker Compose

```bash
# Levantar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f

# Ver servicios corriendo
docker-compose ps
```

**Tiempo estimado:** 10-15 minutos (primer build)

### Desarrollo local

Requiere: Java 21 + Maven

```bash
# 1. Iniciar BDs
docker-compose up -d db-pacientes db-atencion db-recetas db-farmacia db-cobros db-notificaciones

# 2. Config Server (Puerto 8888)
cd ms-config-server
.\mvnw.cmd spring-boot:run

# 3. Eureka Server (Puerto 8761)
cd ms-registry-server
.\mvnw.cmd spring-boot:run

# 4. Microservicios (cada uno en terminal separada)
cd ms-pacientes && .\mvnw.cmd quarkus:dev      # Puerto 8081
cd ms-atencion && .\mvnw.cmd quarkus:dev       # Puerto 8082
cd ms-recetas && .\mvnw.cmd quarkus:dev         # Puerto 8083
cd ms-farmacia && .\mvnw.cmd quarkus:dev        # Puerto 8084
cd ms-cobros && .\mvnw.cmd quarkus:dev          # Puerto 8085
cd ms-notificaciones && .\mvnw.cmd quarkus:dev # Puerto 8086

# 5. API Gateway (Puerto 8080)
cd api-gateway && .\mvnw.cmd quarkus:dev
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