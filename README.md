# Inventory API

API REST de gestión de productos e inventario construida con Java 21 y Spring Boot 3.

## Tech Stack

| Capa | Tecnología |
|------|-----------|
| Framework | Spring Boot 3 |
| Seguridad | Spring Security + JWT (JJWT 0.12) |
| Persistencia | Spring Data JPA + PostgreSQL |
| Validación | Bean Validation (Jakarta) |
| Reducción de boilerplate | Lombok |
| Testing | JUnit 5 + Mockito |

## Estructura del proyecto

```
src/main/java/com/joaquin/inventory/
├── config/          # Configuración CORS y Security
├── controller/      # Endpoints REST (Auth, Product, Category)
├── dto/             # Request/Response objects
├── entity/          # Entidades JPA (Product, Category, User)
├── exception/       # Manejo global de excepciones
├── repository/      # Interfaces JPA con queries personalizadas
├── security/        # JwtUtil + JwtFilter
└── service/         # Lógica de negocio
```

## Endpoints

### Auth
| Método | Endpoint | Acceso |
|--------|----------|--------|
| POST | `/api/auth/login` | Pública |
| POST | `/api/auth/register` | Pública |
| POST | `/api/auth/refresh` | Pública |
| POST | `/api/auth/logout` | USER/ADMIN |
| GET | `/api/auth/me` | Autenticada |

### Productos
| Método | Endpoint | Acceso |
|--------|----------|--------|
| GET | `/api/products` | Pública |
| GET | `/api/products?search=monitor` | Pública |
| GET | `/api/products?categoryId=1` | Pública |
| GET | `/api/products/{id}` | Pública |
| POST | `/api/products` | ADMIN |
| PUT | `/api/products/{id}` | ADMIN |
| DELETE | `/api/products/{id}` | ADMIN |

### Categorías
| Método | Endpoint | Acceso |
|--------|----------|--------|
| GET | `/api/categories` | Pública |
| GET | `/api/categories/{id}` | Pública |
| POST | `/api/categories` | ADMIN |
| PUT | `/api/categories/{id}` | ADMIN |
| DELETE | `/api/categories/{id}` | ADMIN |

### Órdenes
| Método | Endpoint | Acceso |
|--------|----------|--------|
| POST | `/api/orders` | Pública / JWT opcional |
| GET | `/api/orders/my-orders` | USER / ADMIN |
| GET | `/api/orders/{id}` | USER propietario / ADMIN |
| GET | `/api/orders` | ADMIN |
| PATCH | `/api/orders/{id}/status` | ADMIN |

`POST /api/orders` soporta header opcional `Idempotency-Key` para deduplicar reintentos.

## Cómo ejecutar

### Prerrequisitos
- Java 21
- Maven 3.9+
- PostgreSQL 15+

### Setup

1. Crear la base de datos:
```sql
CREATE DATABASE inventory_db;
```

2. Ajustar credenciales en `src/main/resources/application.properties`

3. Ejecutar:
```bash
./mvnw spring-boot:run
```

El servidor arranca en `http://localhost:8080`

Swagger UI disponible en `http://localhost:8080/swagger-ui/index.html`

## Seguridad y operación

- Access token + refresh token con rotación (`/api/auth/refresh`)
- Rate limiting en login configurable por propiedades
- CORS configurable por entorno con `CORS_ALLOWED_ORIGINS`
- Secretos por variables de entorno (`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`)
- Formato de error JSON estandarizado con `code`, `message`, `fieldErrors` y `path`

### Usuario admin por defecto
```
username: admin
password: Admin1234!
```

### Ejemplo de login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin1234!"}'
```

### Ejemplo de crear producto (con token)
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Monitor 4K","price":399.99,"stock":15}'
```

## Tests

```bash
./mvnw test
```

## Deployment en Render

### Preparación

1. **Crear repositorio en GitHub**
   - Crear nuevo repo privado o público en github.com
   - Clone local, agregar archivos, commit y push:
   ```bash
   git init
   git add .
   git commit -m "Initial commit: inventory API ready for deployment"
   git branch -M main
   git remote add origin https://github.com/usuario/inventory-api.git
   git push -u origin main
   ```

2. **Crear cuenta en Render.com**
   - Ve a render.com y crea cuenta (gratuita)
   - Conecta tu cuenta de GitHub

### Deploy automático

**Opción 1: Blueprint (Recomendado)**
1. En Render dashboard: `+ New` → `Blueprint`
2. Selecciona el repositorio
3. Render detectará automáticamente `render.yaml`
4. Revisa variables de entorno (especialmente `JWT_SECRET` y `CORS_ALLOWED_ORIGINS`)
5. Click `Deploy` (tarda ~5 minutos)

**Opción 2: Manual**
1. En Render dashboard: `+ New` → `Web Service`
2. Conecta tu repo GitHub
3. Configura:
   - **Name**: inventory-api
   - **Runtime**: Docker
   - **Build Command**: Automático (detecta Dockerfile)
   - **Start Command**: Automático
4. En `Environment`:
   - `JWT_SECRET`: clave segura (min 32 caracteres)
   - `CORS_ALLOWED_ORIGINS`: URL del frontend (ej: `https://mi-frontend.vercel.app`)
   - `DB_USERNAME`, `DB_PASSWORD`: Render los inyecta automáticamente
   - `DATABASE_URL`: Render lo inyecta automáticamente

5. Click `Create Web Service`
6. En la misma pestaña, ve a `Databases` → `+ New Database`
   - **Name**: postgres
   - **PostgreSQL Version**: 15
   - Crea la base de datos

7. En Web Service, actualiza `Environment` con las credenciales de PostgreSQL generadas

### Variables de entorno en producción
```
JWT_SECRET=tu-clave-super-secreta-minimo-32-caracteres-aqui!
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=1209600000
CORS_ALLOWED_ORIGINS=https://tu-frontend.vercel.app
LOGIN_MAX_ATTEMPTS=5
LOGIN_WINDOW_MS=60000
```

### Health check
El API incluye endpoint `/health` para Render. La aplicación se considera "healthy" si responde con `{"status":"UP"}`.

### URLs después del deploy
- **API Base**: `https://inventory-api.onrender.com`
- **Swagger**: `https://inventory-api.onrender.com/swagger-ui/index.html`
- **Health**: `https://inventory-api.onrender.com/health`

### Troubleshooting
- **Logs en Render**: Ve a tu servicio → `Logs` para ver errores
- **Conexión BD**: Si falla, revisa en Render → `Databases` que la BD esté disponible
- **CORS errors**: Actualiza `CORS_ALLOWED_ORIGINS` con la URL exacta del frontend
- **Token auth fail**: Revisa que `JWT_SECRET` sea la misma en variables de entorno
