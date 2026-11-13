package com.mountainbackend.pedidos.mensajeria;

import java.util.List;

/**
 * Evento pedido.creado: contrato JSON que viaja por Kafka
 * (topico pedidos.creados, broker en backend-3). Lo consumen productos
 * (descuento de stock), notificaciones, envios y pagos (snapshot para
 * cobrar). Cada consumidor usa su propio groupId y se serializa a mano
 * como String para no acoplar clases entre microservicios.
 */
public record EventoPedidoCreado(
		String orderId,
		String customerEmail,
		String customerName,
		List<ItemPedidoCreado> items,
		int total) {

	public record ItemPedidoCreado(
			long productId,
			String name,
			int price,
			int quantity) {
	}
}
