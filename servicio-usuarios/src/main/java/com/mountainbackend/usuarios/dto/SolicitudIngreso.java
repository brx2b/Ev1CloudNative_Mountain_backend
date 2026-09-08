package com.mountainbackend.usuarios.dto;

/**
 * Payload de POST /auth/ingreso (login).
 */
public record SolicitudIngreso(
		String email,
		String password) {

}