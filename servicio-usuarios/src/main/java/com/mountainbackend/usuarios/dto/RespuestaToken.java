package com.mountainbackend.usuarios.dto;

import com.mountainbackend.usuarios.modelo.Usuario;

/**
 * Respuesta de login/registro: el token JWT que el frontend adjuntará como
 * "Authorization: Bearer ..." en las llamadas protegidas (/orders).
 */
public record RespuestaToken(
		String token,
		String tokenType,
		long expiresIn,
		UsuarioDto user) {

	public static RespuestaToken crear(String token, long expiraEnSegundos, Usuario usuario) {
		return new RespuestaToken(token, "Bearer", expiraEnSegundos, UsuarioDto.desde(usuario));
	}
}