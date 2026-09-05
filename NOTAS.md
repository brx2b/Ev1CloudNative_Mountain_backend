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

Backend reestructurado a **Maven multi-módulo** con los dos microservicios listos
para conectarse con Azure y AWS. El código (paquetes, clases, variables) está en
**español**; los **campos JSON se mantienen en inglés** porque son el contrato que
consume `src/services/api.js` y `mockProducts.js`.

| Microservicio | Rutas | Puerto local | Seguridad |
|---|---|---|---|
| `servicio-productos` | `GET /products`, `GET /products/{id}` | 8081 | Pública (catálogo) |
| `servicio-pedidos` | `POST /orders`, `GET /orders`, `GET /orders/{id}` | 8082 | JWT Azure + scope `orders.write` |

Estructura:

```
servicio-productos/
  └── com.mountainbackend.productos
      ├── ServicioProductosApplication
      ├── modelo/Producto          (record igual que mockProducts.js)
      ├── repositorio/RepositorioProductos  (8 productos semilla, en memoria)
      ├── controlador/ControladorProductos
      └── configuracion/FiltroCors
servicio-pedidos/
  └── com.mountainbackend.pedidos
      ├── ServicioPedidosApplication
      ├── modelo/Pedido, LineaPedido, TotalesPedido
      ├── dto/SolicitudPedido
      ├── servicio/ServicioPedidos (totales recalculados en servidor)
      ├── controlador/ControladorPedidos
      ├── seguridad/FiltroValidacionJwt  (JWKS Azure: firma, exp/nbf, iss, aud, scopes/roles)
      └── configuracion/FiltroCors, PropiedadesSeguridad
```

- **CORS** por origen configurable: `app.cors.origenes-permitidos` / env `CORS_ALLOWED_ORIGINS`
  (default `http://localhost:5173`), con `Allow-Credentials` (el front usa `credentials:'include'`).
- **Health checks**: `/actuator/health` en ambos (para el load balancer / ECS).
- **JWT desactivable**: `JWT_ENABLED=false` → modo demo sin token (para probar contra el front
  con `VITE_USE_MOCK=false`); `true` → exige token válido + scope `orders.write` (o rol).

Verificación ya realizada:
- `mvn clean package` → **BUILD SUCCESS**, 8/8 tests.
- `GET /products` → 200 con 8 productos (mismo shape de `mockProducts.js`).
- `POST /orders` → 201 con `{ "id": "PEDIDO-100000", "totals": { count, subtotal } }`.
- CORS: responde solo a `http://localhost:5173`; orígenes ajenos sin header.
- JWT activo: sin token / token basura / token falso → **401**; ruta protegida sin token → 401.

## 9. Cómo correr el backend

Requisitos: JDK 17+ (se probó con Java 25) y Maven (wrapper `mvnw` incluido).

```bash
mvn clean package
```

Luego, opción A (directo) o B (Docker Compose):

```bash
# A) dos procesos
java -jar servicio-productos/target/servicio-productos-0.0.1-SNAPSHOT.jar   # :8081
java -jar servicio-pedidos/target/servicio-pedidos-0.0.1-SNAPSHOT.jar       # :8082

# B) Docker Compose (requiere haber empaquetado antes)
docker compose up --build
```

Prueba rápida en modo demo:

```bash
curl http://localhost:8081/products
curl -X POST http://localhost:8082/orders \
  -H "Content-Type: application/json" \
  -d '{"items":[{"id":1,"name":"Alpha SV Jacket","price":899,"quantity":2}]}'
```

> OJO con Windows PowerShell: escapar el JSON (o usar un archivo con `--data-binary @archivo.json`).

## 10. Conexión con Azure (pasos para el compañero)

1. Crear **App Registration** en Azure AD / Entra ID:
   - Redirect URI (SPA) = la del frontend (p.ej. `http://localhost:5173`).
   - Exponer una API con scope (p.ej. `orders.write`) y opcionalmente App Roles
     (p.ej. `Orders.Write`) para "validación estricta de roles y scopes".
2. Anotar: **Tenant ID**, **Client ID**, **Scopes/Roles** creados.
3. En el deploy de `servicio-pedidos` setear (env vars o en `docker-compose.yml`):
   ```
   JWT_ENABLED=true
   JWT_JWKS_URI=https://login.microsoftonline.com/{TU-TENANT-ID}/discovery/v2.0/keys
   JWT_ISSUER_URI=https://login.microsoftonline.com/{TU-TENANT-ID}/v2.0
   JWT_AUDIENCES=api://{TU-CLIENT-ID}
   JWT_REQUIRED_SCOPES=orders.write
   JWT_REQUIRED_ROLES=Orders.Write            # opcional
   ```
   (Mapeo interno en `application.properties`: `app.seguridad.*`.)
4. Probar con un token real del login del frontend:
   - Sin header `Authorization` → **401**.
   - Con token que NO trae `orders.write` → **403**.
   - Con token válido → **201**.

## 11. Conexión con AWS (pasos para el compañero)

1. Build de imágenes:
   ```bash
   docker build -t pedidos360/servicio-productos:latest servicio-productos/
   docker build -t pedidos360/servicio-pedidos:latest servicio-pedidos/
   ```
2. Subirlas a **ECR** y crear **Task Definitions** en **ECS** (Fargate):
   - `SERVER_PORT=8080`, health check `GET /actuator/health`, y las variables de la §10
     para `servicio-pedidos`.
3. **API Gateway** como único entrypoint:
   - `GET /products` (y `/{id}`) → `servicio-productos` (público).
   - `POST /orders`, `GET /orders` → `servicio-pedidos` con **JWT Authorizer**
     (JWKS de Azure): valida firma/vigencia/issuer/audience → 401/403.
   - Configurar **CORS** restringido al dominio real del frontend.
4. En el frontend, `.env`:
   ```
   VITE_API_BASE_URL=https://TU-APIGATEWAY.execute-api.REGION.amazonaws.com/prod
   VITE_USE_MOCK=false
   ```

## 12. Checklist de entregables

- [ ] Frontend: login/logout con MSAL (ya tiene placeholders en Navbar/Banner).
- [ ] MSAL configurado con los datos de Azure (Client ID, Tenant, redirect, scopes).
- [ ] `src/services/api.js` adjuntando el token JWT en `Authorization: Bearer …`.
- [ ] Microservicios desplegados en AWS (ECR + ECS) ✔ listos para subir.
- [ ] API Gateway con JWT Authorizer (Azure JWKS) + CORS al dominio del front.
- [ ] Evidencias: login con JWT obtenido de Azure; en el Gateway 401/403 sin token
      y 200 con token autorizado; `GET /products` y `POST /orders` funcionando.
- [ ] Repos de GitHub separados por componente, con `.gitignore` (el backend ya tiene).