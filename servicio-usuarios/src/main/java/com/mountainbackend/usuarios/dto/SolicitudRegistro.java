package com.mountainbackend.usuarios.dto;

/**
 * Payload de POST /auth/registro.
 * El frontend envía name, email y password (los campos del formulario).
 */
public record SolicitudRegistro(
		String name,
		String email,
		String password) {

}