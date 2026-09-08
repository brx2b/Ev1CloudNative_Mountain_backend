package com.mountainbackend.gateway.controlador;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro de endpoints del backend completo (PEDIDOS360).
 * GET /api/endpoints -> inventario de TODOS los microservicios: método, ruta,
 * servicio destino y si requiere autenticación.
 */
@RestController
@RequestMapping("/api")
public class ControladorRegistroApi {

	@GetMapping("/endpoints")
	public Map<String, Object> registroEndpoints() {
		return Map.of(
			"servicio", "servicio-gateway",
			"puerto", System.getenv("SERVER_PORT") != null ? System.getenv("SERVER_PORT") : "9000",
			"descripcion", "PEDIDOS360 - API Gateway (entrypoint único del frontend)",
			"servicios", List.of(
				Map.of(
					"servicio", "servicio-usuarios",
					"prefijo", "/auth",
					"endpoints", List.of(
						Map.of("method", "POST", "path", "/auth/registro", "descripcion", "Crea cuenta y entrega JWT", "secured", false),
						Map.of("method", "POST", "path", "/auth/ingreso", "descripcion", "Login con email y clave, entrega JWT", "secured", false),
						Map.of("method", "GET", "path", "/users", "descripcion", "Lista usuarios (demo)", "secured", false),
						Map.of("method", "GET", "path", "/users/{id}", "descripcion", "Detalle de un usuario", "secured", false))),
				Map.of(
					"servicio", "servicio-productos",
					"prefijo", "/",
					"endpoints", List.of(
						Map.of("method", "GET", "path", "/products", "descripcion", "Catálogo de productos", "secured", false),
						Map.of("method", "GET", "path", "/products/{id}", "descripcion", "Detalle de un producto", "secured", false))),
				Map.of(
					"servicio", "servicio-pedidos",
					"prefijo", "/",
					"endpoints", List.of(
						Map.of("method", "POST", "path", "/orders", "descripcion", "Crea un pedido desde el carrito (JWT)", "secured", true),
						Map.of("method", "GET", "path", "/orders", "descripcion", "Lista pedidos (JWT)", "secured", true),
						Map.of("method", "GET", "path", "/orders/{id}", "descripcion", "Detalle de un pedido (JWT)", "secured", true)))),
			"gateway", List.of(
				Map.of("method", "GET", "path", "/api/endpoints", "descripcion", "Este registro de endpoints", "secured", false),
				Map.of("method", "GET", "path", "/actuator/health", "descripcion", "Health check del gateway", "secured", false)));
	}
}