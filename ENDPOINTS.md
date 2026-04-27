# API del Sistema de Monitoreo de Pacientes

## Endpoints por Microservicio

---

## MS-PACIENTES (Puerto 8081)

### Autenticacion
```bash
# Registrar usuario
POST http://localhost:8081/auth/register
Content-Type: application/json
{
    "username": "admin",
    "password": "admin123",
    "email": "admin@hospital.com",
    "roles": ["ADMIN"]
}

# Login
POST http://localhost:8081/auth/login
Content-Type: application/json
{
    "username": "admin",
    "password": "admin123"
}
```

### Pacientes
```bash
# Listar todos
GET http://localhost:8081/pacientes

# Crear paciente
POST http://localhost:8081/pacientes
Content-Type: application/json
{
    "nombres": "Juan",
    "apellidoPaterno": "Perez",
    "apellidoMaterno": "Gomez",
    "dni": "12345678",
    "fechaNacimiento": "1990-05-15",
    "genero": "M",
    "telefono": "999888777",
    "email": "juan@perez.com"
}

# Buscar por ID
GET http://localhost:8081/pacientes/1

# Buscar por DNI
GET http://localhost:8081/pacientes/dni/12345678

# Actualizar
PUT http://localhost:8081/pacientes/1
Content-Type: application/json
{
    "nombres": "Juan Carlos",
    "apellidoPaterno": "Perez"
}
```

---

## MS-ATENCION (Puerto 8082)

### Citas
```bash
# Listar citas
GET http://localhost:8082/citas

# Crear cita
POST http://localhost:8082/citas
Content-Type: application/json
{
    "pacienteId": 1,
    "doctorId": 1,
    "fechaHora": "2026-04-25T09:00:00",
    "estado": "PROGRAMADA",
    "motivo": "Consulta general"
}

# Buscar cita por ID
GET http://localhost:8082/citas/1

# Obtener paciente de una cita (usa OpenFeign/REST Client)
GET http://localhost:8082/citas/1/paciente

# Citas por paciente
GET http://localhost:8082/citas/paciente/1
```

### Consultas
```bash
# Listar consultas
GET http://localhost:8082/consultas

# Crear consulta
POST http://localhost:8082/consultas
Content-Type: application/json
{
    "pacienteId": 1,
    "citaId": 1,
    "doctorId": 1,
    "fechaConsulta": "2026-04-25T10:00:00",
    "sintomas": "Dolor de cabeza",
    "diagnostico": "Migraña",
    "tratamiento": "Paracetamol 500mg"
}

# Consultas por paciente
GET http://localhost:8082/consultas/paciente/1
```

---

## MS-RECETAS (Puerto 8083)

```bash
# Listar recetas
GET http://localhost:8083/recetas

# Crear receta
POST http://localhost:8083/recetas
Content-Type: application/json
{
    "pacienteId": 1,
    "doctorId": 1,
    "consultaId": 1,
    "fechaEmision": "2026-04-25",
    "fechaVigencia": "2026-05-25",
    "medicamentos": "Paracetamol 500mg - 1 ogni 8 horas por 5 dias",
    "indicaciones": "Tomar con alimentos"
}

# Recetas por paciente
GET http://localhost:8083/recetas/paciente/1

# Buscar receta por ID
GET http://localhost:8083/recetas/1
```

---

## MS-FARMACIA (Puerto 8084)

### Medicamentos
```bash
# Listar medicamentos
GET http://localhost:8084/medicamentos

# Crear medicamento
POST http://localhost:8084/medicamentos
Content-Type: application/json
{
    "codigo": "PARA001",
    "nombre": "Paracetamol 500mg",
    "descripcion": "Analgésico y antifebril",
    "presentacion": "Tableta",
    "stock": 100,
    "stockMinimo": 20
}

# Buscar por ID
GET http://localhost:8084/medicamentos/1

# Buscar por codigo
GET http://localhost:8084/medicamentos/codigo/PARA001

# Actualizar stock
PUT http://localhost:8084/medicamentos/1
Content-Type: application/json
{
    "stock": 95
}
```

