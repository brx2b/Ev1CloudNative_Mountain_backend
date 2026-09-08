# Pedidos360 — Notas de Continuación

> Frontend de la tienda "SummitLab" (equipamiento de montaña) dentro del proyecto
> **Pedidos360: Arquitectura Cloud-Native Multi-Nube**.
> Este archivo resume el estado actual, cómo correr el proyecto, y qué falta para
> seguir otro día.

---

## 1. Cómo levantar el proyecto (en una sesión futura)

```bash
npm install        # solo la primera vez
npm run dev        # desarrollo en http://localhost:5173
npm run build      # build de producción (dist/)
npm run preview    # sirve el build
npm run lint       # ESLint
```

- Stack: **React 19 + Vite 8 + Tailwind CSS v4** (sin router todavía, página única con anclas `#catalog`, `#tech`, `#activities`).
- Todo el estado de carrito se persiste en `localStorage` (clave `summitlab.cart.v1`).

---

## 2. Estructura del código

```
src/
├── main.jsx                          # Providers: ToastProvider + CartProvider
├── App.jsx                           # Página principal (catálogo, filtros, tech, banner)
├── index.css                         # Tema, colores, tipografías y TODAS las animaciones
├── context/
│   └── CartContext.jsx               # Estado del carrito + persistencia
├── services/
│   └── api.js                        # Capa de API lista para el backend (mocks hoy)
├── lib/
│   └── placeholderImage.js           # Imagen de respaldo SVG + manejo de error
├── data/
│   └── mockProducts.js               # Productos de prueba, actividades, filtros
└── components/
    ├── layout/   (NavbarTech, FooterAlpine)
    ├── home/     (HeroMountain, ActivityFilter)
    ├── catalog/  (ProductCard, TechFilterDrawer)
    ├── cart/     (CartDrawer)
    └── ui/       (Toast, Reveal)
```

### Animaciones (todo en `src/index.css`)
- `animate-fade-up`, `animate-fade-in`, `animate-scale-in`, `animate-pop`
- `animate-aurora`, `animate-kenburns`, `animate-drift` (partículas de nieve)
- `animate-marquee`, `animate-spin-slow`, `animate-gradient-x`
- `animate-slide-down`, `animate-toast-in`, `animate-cart-pop`
- Scroll-reveal: componente `<Reveal>` (`src/components/ui/Reveal.jsx`) con
  IntersectionObserver + delays escalonados.
- Todas respetan `prefers-reduced-motion`.

---

## 3. Conectar el backend (lo que hay que hacer después)

Hoy todo funciona con datos simulados. La integración real está preparada en
`src/services/api.js`:

1. Crear `.env` copiando `.env.example`:
   ```
   VITE_API_BASE_URL=https://TU-APIGATEWAY.execute-api.REGION.amazonaws.com/prod
   VITE_USE_MOCK=false
   ```
2. Al poner `VITE_USE_MOCK=false`:
   - `productService.list()` hará GET a `${API_BASE_URL}/products`
   - `cartService.create(payload)` hará POST a `${API_BASE_URL}/orders`
3. Los 401/403 del API Gateway ya se manejan: el checkout muestra el toast de
   "Necesitas iniciar sesión".

### Pendiente por componente
| Componente | Proveedor | Ruta esperada | Estado |
|---|---|---|---|
| Catálogo de productos | Microservicio Productos (AWS) | `/products` | Conectar |
| Pedidos / carrito | Microservicio Carrito (AWS) | `/orders` (POST) | Conectar |
| Login/Logout | Azure AD (MSAL) | OAuth 2.0 / OIDC + PKCE | Pendiente |

---

## 4. Siguiente etapa: Azure AD + MSAL (aún NO implementado)

- Instalar `@azure/msal-browser` y `@azure/msal-react`.
- Configurar `PublicClientApplication` con `auth.flowRedirectStartInBackground` etc.
- App Registration: URIs de redirección, scopes y roles requeridos.
- Adjuntar el token JWT en cada petición hacia la API (interceptor / wrapper de `fetch`
  en `src/services/api.js`).
- Los botones de la UI ya tienen el placeholder:
  - Navbar: botón **"Ingresar"** (`handleAuth`).
  - Banner: botón **"Crear cuenta"**.

## 5. AWS API Gateway + microservicios (BACKEND LISTO — queda desplegar en AWS)

Los **microservicios ya están implementados y probados** en este repo (ver §8).
Lo que sigue es infraestructura (Azure + AWS), no código:

- **Productos**: `servicio-productos` expone `/products` (listado + detalle). ✅
- **Carrito/Órdenes**: `servicio-pedidos` protege `/orders` con JWT + scopes/roles. ✅
- **JWT Authorizer** en el API Gateway (firma/vigencia/issuer/audience contra los
  JWKS de Azure, 401/403) → se configura en AWS al crear el Gateway.
