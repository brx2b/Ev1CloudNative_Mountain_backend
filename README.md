# SummitLab - Backend Cloud-Native

Backend de la tienda de equipamiento de montaña SummitLab. Es un proyecto Maven multi-módulo con cuatro servicios Spring Boot y un gateway HTTP.

## Propósito y flujo

El frontend solo debe llamar al gateway. Las URLs internas se configuran en el gateway mediante variables de entorno:

```text
Frontend -> gateway:9000 -> productos:8080
                         -> usuarios:8080
                         -> pedidos:8080
```

En producción, AWS API Gateway puede ser el punto público y reemplazar al gateway Spring, pero el frontend sigue usando un único endpoint público.

| Servicio             | Función            | Puerto local | Rutas principales                           |
| -------------------- | ------------------ | -----------: | ------------------------------------------- |
| `servicio-productos` | Catálogo           |         8081 | `GET /products`, `GET /products/{id}`       |
| `servicio-pedidos`   | Pedidos            |         8082 | `POST/GET /orders`                          |
| `servicio-usuarios`  | Registro e ingreso |         8083 | `POST /auth/registro`, `POST /auth/ingreso` |
| `servicio-gateway`   | Enrutamiento único |         9000 | `/products`, `/auth/*`, `/orders`           |

## Dónde colocar las URLs de los microservicios

### Docker Compose

Las URLs internas están en `docker-compose.yml`, dentro de `servicio-gateway`:

```yaml
environment:
  AUTH_SERVICE_URL: "http://servicio-usuarios:8080"
  PRODUCTOS_SERVICE_URL: "http://servicio-productos:8080"
  PEDIDOS_SERVICE_URL: "http://servicio-pedidos:8080"
```

Estos nombres funcionan porque Compose crea una red interna. No los cambies por `localhost` dentro de los contenedores: `localhost` apuntaría al propio gateway.

### Ejecución directa con JARs

Los valores por defecto están en `servicio-gateway/src/main/resources/application.properties`:

```properties
app.servicios.auth-url=${AUTH_SERVICE_URL:http://localhost:8083}
app.servicios.productos-url=${PRODUCTOS_SERVICE_URL:http://localhost:8081}
app.servicios.pedidos-url=${PEDIDOS_SERVICE_URL:http://localhost:8082}
```

También pueden sobrescribirse al iniciar el gateway:

```powershell
$env:AUTH_SERVICE_URL="http://127.0.0.1:8083"
$env:PRODUCTOS_SERVICE_URL="http://127.0.0.1:8081"
$env:PEDIDOS_SERVICE_URL="http://127.0.0.1:8082"
java -jar servicio-gateway/target/servicio-gateway-0.0.1-SNAPSHOT.jar
```

### EC2 con Docker Compose

Si los cuatro contenedores corren en la misma EC2, conserva las URLs internas de Compose. Solo publica el gateway:

```yaml
ports:
  - "9000:8080"
```

Los puertos `8081`, `8082` y `8083` pueden quitarse de `ports` para que no sean accesibles desde Internet. El frontend usará únicamente `http://DNS-PUBLICO-EC2:9000`.

En una arquitectura distribuida, sustituye las variables del gateway por DNS privados, IPs privadas, Cloud Map o load balancers internos. No uses una IP pública para comunicación entre microservicios si están en la misma VPC.

## Autenticación y propósito del token

### Flujo local actual

`servicio-usuarios` recibe registro/login y emite un JWT HS256. El frontend lo envía en cada llamada protegida:

```http
Authorization: Bearer eyJ...
```

`servicio-pedidos` valida la firma con el mismo secreto compartido. Este modo es útil para desarrollo o una instalación demo.

Variables relacionadas:

```env
JWT_SECRET=secreto-largo-y-aleatorio
JWT_EMISOR=pedidos360-usuarios
JWT_AUDIENCIA=pedidos360-api
JWT_LOCAL_ENABLED=true
JWT_LOCAL_SECRET=secreto-largo-y-aleatorio
JWT_LOCAL_ISSUER=pedidos360-usuarios
JWT_LOCAL_AUDIENCE=pedidos360-api
```

El secreto debe ser idéntico en usuarios y pedidos, pero nunca debe quedar publicado en el repositorio.

### Azure Entra ID en producción

