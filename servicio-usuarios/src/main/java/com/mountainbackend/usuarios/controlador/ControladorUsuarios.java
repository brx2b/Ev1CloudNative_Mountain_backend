package com.mountainbackend.usuarios.controlador;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mountainbackend.usuarios.dto.UsuarioDto;
import com.mountainbackend.usuarios.servicio.ServicioUsuarios;

/**
 * Consulta pública de usuarios (demo).
 * GET /users       -> lista usuarios / GET /users/{id} -> detalle unitario.
 */
@RestController
@RequestMapping("/users")
public class ControladorUsuarios {

	private final ServicioUsuarios servicioUsuarios;

	public ControladorUsuarios(ServicioUsuarios servicioUsuarios) {
		this.servicioUsuarios = servicioUsuarios;
	}

	@GetMapping
	public List<UsuarioDto> listar() {
		return servicioUsuarios.listarTodos();
	}

	@GetMapping("/{id}")
	public ResponseEntity<UsuarioDto> obtenerPorId(@PathVariable("id") long id) {
		return servicioUsuarios.buscarPorId(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}
}