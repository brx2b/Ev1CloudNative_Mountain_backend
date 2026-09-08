package com.mountainbackend.usuarios.dto;

import java.util.List;

import com.mountainbackend.usuarios.modelo.Usuario;

/**
 * Representación pública de un usuario (nunca expone el hash de la contraseña).
 * Contrato JSON que consume el frontend: id, name, email, roles.
 */
public record UsuarioDto(
		long id,
		String name,
		String email,
		List<String> roles) {

	public static UsuarioDto desde(Usuario usuario) {
		return new UsuarioDto(usuario.id(), usuario.name(), usuario.email(), List.of(usuario.rol()));
	}
}