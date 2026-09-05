package com.mountainbackend.pedidos.modelo;

/**
 * Línea de un pedido normalizada por el servicio.
 * Los nombres de los campos se mantienen en inglés porque forman parte del
 * contrato JSON que consume el frontend.
 */
public record LineaPedido(
		Long productId,
		String name,
		int price,
		int quantity) {

}