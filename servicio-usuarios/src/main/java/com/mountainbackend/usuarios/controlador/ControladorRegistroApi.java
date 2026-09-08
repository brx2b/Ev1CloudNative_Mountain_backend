package com.mountainbackend.usuarios.controlador;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mountainbackend.usuarios.configuracion.RegistroEndpoints;

/**
 * Registro de endpoints del microservicio.
 * GET /api/endpoints -> inventario de rutas reales (método + path + handler).
 */
@RestController
@RequestMapping("/api")
public class ControladorRegistroApi {

	private final RegistroEndpoints registroEndpoints;

	public ControladorRegistroApi(RegistroEndpoints registroEndpoints) {
		this.registroEndpoints = registroEndpoints;
	}

	@GetMapping("/endpoints")
	@SuppressWarnings("unchecked")
	public Map<String, Object> registroEndpoints() {
		String puerto = System.getenv("SERVER_PORT") != null ? System.getenv("SERVER_PORT") : "8083";
		return Map.of(
			"servicio", "servicio-usuarios",
			"puerto", puerto,
			"endpoints", (List<Map<String, Object>>) registroEndpoints.listarEndpoints());
	}
}