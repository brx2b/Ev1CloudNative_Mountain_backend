package com.mountainbackend.productos.controlador;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mountainbackend.productos.dto.SolicitudProducto;
import com.mountainbackend.productos.modelo.Producto;
import com.mountainbackend.productos.repositorio.RepositorioProductos;

/**
 * Rutas públicas del catálogo expuestas a través del AWS API Gateway.
 * GET  /products      -> lista completa (el frontend acepta array o { products: [] })
 * GET  /products/{id} -> detalle unitario
 * POST /products      -> crea un producto y devuelve 201 con el recurso
 */
@RestController
@RequestMapping("/products")
public class ControladorProductos {

	private final RepositorioProductos repositorio;

	public ControladorProductos(RepositorioProductos repositorio) {
		this.repositorio = repositorio;
	}

	@GetMapping
	public List<Producto> listar() {
		return repositorio.listarTodos();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Producto> obtenerPorId(@PathVariable("id") long id) {
		return repositorio.buscarPorId(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<Producto> crear(@RequestBody SolicitudProducto solicitud) {
		validar(solicitud);
		Producto creado = repositorio.guardar(solicitud);
		return ResponseEntity.status(HttpStatus.CREATED)
			.location(URI.create("/products/" + creado.id()))
			.body(creado);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> alErrorDeArgumento(IllegalArgumentException ex) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("status", 400);
		cuerpo.put("error", ex.getMessage());
		return ResponseEntity.badRequest().body(cuerpo);
	}

	private void validar(SolicitudProducto solicitud) {
		if (solicitud.name() == null || solicitud.name().isBlank()) {
			throw new IllegalArgumentException("El nombre del producto es obligatorio.");
		}
		if (solicitud.price() == null || solicitud.price() <= 0) {
			throw new IllegalArgumentException("El precio debe ser un número mayor que cero.");
		}
	}
}