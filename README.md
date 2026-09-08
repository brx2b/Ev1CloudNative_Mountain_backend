# Pedidos360 — Backend Cloud-Native Multi-Nube

Repositorio desarrollado por Brian Aravena y Fabián Reyes.

Backend de **Pedidos360** (tienda de equipamiento de montaña "SummitLab"), migrado a una
arquitectura Cloud-Native de microservicios:

| Microservicio | Rutas principales | Puerto local | Seguridad |
|---|---|---|---|
| `servicio-productos` | `/products` (GET) | 8081 | Pública (catálogo) |
| `servicio-pedidos` | `/orders` (POST/GET) | 8082 | JWT local HS256 + Azure JWKS (opcional) |
| `servicio-usuarios` | `/auth/registro`, `/auth/ingreso`, `/users` | 8083 | Pública (registro/login) |
| `servicio-gateway` | **Entry point único** — enruta todo a los MS internos | 9000 | Propaga Authorization al MS destino |

**Registro de endpoints**: cada servicio expone `GET /actuator` + `GET /api/endpoints` con
un inventario JSON de sus rutas. El gateway consolida todos en `GET /api/endpoints`.

> El código interno (paquetes, clases, variables) está en **español**.
> Los nombres de los **campos JSON** (`name`, `items`, `totals`, `productId`, …) se
> mantienen en inglés porque son el **contrato que consume el frontend** y no deben romperse.
>
> 📌 **Documento de continuación**: `NOTAS.md` tiene el estado completo del proyecto
> (frontend + backend) y los pasos detallados para terminar la conexión.

---

## 1. Estructura

```
servicio-productos/    Microservicio de Productos (catálogo semilla con los 8 mock del front)
servicio-pedidos/      Microservicio de Carrito/Órdenes (JWT local + Azure JWKS opcional)
servicio-usuarios/     Microservicio de Registro/Login (emite JWT HS256 propio)
servicio-gateway/      API Gateway / Proxy (entry point único, enruta y consolida endpoints)
docker-compose.yml     Levanta los 4 microservicios
NOTAS.md               Notas de continuación del proyecto completo
```

## 2. Cómo compilar y correr

Requisitos: JDK 17 o superior y Maven (se usa el wrapper `mvnw`).

```bash
mvn clean package          # compila y corre los tests de los 4 microservicios
```

### Opción A — Maven directo
```bash
java -jar servicio-productos/target/servicio-productos-0.0.1-SNAPSHOT.jar   # → :8081
java -jar servicio-pedidos/target/servicio-pedidos-0.0.1-SNAPSHOT.jar       # → :8082
java -jar servicio-usuarios/target/servicio-usuarios-0.0.1-SNAPSHOT.jar     # → :8083
java -jar servicio-gateway/target/servicio-gateway-0.0.1-SNAPSHOT.jar       # → :9000
```

### Opción B — Docker Compose
```bash
mvn clean package
docker compose up --build
```
- Productos: http://localhost:8081/products
- Órdenes: http://localhost:8082/orders
- Usuarios: http://localhost:8083/auth/ingreso
- **Gateway (entrypoint)**: http://localhost:9000/products

> En los contenedores todos escuchan en `SERVER_PORT=8080` (mapeados a 8081/8082/8083/9000 en el host).

---

## 3. Contrato de API (lo que consume el frontend)

### GET `/products`
Devuelve un **array de productos** con la misma forma que `mockProducts.js`:

```json
[
  {
    "id": 1,
    "name": "Alpha SV Jacket",
    "description": "...",
    "price": 899,
    "image": "https://...",
    "membrane": "Gore-Tex Pro 3L",
    "tempRating": "-30°C",
    "waterproof": "28,000 mm",
    "weight": "430g",
    "activity": ["alpinismo", "expedicion"],
    "category": "hardshell",
    "brand": "Summit Lab",
    "colors": ["#1a1a2e", "#c0392b", "#2d3436"]
  }
]
```

### GET `/products/{id}`
Devuelve un único producto (`200`) o `404` si no existe.

### POST `/orders` _(requiere JWT cuando Azure está conectado)_
Body aceptado (el frontend envía los items del carrito, que incluyen el producto completo):

```json
{
  "items": [
    { "id": 1, "name": "Alpha SV Jacket", "price": 899, "quantity": 2 }
  ]
}
```
- Acepta `id` o `productId` por item; `price` y `quantity` opcionales (el total se
  recalcula en el servidor, nunca se confía en el cliente).
- Respuesta `201`:

```json
{
  "id": "PEDIDO-100001",
  "status": "RECIBIDO",
  "customerEmail": "usuario@empresa.cl",
  "customerName": null,
  "createdAt": "2026-09-05T12:00:00Z",
  "items": [ { "productId": 1, "name": "Alpha SV Jacket", "price": 899, "quantity": 2 } ],
  "totals": { "count": 2, "subtotal": 1798 }
}
```
- `400` si `items` viene vacío o sin `id`/`productId`.

