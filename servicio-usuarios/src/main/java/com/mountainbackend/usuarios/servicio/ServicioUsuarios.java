package com.mountainbackend.usuarios.servicio;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Service;

import com.mountainbackend.usuarios.configuracion.PropiedadesAutenticacion;
import com.mountainbackend.usuarios.dto.RespuestaToken;
import com.mountainbackend.usuarios.dto.SolicitudIngreso;
import com.mountainbackend.usuarios.dto.SolicitudRegistro;
import com.mountainbackend.usuarios.dto.UsuarioDto;
import com.mountainbackend.usuarios.modelo.Usuario;
import com.mountainbackend.usuarios.repositorio.RepositorioUsuarios;
import com.mountainbackend.usuarios.seguridad.EmisorJwt;

/**
 * Registro y login de la tienda SummitLab.
 * Las contraseñas se guardan con hash PBKDF2 (sí importa para la demo).
 * Al autenticar se emite un JWT HS256 consumible por servicio-pedidos.
 */
@Service
public class ServicioUsuarios {

	private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
	private static final int ITERACIONES = 40_000;
	private static final int LARGO_CLAVE = 256;

	private final RepositorioUsuarios repositorio;
	private final EmisorJwt emisorJwt;
	private final PropiedadesAutenticacion propiedades;

	public ServicioUsuarios(RepositorioUsuarios repositorio, EmisorJwt emisorJwt, PropiedadesAutenticacion propiedades) {
		this.repositorio = repositorio;
		this.emisorJwt = emisorJwt;
		this.propiedades = propiedades;
		sembrarUsuarioDemo();
	}

	public RespuestaToken registrar(SolicitudRegistro solicitud) {
		if (solicitud == null || isVacio(solicitud.email()) || isVacio(solicitud.password())) {
			throw new IllegalArgumentException("El registro requiere 'email' y 'password' obligatorios.");
		}
		if (!solicitud.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
			throw new IllegalArgumentException("El email no es válido.");
		}
		if (solicitud.password().length() < 6) {
			throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
		}
		String email = solicitud.email().trim().toLowerCase();
		if (repositorio.existeEmail(email)) {
			throw new IllegalArgumentException("Ya existe una cuenta con ese email.");
		}

		String nombre = solicitud.name() != null && !solicitud.name().isBlank()
			? solicitud.name().trim()
			: email.substring(0, email.indexOf('@'));

		Usuario guardado = repositorio.guardar(new Usuario(
			0,
			nombre,
			email,
			hashear(solicitud.password()),
			"CLIENTE"));
		return emitirTokens(guardado);
	}

	public RespuestaToken ingresar(SolicitudIngreso solicitud) {
		if (solicitud == null || isVacio(solicitud.email()) || isVacio(solicitud.password())) {
			throw new IllegalArgumentException("El ingreso requiere 'email' y 'password'.");
		}
		String email = solicitud.email().trim().toLowerCase();
		Optional<Usuario> encontrado = repositorio.buscarPorEmail(email);
		if (encontrado.isEmpty() || !verificarContrasena(solicitud.password(), encontrado.get().passwordHash())) {
			throw new IllegalStateException("Credenciales inválidas.");
		}
		return emitirTokens(encontrado.get());
	}

	public Optional<UsuarioDto> buscarPorId(long id) {
		return repositorio.buscarPorId(id).map(UsuarioDto::desde);
	}

	public java.util.List<UsuarioDto> listarTodos() {
		return repositorio.listarTodos().stream().map(UsuarioDto::desde).toList();
	}

	private RespuestaToken emitirTokens(Usuario usuario) {
		// vigencia del token en segundos (para expiresIn del payload)
		long expiraSegundos = propiedades.getHorasExpiracion() * 3600L;
		String token = emisorJwt.emitir(
			String.valueOf(usuario.id()),
			usuario.email(),
			usuario.name(),
			usuario.rol());
		return RespuestaToken.crear(token, expiraSegundos, usuario);
	}

	private void sembrarUsuarioDemo() {
		String email = propiedades.getUsuarioSeedEmail().trim().toLowerCase();
		if (email.isEmpty() || repositorio.existeEmail(email)) {
			return;
		}
		String password = propiedades.getUsuarioSeedPassword();
		if (isVacio(password)) {
			return;
		}
		repositorio.guardar(new Usuario(
			1L,
			"Demo SummitLab",
			email,
			hashear(password),
			"CLIENTE"));
	}

	private String hashear(String password) {
		try {
			byte[] sal = new byte[16];
			new SecureRandom().nextBytes(sal);
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sal, ITERACIONES, LARGO_CLAVE);
			byte[] hash = SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
			return ITERACIONES + ":" + Base64.getEncoder().encodeToString(sal) + ":" + Base64.getEncoder().encodeToString(hash);
		} catch (Exception ex) {
			throw new IllegalStateException("Error al hashear la contraseña", ex);
		}
	}

	private boolean verificarContrasena(String password, String almacenada) {
		try {
			String[] partes = almacenada.split(":");
			if (partes.length != 3) {
				return false;
			}
			int iteraciones = Integer.parseInt(partes[0]);
			byte[] sal = Base64.getDecoder().decode(partes[1]);
			byte[] hashEsperado = Base64.getDecoder().decode(partes[2]);
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sal, iteraciones, LARGO_CLAVE);
			byte[] hashCalculado = SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
			return MessageDigest.isEqual(hashEsperado, hashCalculado);
		} catch (Exception ex) {
			return false;
		}
	}

	private boolean isVacio(String valor) {
		return valor == null || valor.trim().isEmpty();
	}
}