### Dispensaciones
```bash
# Listar dispensaciones
GET http://localhost:8084/dispensaciones

# Crear dispensacion
POST http://localhost:8084/dispensaciones
Content-Type: application/json
{
    "recetaId": 1,
    "medicamentoId": 1,
    "cantidad": 15,
    "fechaDispensacion": "2026-04-25",
    "farmaceuticoId": 1
}

# Dispensaciones por receta
GET http://localhost:8084/dispensaciones/receta/1
```

---

## MS-COBROS (Puerto 8085)

```bash
# Listar cobros
GET http://localhost:8085/cobros

# Crear cobro
POST http://localhost:8085/cobros
Content-Type: application/json
{
    "pacienteId": 1,
    "tipo": "CONSULTA",
    "referenciaId": 1,
    "monto": 50.00,
    "estado": "PENDIENTE",
    "descripcion": "Consulta medica general"
}

# Cobros por paciente
GET http://localhost:8085/cobros/paciente/1

# Buscar por ID
GET http://localhost:8085/cobros/1
```

---

## MS-NOTIFICACIONES (Puerto 8086)

```bash
# Listar notificaciones
GET http://localhost:8086/notificaciones

# Crear notificacion
POST http://localhost:8086/notificaciones
Content-Type: application/json
{
    "pacienteId": 1,
    "tipo": "RECORDATORIO",
    "mensaje": "Tiene una cita programada para manana a las 9:00 AM",
    "canal": "EMAIL"
}

# Notificaciones por paciente
GET http://localhost:8086/notificaciones/paciente/1
```

---

## API GATEWAY (Puerto 8080)

Todos los endpoints anteriores también están disponibles a través del API Gateway:

```bash
# Ejemplo via Gateway
GET http://localhost:8080/pacientes
GET http://localhost:8080/auth/login
GET http://localhost:8080/citas
GET http://localhost:8080/recetas
GET http://localhost:8080/medicamentos
```

---

## Flujo de Prueba Completo

```bash
# 1. Registrar usuario admin
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","email":"admin@hospital.com","roles":["ADMIN"]}'

# 2. Login para obtener token
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 3. Crear paciente (usando el token)
curl -X POST http://localhost:8081/pacientes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN_AQUI" \
  -d '{"nombres":"Maria","apellidoPaterno":"Garcia","dni":"87654321"}'

# 4. Crear cita para el paciente
curl -X POST http://localhost:8082/citas \
  -H "Content-Type: application/json" \
  -d '{"pacienteId":1,"doctorId":1,"fechaHora":"2026-04-25T09:00:00","estado":"PROGRAMADA","motivo":"Control general"}'

# 5. Obtener datos del paciente desde la cita (OpenFeign)
curl http://localhost:8082/citas/1/paciente

# 6. Crear receta
curl -X POST http://localhost:8083/recetas \
  -H "Content-Type: application/json" \
  -d '{"pacienteId":1,"doctorId":1,"fechaEmision":"2026-04-25","medicamentos":"Vitamina C - 1 diaria","indicaciones":"Tomar en la manana"}'

# 7. Crear medicamento
curl -X POST http://localhost:8084/medicamentos \
  -H "Content-Type: application/json" \
  -d '{"codigo":"VITC001","nombre":"Vitamina C 1g","stock":50}'

# 8. Crear cobro
curl -X POST http://localhost:8085/cobros \
  -H "Content-Type: application/json" \
  -d '{"pacienteId":1,"tipo":"CONSULTA","monto":50.00,"estado":"PENDIENTE"}'

# 9. Enviar notificacion
curl -X POST http://localhost:8086/notificaciones \
  -H "Content-Type: application/json" \
  -d '{"pacienteId":1,"tipo":"RECORDATORIO","mensaje":"Su cita es manana","canal":"SMS"}'
```

---

## Resumen de Puertos

| Servicio | Puerto |
|----------|--------|
| Config Server | 8888 |
| Registry Server | 8761 |
| ms-pacientes | 8081 |
| ms-atencion | 8082 |
| ms-recetas | 8083 |
| ms-farmacia | 8084 |
| ms-cobros | 8085 |
| ms-notificaciones | 8086 |
| api-gateway | 8080 |
