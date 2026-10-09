package com.mountainbackend.pedidos.mensajeria;

import java.util.List;

/**
 * Evento pedido.creado: contrato JSON que viaja por RabbitMQ
 * (exchange pedidos.eventos, routing key pedido.creado, cola
 * pedidos.creados). Lo consumen productos (descuento de stock),
 * notificaciones y envíos. Se serializa a mano como String para no
 * acoplar clases entre microservicios.
 */
public record EventoPedidoCreado(
		String orderId,
		String customerEmail,
		String customerName,
		List<ItemPedidoCreado> items,
		int total) {

	public record ItemPedidoCreado(
			long productId,
			int quantity) {
	}
}
