package com.mountainbackend.productos.dto;

import java.util.List;

/**
 * Payload aceptado por POST /products.
 * Idéntico al Producto pero sin id: el servidor asigna el siguiente disponible
 * (los nombres del JSON se mantienen en inglés, igual que el frontend).
 */
public record SolicitudProducto(
		String name,
		String description,
		Integer price,
		String image,
		String membrane,
		String tempRating,
		String waterproof,
		String weight,
		List<String> activity,
		String category,
		String brand,
		List<String> colors) {

}