package com.mountainbackend.productos.modelo;

import java.util.List;

/**
 * Producto del catálogo de la tienda SummitLab.
 * Los campos reflejan exactamente el contrato que consume el frontend
 * (ver mockProducts.js), por eso los nombres de las propiedades del JSON
 * se mantienen en inglés.
 */
public record Producto(
		long id,
		String name,
		String description,
		int price,
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