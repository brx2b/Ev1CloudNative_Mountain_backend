# SummitLab - Backend Cloud-Native

Backend de la tienda de equipamiento de montaña SummitLab. Es un proyecto Maven multi-módulo con tres microservicios Spring Boot. El gateway es **AWS API Gateway** (el antiguo `servicio-gateway` Spring se eliminó).

## Propósito y flujo

El frontend solo llama a AWS API Gateway, que enruta a cada microservicio:

```text
Frontend -> AWS API Gateway -> productos:8080 (/products)
                             -> usuarios:8080  (/auth/*)
                             -> pedidos:8080   (/orders)
```

| Servicio             | Función            | Puerto local | Rutas principales                           |
| -------------------- | ------------------ | -----------: | ------------------------------------------- |
| `servicio-productos` | Catálogo           |         8081 | `GET /products`, `GET /products/{id}`       |
| `servicio-pedidos`   | Pedidos            |         8082 | `POST/GET /orders`                          |
| `servicio-usuarios`  | Registro e ingreso |         8083 | `POST /auth/registro`, `POST /auth/ingreso` |

## Dónde colocar las URLs de los microservicios

Ya no hay gateway Spring que configurar: cada microservicio es autónomo y
escucha en el puerto `8080` de su contenedor (publicados como `8081/8082/8083`
en `docker-compose.yml` para desarrollo local).

### EC2 con Docker Compose

Si los tres contenedores corren en la misma EC2, AWS API Gateway los alcanza
por integración HTTP directa a `http://IP-O-DNS-EC2:8081|8082|8083` según la
ruta (`/products` → 8081, `/auth/*` → 8083, `/orders` → 8082).

Mientras no exista el API Gateway, el entrypoint es el nginx del EC2 frontend
(misma VPC), que rutea `/api/*` directo a cada puerto (`8081/8082/8083`).
En el SG del backend solo se abre `8081-8083` desde el SG del frontend.

En una arquitectura distribuida (uno o más EC2, ECS o ALB), cada ruta del API
Gateway apunta a su destino correspondiente (DNS privados, Cloud Map o load
balancers internos). No uses una IP pública para comunicación entre
microservicios si están en la misma VPC.

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

URLs locales (cada servicio directo, sin gateway):

```text
Productos: http://localhost:8081
Pedidos:   http://localhost:8082
Usuarios:  http://localhost:8083
```

Pruebas rápidas por servicio:

```bash
curl http://localhost:8081/products
curl -X POST http://localhost:8083/auth/ingreso \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@summitlab.cl","password":"demo1234"}'
```

## Despliegue en una EC2

1. Crear una instancia EC2 con Docker instalado y asignar un Security Group.
2. Permitir SSH solo desde la IP administrativa.
3. Permitir `8081`, `8082` y `8083` desde Internet (los necesita AWS API
   Gateway por integración HTTP) o, mejor, desde la VPC si usas VPC Link.
4. Clonar el repositorio y configurar secretos mediante variables de entorno.
5. Construir y levantar los servicios:

```bash
./mvnw clean package -DskipTests
docker compose up -d --build
docker compose ps
docker compose logs -f servicio-productos
```

### Memoria (una sola EC2 basta)

No necesitas otro EC2: los 3 microservicios caben en una sola instancia.
Cada JVM se limita con `JAVA_OPTS` (default en `docker-compose.yml`:
`-Xms128m -Xmx384m`, sobrescribible con la variable `JAVA_OPTS`):

| Instancia   | RAM  | Veredicto                                              |
| ----------- | ---- | ------------------------------------------------------ |
| `t3.small`  | 2 GB | Suficiente para demo/Ev1 (~1.2-1.5 GB los 3 MS + SO).  |
| `t3.medium` | 4 GB | Recomendada para uso real o picos de tráfico.          |
| 3 × `t3.small` | 2 GB c/u | Solo si quieres aislamiento total por servicio (3× costo). |

(Ojo: la familia `m3` no tiene tamaño "small" — el mínimo es `m3.medium`,
generación 2013. Para este proyecto usa `t3.small`/`t3.medium`, más baratos
y modernos. Alternativa sin administrar EC2: ECS Fargate, 0.25 vCPU + 0.5 GB
por tarea.)

Cambia `CORS_ALLOWED_ORIGINS` por el dominio real del frontend. El frontend
usa únicamente la URL de AWS API Gateway:

```env
VITE_API_BASE_URL=https://tu-api-gateway.execute-api.REGION.amazonaws.com/prod
VITE_USE_MOCK=false
```

## AWS API Gateway

API Gateway es el endpoint público, control de CORS, throttling y punto donde se aplica el JWT Authorizer de Entra ID. Debe enrutar:

| Ruta pública                   | Destino              | Integración HTTP (1 EC2)   |
| ------------------------------ | -------------------- | -------------------------- |
| `/products`                    | `servicio-productos` | `http://EC2:8081/products` |
| `/auth/{proxy+}`               | `servicio-usuarios`  | `http://EC2:8083/auth/{proxy}` |
| `/orders` y `/orders/{proxy+}` | `servicio-pedidos`   | `http://EC2:8082/orders...` |

Los destinos pueden ser una EC2 mediante integración HTTP, un Application Load Balancer interno o servicios ECS. El frontend solo recibe la URL de API Gateway:

```env
VITE_API_BASE_URL=https://API_ID.execute-api.REGION.amazonaws.com/prod
```

No coloques las URLs privadas de los microservicios en el `.env` del frontend.

## Salud y observabilidad

Cada servicio expone `GET /actuator/health`; `servicio-usuarios` además expone
`GET /api/endpoints` (inventario de sus rutas, accesible por el nginx como
`/api/usuarios/endpoints`).

## Notas de producción

- Los repositorios actuales viven en memoria; para persistencia usar DynamoDB, RDS u otra base de datos administrada.
- Cambiar todos los secretos de ejemplo antes de exponer la EC2.
- Configurar HTTPS: el tráfico navegador → API Gateway ya va por HTTPS; entre
  API Gateway y la EC2 usa VPC Link o restringe el Security Group.
- El frontend nunca llama a los microservicios directo: todo pasa por API Gateway.
