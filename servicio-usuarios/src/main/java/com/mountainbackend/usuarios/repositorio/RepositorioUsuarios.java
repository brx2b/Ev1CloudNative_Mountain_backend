package com.mountainbackend.usuarios.repositorio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.mountainbackend.usuarios.modelo.Usuario;

/**
 * Usuarios en memoria (mapa concurrente) pensado para la demo.
 * En producción puede reemplazarse por DynamoDB / RDS sin tocar el controlador.
 */
@Repository
public class RepositorioUsuarios {

	private final Map<Long, Usuario> porId = new ConcurrentHashMap<>();
	private final Map<String, Long> idsPorEmail = new ConcurrentHashMap<>();
	private final AtomicLong secuencia = new AtomicLong(1);

	public Usuario guardar(Usuario usuario) {
		long id = usuario.id() > 0 ? usuario.id() : secuencia.getAndIncrement();
		Usuario nuevo = new Usuario(id, usuario.name(), usuario.email(), usuario.passwordHash(), usuario.rol());
		porId.put(id, nuevo);
		idsPorEmail.put(nuevo.email(), id);
		return nuevo;
	}

	public Optional<Usuario> buscarPorId(long id) {
		return Optional.ofNullable(porId.get(id));
	}

	public Optional<Usuario> buscarPorEmail(String email) {
		Long id = idsPorEmail.get(email);
		return id != null ? Optional.ofNullable(porId.get(id)) : Optional.empty();
	}

	public boolean existeEmail(String email) {
		return idsPorEmail.containsKey(email);
	}

	public List<Usuario> listarTodos() {
		return List.copyOf(porId.values());
	}
}