package com.mountainbackend.pedidos.servicio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.mountainbackend.pedidos.dto.SolicitudPedido;
import com.mountainbackend.pedidos.modelo.LineaPedido;
import com.mountainbackend.pedidos.modelo.Pedido;
import com.mountainbackend.pedidos.modelo.TotalesPedido;

/**
 * Registro de pedidos en memoria (mapa concurrente).
 * Para producción puede reemplazarse por una base de datos (DynamoDB /
 * RDS) sin modificar el controlador.
 */
@Service
public class ServicioPedidos {

	private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
	private final AtomicLong secuencia = new AtomicLong(100_000);

	public Pedido crear(SolicitudPedido solicitud, String correoCliente, String nombreCliente) {
		List<SolicitudPedido.LineaSolicitud> lineas = solicitud != null ? solicitud.getItems() : null;
		if (lineas == null || lineas.isEmpty()) {
			throw new IllegalArgumentException("El pedido debe incluir al menos un item (campo 'items').");
		}

		List<LineaPedido> items = new ArrayList<>();
		int cantidadTotal = 0;
		int subtotal = 0;
		for (SolicitudPedido.LineaSolicitud linea : lineas) {
			Long idProducto = linea.id() != null ? linea.id() : linea.productId();
			if (idProducto == null || idProducto <= 0) {
				throw new IllegalArgumentException("Cada item debe incluir 'id' o 'productId' válido.");
			}
			int cantidad = linea.quantity() != null ? linea.quantity() : 1;
			if (cantidad < 1) {
				throw new IllegalArgumentException("El campo 'quantity' de cada item debe ser mayor o igual a 1.");
			}
			int precio = linea.price() != null ? linea.price() : 0;
			cantidadTotal += cantidad;
			subtotal += precio * cantidad;
			items.add(new LineaPedido(idProducto, linea.name() != null ? linea.name() : "", precio, cantidad));
		}

		String id = "PEDIDO-" + secuencia.getAndIncrement();
		Pedido pedido = new Pedido(
			id,
			"RECIBIDO",
			correoCliente,
			nombreCliente,
			Instant.now(),
			List.copyOf(items),
			new TotalesPedido(cantidadTotal, subtotal));
		pedidos.put(id, pedido);
		return pedido;
	}

	public List<Pedido> listarTodos() {
		return new ArrayList<>(pedidos.values());
	}

	public Optional<Pedido> buscarPorId(String id) {
		return Optional.ofNullable(pedidos.get(id));
	}
}