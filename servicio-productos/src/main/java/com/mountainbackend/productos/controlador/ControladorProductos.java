package com.mountainbackend.productos.controlador;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mountainbackend.productos.modelo.Producto;
import com.mountainbackend.productos.repositorio.RepositorioProductos;

/**
 * Rutas públicas del catálogo expuestas a través del AWS API Gateway.
 * GET /products      -> lista completa (el frontend acepta array o { products: [] })
 * GET /products/{id} -> detalle unitario
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
}