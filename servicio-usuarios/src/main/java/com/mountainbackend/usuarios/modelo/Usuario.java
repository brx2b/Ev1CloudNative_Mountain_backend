package com.mountainbackend.usuarios.modelo;

/**
 * Usuario registrado en la tienda SummitLab.
 * La contraseña nunca se almacena en claro: solo vive el hash PBKDF2.
 * Este modelo es interno; para exponerlo al frontend se usa UsuarioDto
 * (id, name, email, roles), que nunca incluye el hash.
 */
public record Usuario(
		long id,
		String name,
		String email,
		String passwordHash,
		String rol) {
	// sin métodos: la conversión a UsuarioDto vive en UsuarioDto.desde(...).
}