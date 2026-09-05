# Pedidos360 — Backend Cloud-Native Multi-Nube

Repositorio desarrollado por Brian Aravena y Fabián Reyes.

Backend de **Pedidos360** (tienda de equipamiento de montaña "SummitLab"), migrado a una
arquitectura Cloud-Native de microservicios:

| Microservicio | Ruta | Puerto local | Seguridad |
|---|---|---|---|
| `servicio-productos` | `/products` (GET) | 8081 | Pública (catálogo) |
| `servicio-pedidos` | `/orders` (POST/GET) | 8082 | JWT Azure AD / Entra ID + scopes/roles |

El microservicio de **Identidad/Login** no vive acá: lo administra **Azure AD / Microsoft
Entra ID** (flujo OAuth 2.0 / OIDC con PKCE) y el **AWS API Gateway** valida los tokens
JWT en el borde antes de enrutar hacia estos servicios.

> El código interno (paquetes, clases, variables) está en **español**.
> Los nombres de los **campos JSON** (`name`, `items`, `totals`, `productId`, …) se
> mantienen en inglés porque son el **contrato que consume el frontend** y no deben romperse.
>
> 📌 **Documento de continuación**: `NOTAS.md` tiene el estado completo del proyecto
> (frontend + backend) y los pasos detallados para terminar la conexión.

---

## 1. Estructura

```
servicio-productos/  Microservicio de Productos (catálogo semilla con los 8 mock del front)
servicio-pedidos/    Microservicio de Carrito/Órdenes (protegido con JWT)
docker-compose.yml   Levanta ambos localmente
NOTAS.md             Notas del frontend (estado e integración pendiente)
```

## 2. Cómo compilar y correr

Requisitos: JDK 17 o superior y Maven (se usa el wrapper `mvnw`).

```bash
mvn clean package          # compila y corre los tests de ambos microservicios
```

### Opción A — Maven directo
```bash
java -jar servicio-productos/target/servicio-productos-0.0.1-SNAPSHOT.jar   # → :8081
java -jar servicio-pedidos/target/servicio-pedidos-0.0.1-SNAPSHOT.jar       # → :8082
```

### Opción B — Docker Compose
```bash
mvn clean package
docker compose up --build
```
- Productos: http://localhost:8081/products
- Órdenes: http://localhost:8082/orders

> En el contenedor ambos escuchan en `SERVER_PORT=8080` (mapeado a 8081/8082 en el host).

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

---

## 4. Conectar los datos de Azure (AD / Entra ID)

El `servicio-pedidos` valida los tokens **además** del JWT Authorizer del API Gateway
(defensa en profundidad). Todo se configura por variables de entorno:

| Variable | Ejemplo |
|---|---|
| `JWT_ENABLED` | `true` |
| `JWT_JWKS_URI` | `https://login.microsoftonline.com/{TU-TENANT}/discovery/v2.0/keys` |
| `JWT_ISSUER_URI` | `https://login.microsoftonline.com/{TU-TENANT}/v2.0` |
| `JWT_AUDIENCES` | `api://{TU-CLIENT-ID}` |
| `JWT_REQUIRED_SCOPES` | `orders.write` |
| `JWT_REQUIRED_ROLES` | _(opcional)_ p.ej. `Orders.Write` |

Mapeo interno (propiedades Spring, ya resueltas desde las variables): `app.seguridad.habilitado`,
`app.seguridad.jwks-uri`, `app.seguridad.emisor-uri`, `app.seguridad.audiencias`,
`app.seguridad.scopes-requeridos`, `app.seguridad.roles-requeridos`.

Validaciones en el microservicio:
- Firma y vigencia contra el **JWKS** de Azure (RS256).
- `iss` debe coincidir con `app.seguridad.emisor-uri`.
- `aud` debe contener uno de los valores de `app.seguridad.audiencias`.
- Debe traer el scope `orders.write` (claim `scp`/`scope`) o el rol configurado
  (claim `roles`).
- `401` sin token o token inválido/expirado; `403` sin scope/rol.

> Con `JWT_ENABLED=false` (default) el servicio corre en **modo demo** sin token:
> ideal para probar contra el frontend con `VITE_USE_MOCK=false` antes de conectar Azure.

---

## 5. Conectar AWS (API Gateway + contenedores)

1. Build de las imágenes (o usar ECR):
   ```bash
   docker build -t pedidos360/servicio-productos:latest servicio-productos/
   docker build -t pedidos360/servicio-pedidos:latest servicio-pedidos/
   ```
2. Desplegar los contenedores en **ECS** (task definitions con `SERVER_PORT=8080`
   y health check en `/actuator/health`).
3. **API Gateway** como único entrypoint:
   - `GET /products` → integración HTTP hacia `servicio-productos` (pública).
   - `POST /orders`, `GET /orders` → integración hacia `servicio-pedidos` con
     **JWT Authorizer** conectado a los JWKS de Azure (rechaza 401/403).
   - CORS restringido al dominio del frontend.
4. En el frontend, `.env` apunta al Gateway:
   ```
   VITE_API_BASE_URL=https://TU-APIGATEWAY.execute-api.REGION.amazonaws.com/prod
   VITE_USE_MOCK=false
   ```

---

## 7. Estado del proyecto y próximos pasos (handoff)

**Hecho ✅**
- Backend multi-módulo listo y verificado: `servicio-productos` + `servicio-pedidos`
  (`mvn clean package` → BUILD SUCCESS, 8/8 tests, pruebas 200/201/CORS/401).
- Contrato de API calza con `src/services/api.js` y `mockProducts.js`.
- `order-service` valida JWT contra JWKS de Azure (firma, vigencia, issuer, audience,
  scopes/roles) activable por variables de entorno.

**Pendiente de conectar ⏳** (no hay que tocar código, solo infraestructura)
1. **Azure**: crear App Registration (Tenant/Client ID, redirect de `http://localhost:5173`,
   scope `orders.write`, App Roles opcionales) y setear las variables JWT de la §4.
2. **AWS**: subir las imágenes (Dockerfiles incluidos) a ECR, desplegar en ECS y crear el
   **API Gateway** con JWT Authorizer (Azure JWKS) + CORS al dominio del front.
3. **Frontend**: configurar MSAL con los datos de Azure y apuntar
   `VITE_API_BASE_URL=https://TU-APIGATEWAY.execute-api.REGION.amazonaws.com/prod`
   con `VITE_USE_MOCK=false`.
4. **Evidencias**: login obteniendo el JWT desde Azure, 401/403 sin token y 200 autorizado
   en el Gateway, `GET /products` y `POST /orders` en el backend.

Pasos detallados en `NOTAS.md` (§9 a §12).

---

## 8. Notas

- Los datos de productos/pedidos viven en **memoria** (mapas concurrentes),
  pensado para la demo. Se puede persistir con DynamoDB/RDS sin cambiar controladores.
- El `customerEmail`/`customerName` del pedido sale de los claims del JWT
  (`preferred_username`/`upn` y `name`) cuando la validación está activa.
- CORS configurable por servicio vía `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`).