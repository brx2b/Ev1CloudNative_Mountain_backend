package com.mountainbackend.usuarios.controlador;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mountainbackend.usuarios.dto.RespuestaToken;
import com.mountainbackend.usuarios.dto.SolicitudIngreso;
import com.mountainbackend.usuarios.dto.SolicitudRegistro;
import com.mountainbackend.usuarios.servicio.ServicioUsuarios;

/**
 * Registro e ingreso de usuarios. Público (no requiere token).
 * POST /auth/registro -> crea la cuenta y devuelve el JWT.
 * POST /auth/ingreso   -> valida credenciales y devuelve el JWT.
 */
@RestController
@RequestMapping("/auth")
public class ControladorAutenticacion {

	private final ServicioUsuarios servicioUsuarios;

	public ControladorAutenticacion(ServicioUsuarios servicioUsuarios) {
		this.servicioUsuarios = servicioUsuarios;
	}

	@PostMapping("/registro")
	public ResponseEntity<RespuestaToken> registrar(@RequestBody SolicitudRegistro solicitud) {
		RespuestaToken respuesta = servicioUsuarios.registrar(solicitud);
		return ResponseEntity.status(HttpStatus.CREATED)
			.location(URI.create("/users/" + respuesta.user().id()))
			.body(respuesta);
	}

	@PostMapping("/ingreso")
	public ResponseEntity<RespuestaToken> ingresar(@RequestBody SolicitudIngreso solicitud) {
		return ResponseEntity.ok(servicioUsuarios.ingresar(solicitud));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> alErrorDeArgumento(IllegalArgumentException ex) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("status", 400);
		cuerpo.put("error", ex.getMessage());
		return ResponseEntity.badRequest().body(cuerpo);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, Object>> alCredencialesInvalidas(IllegalStateException ex) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("status", 401);
		cuerpo.put("error", ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(cuerpo);
	}
}