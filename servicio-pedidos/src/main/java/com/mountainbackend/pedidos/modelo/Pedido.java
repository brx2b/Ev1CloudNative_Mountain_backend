package com.mountainbackend.pedidos.modelo;

import java.time.Instant;
import java.util.List;

/**
 * Pedido registrado por el microservicio de carrito/compras.
 * El id sigue el formato del mock del frontend: PEDIDO-XXXXXX.
 * Los campos (id/status/items/totals/createdAt/...) son parte del contrato
 * JSON que consume el frontend.
 */
public record Pedido(
		String id,
		String status,
		String customerEmail,
		String customerName,
		Instant createdAt,
		List<LineaPedido> items,
		TotalesPedido totals) {

}