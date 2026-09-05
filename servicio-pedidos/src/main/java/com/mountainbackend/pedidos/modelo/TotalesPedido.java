package com.mountainbackend.pedidos.modelo;

/**
 * Totales calculados por el servidor (nunca se confía en los del cliente).
 * Los nombres (count/subtotal) son parte del contrato JSON del frontend.
 */
public record TotalesPedido(
		int count,
		int subtotal) {

}