- CORS restringido al dominio del frontend → se configura en el Gateway (los
  microservicios ya traen CORS por origen para desarrollo).

---

## 6. Imágenes y precios (último cambio)

- Verificadas todas las URLs con HEAD (200 OK). Se reemplazaron dos que devolvían
  404 en `src/data/mockProducts.js`:
  - `Ridge Thermal Down` → `photo-1544022613-e87ca75a784a`
  - `StormBreaker GTX` → `photo-1594633312681-425c7b97ccd1`
- Hero y banner de montaña verificados (200 OK).
- Respaldo automático: si una imagen (incluidas las del backend futuro) no carga,
  `onError` la cambia por un placeholder SVG (`src/lib/placeholderImage.js`).
- **Precios en pesos chilenos (CLP)**: `Intl.NumberFormat('es-CL', { style: 'currency',
  currency: 'CLP' })` en `ProductCard.jsx` y `CartDrawer.jsx`.
  - Nota: hoy el número del precio se muestra tal cual (ej. 899 → `$899`).
    Si se quiere el valor real en CLP hay que multiplicar por el tipo de cambio al
    cargar el catálogo.

---

## 7. Errores conocidos / mejoras pendientes

- El botón de **buscar** del navbar es placeholder (muestra un toast).
- El **login** es placeholder hasta integrar MSAL.
- No hay página de detalle de producto ni vista 404 (rutas con anclas únicamente).
- No hay test runners configurados (solo lint + build).
- Si se agrega React Router, migrar las anclas `#catalog/#tech/#activities` a rutas.

---

## 8. Estado del Backend (completado y verificado)

Backend **Maven multi-módulo** con 4 microservicios. Código en **español**, JSON en inglés.

| Microservicio | Rutas | Puerto | Seguridad |
|---|---|---|---|
| `servicio-productos` | `GET /products`, `GET /products/{id}` | 8081 | Pública |
| `servicio-pedidos` | `POST /orders`, `GET /orders`, `GET /orders/{id}` | 8082 | JWT local HS256 + Azure JWKS |
| `servicio-usuarios` | `POST /auth/registro`, `POST /auth/ingreso`, `GET /users` | 8083 | Pública (emite JWT) |
| `servicio-gateway` | **Entry point único** — enruta todo a los MS internos | 9000 | Propaga Authorization |

Estructura:

```
servicio-productos/
  └── com.mountainbackend.productos
      ├── ServicioProductosApplication
      ├── modelo/Producto
      ├── repositorio/RepositorioProductos  (8 productos semilla)
      ├── controlador/ControladorProductos
      └── configuracion/FiltroCors
servicio-pedidos/
  └── com.mountainbackend.pedidos
      ├── ServicioPedidosApplication
      ├── modelo/Pedido, LineaPedido, TotalesPedido
      ├── dto/SolicitudPedido
      ├── servicio/ServicioPedidos
      ├── controlador/ControladorPedidos
      ├── seguridad/FiltroValidacionJwt  (local HS256 + Azure JWKS)
      └── configuracion/FiltroCors, PropiedadesSeguridad
servicio-usuarios/
  └── com.mountainbackend.usuarios
      ├── ServicioUsuariosApplication
      ├── modelo/Usuario
      ├── dto/SolicitudRegistro, SolicitudIngreso, RespuestaToken
      ├── repositorio/RepositorioUsuarios
      ├── servicio/ServicioUsuarios  (PBKDF2, seed demo@summitlab.cl)
      ├── controlador/ControladorAutenticacion, ControladorUsuarios, ControladorRegistroApi
      ├── seguridad/EmisorJwt, PropiedadesAutenticacion, RegistroEndpoints
      └── configuracion/FiltroCors
servicio-gateway/
  └── com.mountainbackend.gateway
      ├── ServicioGatewayApplication
      ├── proxy/FiltroProxyApi  (RestClient: /auth → usuarios, /products → productos, /orders → pedidos)
      ├── controlador/ControladorRegistroApi  (consolida /api/endpoints de todos los MS)
      └── configuracion/FiltroCors, PropiedadesGateway
```

- **JWT local**: `servicio-usuarios` firma HS256 con `app.auth.secreto`; `servicio-pedidos` valida con `app.seguridad.local-secreto`.
- **JWT Azure** (opcional): con `JWT_LOCAL_ENABLED=false` + `JWT_ENABLED=true`, valida JWKS RS256.
- **Registro de endpoints**: `GET /api/endpoints` en cada servicio; el gateway consolida todos.
- **Health checks**: `/actuator/health` en todos los servicios.

