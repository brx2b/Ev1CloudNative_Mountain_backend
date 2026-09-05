package com.mountainbackend.pedidos.dto;

import java.util.List;

/**
 * Payload aceptado por POST /orders.
 * Tolera que el item traiga "id" o "productId" (el frontend envía el producto
 * completo del carrito). El precio y el total se recalculan en el servidor.
 */
public class SolicitudPedido {

	private List<LineaSolicitud> items;

	public List<LineaSolicitud> getItems() {
		return items;
	}

	public void setItems(List<LineaSolicitud> items) {
		this.items = items;
	}

	/**
	 * Línea de carrito. Unicamente id/productId y quantity son obligatorios.
	 * Se aceptan esos nombres (inglés) tal como los envía el frontend.
	 */
	public record LineaSolicitud(
			Long id,
			Long productId,
			String name,
			Integer price,
			Integer quantity) {

	}
}