Azure Entra ID autentica al usuario y emite un access token firmado con RS256. El token permite que API Gateway y los microservicios comprueben identidad, audiencia y permisos. No es una sesión ni reemplaza la autorización del endpoint: `/orders` debe exigir el scope o rol correspondiente, por ejemplo `orders.write`.

En `servicio-pedidos` se configuran estas variables:

```env
JWT_ENABLED=true
JWT_JWKS_URI=https://login.microsoftonline.com/TENANT_ID/discovery/v2.0/keys
JWT_ISSUER_URI=https://login.microsoftonline.com/TENANT_ID/v2.0
JWT_AUDIENCES=api://API_CLIENT_ID
JWT_REQUIRED_SCOPES=orders.write
JWT_REQUIRED_ROLES=
JWT_LOCAL_ENABLED=false
```

Los nombres de las propiedades equivalentes están en `servicio-pedidos/src/main/resources/application.properties`.

En AWS API Gateway se configura un JWT Authorizer con el mismo issuer y audience. El gateway rechaza tokens inválidos antes de enviar la petición al microservicio. La validación en `servicio-pedidos` permanece como defensa en profundidad.

> Importante: el frontend actual usa el login local de `/auth/ingreso`. Para usar Entra ID de extremo a extremo hay que registrar la SPA, configurar MSAL con Authorization Code + PKCE y enviar el access token de Entra ID. No se deben mezclar tokens HS256 locales con tokens RS256 de Entra ID en el mismo modo de validación.

## Levantar localmente

Requisitos: JDK 17 o superior, Docker y Docker Compose.

```bash
./mvnw clean package
docker compose up --build
```

En Windows:

```powershell
.\mvnw.cmd clean package
docker compose up --build
```

URLs locales:

```text
Gateway:  http://localhost:9000
Productos: http://localhost:8081
Pedidos:   http://localhost:8082
Usuarios:  http://localhost:8083
```

Pruebas rápidas a través del gateway:

```bash
curl http://localhost:9000/products
curl -X POST http://localhost:9000/auth/ingreso \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@summitlab.cl","password":"demo1234"}'
```

## Despliegue en una EC2

1. Crear una instancia EC2 con Docker instalado y asignar un Security Group.
2. Permitir SSH solo desde la IP administrativa.
3. Permitir `80/443` si habrá Nginx o reverse proxy.
4. Permitir `9000` solo si el frontend accederá directamente al gateway.
5. No exponer `8081`, `8082` ni `8083` públicamente.
6. Clonar el repositorio y configurar secretos mediante variables de entorno.
7. Construir y levantar los servicios:

```bash
./mvnw clean package -DskipTests
docker compose up -d --build
docker compose ps
docker compose logs -f servicio-gateway
```

Cambia `CORS_ALLOWED_ORIGINS` por el dominio real del frontend. Si usas Nginx, publica `/api` hacia `http://127.0.0.1:9000` y configura en el frontend:

```env
VITE_API_BASE_URL=https://tu-dominio.com/api
VITE_USE_MOCK=false
```

Sin Nginx, usa directamente `http://DNS-EC2:9000` como valor base.

## AWS API Gateway

API Gateway es el endpoint público, control de CORS, throttling y punto donde puede aplicarse el JWT Authorizer de Entra ID. Debe enrutar:

| Ruta pública                   | Destino              |
| ------------------------------ | -------------------- |
| `/products`                    | `servicio-productos` |
| `/auth/{proxy+}`               | `servicio-usuarios`  |
| `/orders` y `/orders/{proxy+}` | `servicio-pedidos`   |

Los destinos pueden ser una EC2 mediante HTTP integration, un Application Load Balancer interno o servicios ECS. El frontend solo recibe la URL de API Gateway:

```env
VITE_API_BASE_URL=https://API_ID.execute-api.REGION.amazonaws.com/prod
```

No coloques las URLs privadas de los microservicios en el `.env` del frontend.

## Salud y observabilidad

Cada servicio expone `GET /actuator/health` y `GET /api/endpoints`. El gateway consolida el inventario en `GET /api/endpoints`.

## Notas de producción

- Los repositorios actuales viven en memoria; para persistencia usar DynamoDB, RDS u otra base de datos administrada.
- Cambiar todos los secretos de ejemplo antes de exponer la EC2.
- Configurar HTTPS, preferentemente mediante Application Load Balancer, Nginx o API Gateway.
- Mantener el gateway como único acceso del frontend a los microservicios.
