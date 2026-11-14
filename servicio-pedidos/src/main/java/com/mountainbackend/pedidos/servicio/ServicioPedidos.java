package com.mountainbackend.pedidos.servicio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.mountainbackend.pedidos.dto.SolicitudPedido;
import com.mountainbackend.pedidos.mensajeria.EventoPedidoCreado;
import com.mountainbackend.pedidos.mensajeria.PublicadorPedidos;
import com.mountainbackend.pedidos.modelo.LineaPedido;
import com.mountainbackend.pedidos.modelo.Pedido;
import com.mountainbackend.pedidos.modelo.TotalesPedido;

/**
 * Registro de pedidos en memoria (mapa concurrente).
 * Para producción puede reemplazarse por una base de datos (DynamoDB /
 * RDS) sin modificar el controlador.
 *
 * El precio y el nombre de cada item se RECALCULAN contra el catálogo de
 * servicio-productos (GET /products/{id}); si el catálogo no responde se
 * cae al precio que envió el cliente (defensivo).
 */
@Service
public class ServicioPedidos {

	private static final Logger log = LoggerFactory.getLogger(ServicioPedidos.class);

	private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
	private final AtomicLong secuencia = new AtomicLong(100_000);
	private final PublicadorPedidos publicador;
	private final RestClient restProductos;

	public ServicioPedidos(PublicadorPedidos publicador,
			@Value("${app.productos.base-url:http://servicio-productos:8080}") String baseUrlProductos) {
		this.publicador = publicador;
		this.restProductos = RestClient.builder().baseUrl(baseUrlProductos).build();
	}

	public Pedido crear(SolicitudPedido solicitud, String correoCliente, String nombreCliente) {
		List<SolicitudPedido.LineaSolicitud> lineas = solicitud != null ? solicitud.getItems() : null;
		if (lineas == null || lineas.isEmpty()) {
			throw new IllegalArgumentException("El pedido debe incluir al menos un item (campo 'items').");
		}

		List<LineaPedido> items = new ArrayList<>();
		int cantidadTotal = 0;
		int subtotal = 0;
		Map<Long, ProductoRemoto> catalogo = new ConcurrentHashMap<>();
		for (SolicitudPedido.LineaSolicitud linea : lineas) {
			Long idProducto = linea.id() != null ? linea.id() : linea.productId();
			if (idProducto == null || idProducto <= 0) {
				throw new IllegalArgumentException("Cada item debe incluir 'id' o 'productId' válido.");
			}
			int cantidad = linea.quantity() != null ? linea.quantity() : 1;
			if (cantidad < 1) {
				throw new IllegalArgumentException("El campo 'quantity' de cada item debe ser mayor o igual a 1.");
			}
			ProductoRemoto remoto = catalogo.computeIfAbsent(idProducto, this::buscarProducto);
			int precio = remoto != null ? remoto.price()
				: (linea.price() != null ? linea.price() : 0);
			String nombre = remoto != null ? remoto.name()
				: (linea.name() != null ? linea.name() : "");
			cantidadTotal += cantidad;
			subtotal += precio * cantidad;
			items.add(new LineaPedido(idProducto, nombre, precio, cantidad));
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
		// Evento async: stock, notificaciones y envíos. No bloquea el 201.
		publicador.publicarPedidoCreado(new EventoPedidoCreado(
			id,
			correoCliente,
			nombreCliente,
			items.stream()
				.map(item -> new EventoPedidoCreado.ItemPedidoCreado(
					item.productId(), item.name(), item.price(), item.quantity()))
				.toList(),
			subtotal));
		return pedido;
	}

	public List<Pedido> listarTodos() {
		return new ArrayList<>(pedidos.values());
	}

	public Optional<Pedido> buscarPorId(String id) {
		return Optional.ofNullable(pedidos.get(id));
	}

	/** Precio y nombre canonico del producto (contrato JSON del servicio-productos). */
	private record ProductoRemoto(String name, int price) {
	}

	/**
	 * Consulta el precio real al catálogo. Devuelve null si no existe o el
	 * servicio-productos no responde (el pedido sigue con el precio del cliente).
	 */
	private ProductoRemoto buscarProducto(Long id) {
		try {
			ProductoRemoto producto = restProductos.get()
				.uri("/products/{id}", id)
				.retrieve()
				.body(ProductoRemoto.class);
			return producto;
		} catch (Exception ex) {
			log.warn("No se pudo consultar el producto {} al catálogo: {}", id, ex.getMessage());
			return null;
		}
	}
}