### GET `/orders` y GET `/orders/{id}` _(requieren JWT)_
Listan los pedidos registrados. Por defecto el id se genera en memoria
(`PEDIDO-XXXXXX`); para producción se puede reemplazar el repositorio por
DynamoDB/RDS sin tocar el controlador.

### POST `/auth/registro`
Registra un usuario nuevo y devuelve un JWT:

```json
{
  "token": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "id": 1, "name": "Fabián Reyes", "email": "fabian@summitlab.cl", "rol": "CLIENTE" }
}
```
- `409` si el email ya existe.

### POST `/auth/ingreso`
Login: misma respuesta que registro. `401` si las credenciales son incorrectas.

### GET `/users` y GET `/users/{id}`
Listan usuarios registrados (solo datos demo).

### GET `/api/endpoints` _(en cada servicio y consolidado en el gateway)_
Inventario JSON de las rutas del servicio, útil para monitoreo y debug.

---

## 4. Seguridad JWT (local + Azure opcional)

`servicio-pedidos` valida tokens con **doble modo**:

**Modo local** (default, para desarrollo):
| Variable | Default |
|---|---|
| `JWT_LOCAL_ENABLED` | `true` |
| `JWT_LOCAL_SECRET` | `cambiar-en-produccion-secreto-compartido-32bytes` |
| `JWT_LOCAL_ISSUER` | `servicio-usuarios` |

Valida HS256 contra el secreto compartido. `servicio-usuarios` firma con el mismo secreto.

**Modo Azure** (producción):
| Variable | Ejemplo |
|---|---|
| `JWT_LOCAL_ENABLED` | `false` |
| `JWT_ENABLED` | `true` |
| `JWT_JWKS_URI` | `https://login.microsoftonline.com/{TENANT}/discovery/v2.0/keys` |
| `JWT_ISSUER_URI` | `https://login.microsoftonline.com/{TENANT}/v2.0` |
| `JWT_AUDIENCES` | `api://{CLIENT-ID}` |
| `JWT_REQUIRED_SCOPES` | `orders.write` |

Con ambos desactivados el servicio corre en **modo demo** sin token.

---

## 5. Conectar AWS (producción)

En producción el `servicio-gateway` local se reemplaza por **AWS API Gateway** (o se usa el
gateway como sidecar/ALB). Imágenes y contenedores:

1. Build de imágenes:
   ```bash
   docker build -t pedidos360/servicio-productos:latest servicio-productos/
   docker build -t pedidos360/servicio-pedidos:latest servicio-pedidos/
   docker build -t pedidos360/servicio-usuarios:latest servicio-usuarios/
   docker build -t pedidos360/servicio-gateway:latest servicio-gateway/
   ```
2. Desplegar en **ECS** (task defs con `SERVER_PORT=8080` y health check `/actuator/health`).
3. **API Gateway** en AWS enruta: `/products` → productos, `/auth/*` → usuarios,
   `/orders` → pedidos (con JWT Authorizer si se usa Azure).
4. En el frontend `.env`:
   ```
   VITE_API_BASE_URL=https://TU-APIGATEWAY.execute-api.REGION.amazonaws.com/prod
   VITE_USE_MOCK=false
   ```

---

## 7. Estado del proyecto

**Hecho**
- Backend de 4 microservicios listo: productos, pedidos, usuarios, gateway
  (`mvn clean package` → BUILD SUCCESS, 17 tests).
- Gateway como entrypoint único: enruta `/auth/*` → usuarios, `/products` → productos,
  `/orders` → pedidos, `/api/endpoints` → consolidado.
- JWT local HS256: `servicio-usuarios` firma, `servicio-pedidos` valida.
- Registro de endpoints: `GET /api/endpoints` en cada servicio y consolidado en el gateway.
- Frontend conectado: `authService.register/login/logout` + Bearer token automático.
- Botones "Ingresar" y "Crear cuenta" abren `AuthModal` (login/registro).
- Checkout sin token muestra toast + abre modal de login.

**Pendiente (infraestructura, no código)**
1. Azure: App Registration + variables JWT si se quiere validación RS256 en producción.
2. AWS: ECR + ECS + API Gateway (reemplaza `servicio-gateway` en el deploy).
3. Frontend: `.env` apuntando al API Gateway de AWS.

Pasos detallados en `NOTAS.md`.

---

## 8. Notas

- Los datos viven en **memoria** (mapas concurrentes). Persistir con DynamoDB/RDS sin cambiar controladores.
- `servicio-usuarios` emite JWT HS256 con claim `email`; `servicio-pedidos` lo lee para `customerEmail`.
- CORS configurable por servicio vía `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`).
- El secreto JWT compartido (`JWT_LOCAL_SECRET` / `app.auth.secreto`) debe ser el mismo en usuarios y pedidos.
- Seed de usuario demo: `demo@summitlab.cl` / `demo1234`.