Verificación realizada:
- `mvn clean package` → **BUILD SUCCESS**, 17 tests (productos 4, pedidos 4, usuarios 6, gateway 3).
- `GET /products` → 200 con 8 productos.
- `POST /auth/registro` → 201 con JWT + datos usuario.
- `POST /auth/ingreso` → 201 con JWT.
- `POST /orders` con Bearer token → 201 con pedido + email del JWT.
- `POST /orders` sin token → **401**.
- `GET /api/endpoints` (gateway) → JSON con inventario consolidado de los 3 MS.

## 9. Cómo correr el backend

Requisitos: JDK 17+ (se probó con Java 25) y Maven (wrapper `mvnw` incluido).

```bash
mvn clean package
```

Luego, opción A (directo) o B (Docker Compose):

```bash
# A) 4 procesos (el gateway es el entrypoint del frontend)
java -jar servicio-productos/target/servicio-productos-0.0.1-SNAPSHOT.jar   # :8081
java -jar servicio-pedidos/target/servicio-pedidos-0.0.1-SNAPSHOT.jar       # :8082
java -jar servicio-usuarios/target/servicio-usuarios-0.0.1-SNAPSHOT.jar     # :8083
java -jar servicio-gateway/target/servicio-gateway-0.0.1-SNAPSHOT.jar       # :9000

# B) Docker Compose (requiere haber empaquetado antes)
docker compose up --build
```

Prueba rápida (todo vía el gateway en :9000):

```bash
# Catálogo
curl http://localhost:9000/products

# Login con usuario demo
curl -X POST http://localhost:9000/auth/ingreso \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@summitlab.cl","password":"demo1234"}'

# Pedido con token (reemplaza TOKEN con el del login)
curl -X POST http://localhost:9000/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"items":[{"productId":1,"name":"Alpha SV Jacket","price":899,"quantity":2}]}'

# Registro de endpoints
curl http://localhost:9000/api/endpoints
```

> OJO con Windows PowerShell: escapar el JSON (o usar un archivo con `--data-binary @archivo.json`).

## 10. Conexión con Azure (opcional, producción)

El `servicio-pedidos` soporta validación JWKS de Azure como **doble capa** (además del JWT local).

1. Crear **App Registration** en Azure AD / Entra ID:
   - Redirect URI = la del frontend (`http://localhost:5173`).
   - Scope `orders.write`, App Roles opcionales.
2. En `servicio-pedidos` setear:
   ```
   JWT_LOCAL_ENABLED=false
   JWT_ENABLED=true
   JWT_JWKS_URI=https://login.microsoftonline.com/{TENANT}/discovery/v2.0/keys
   JWT_ISSUER_URI=https://login.microsoftonline.com/{TENANT}/v2.0
   JWT_AUDIENCES=api://{CLIENT-ID}
   JWT_REQUIRED_SCOPES=orders.write
   ```
3. Validaciones: firma RS256 contra JWKS, iss, aud, scopes/roles → 401/403.

## 11. Conexión con AWS (producción)

En producción el `servicio-gateway` se reemplaza por **AWS API Gateway** (o se despliega como sidecar).

1. Build de imágenes:
   ```bash
   docker build -t pedidos360/servicio-productos:latest servicio-productos/
   docker build -t pedidos360/servicio-pedidos:latest servicio-pedidos/
   docker build -t pedidos360/servicio-usuarios:latest servicio-usuarios/
   docker build -t pedidos360/servicio-gateway:latest servicio-gateway/
   ```
2. Subir a **ECR**, crear **Task Definitions** en **ECS** (Fargate):
   - `SERVER_PORT=8080`, health check `GET /actuator/health`.
3. **API Gateway** en AWS enruta: `/products` → productos, `/auth/*` → usuarios,
   `/orders` → pedidos (con JWT Authorizer si se usa Azure).
4. Frontend `.env`:
   ```
   VITE_API_BASE_URL=https://TU-APIGATEWAY.execute-api.REGION.amazonaws.com/prod
   VITE_USE_MOCK=false
   ```

## 12. Checklist de entregables

- [x] Backend 4 microservicios: productos, pedidos, usuarios, gateway — BUILD SUCCESS, 17 tests.
- [x] Gateway como entrypoint único: enruta `/auth/*`, `/products`, `/orders`, `/api/endpoints`.
- [x] JWT local HS256: usuarios firma, pedidos valida.
- [x] Registro de endpoints: `GET /api/endpoints` consolidado en gateway.
- [x] Frontend: `authService.register/login/logout` + Bearer token automático.
- [x] Botones "Ingresar" y "Crear cuenta" abren AuthModal (login/registro).
- [x] Checkout sin token muestra toast + abre modal de login.
- [ ] Azure: App Registration + JWT JWKS (solo si se quiere producción con Azure AD).
- [ ] AWS: ECR + ECS + API Gateway (reemplaza `servicio-gateway` en deploy).