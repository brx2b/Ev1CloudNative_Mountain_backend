package com.mountainbackend.productos.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mountainbackend.productos.repositorio.RepositorioProductos;

/**
 * Consume pedidos.creados (topico Kafka en backend-3) y descuenta stock.
 * groupId propio: como consumidor independiente recibe TODOS los eventos
 * aunque otros servicios tambien consuman el mismo topico. El payload
 * llega como String JSON; si es invalido solo se registra el fallo.
 */
@Component
public class ConsumidorStock {

	private static final Logger log = LoggerFactory.getLogger(ConsumidorStock.class);

	private final RepositorioProductos repositorio;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ConsumidorStock(RepositorioProductos repositorio) {
		this.repositorio = repositorio;
	}

	@KafkaListener(
		topics = "${app.kafka.topic.pedidos:pedidos.creados}",
		groupId = "productos-stock",
		containerFactory = "kafkaListenerContainerFactory")
	public void alPedidoCreado(String json) {
		try {
			JsonNode evento = objectMapper.readTree(json);
			String orderId = evento.path("orderId").asText("?");
			JsonNode items = evento.path("items");
			if (items.isArray()) {
				for (JsonNode item : items) {
					long productId = item.path("productId").asLong(-1);
					int quantity = item.path("quantity").asInt(1);
					int restante = repositorio.descontarStock(productId, quantity);
					if (restante >= 0) {
						log.info("Stock descontado por {}: producto {} x{} (quedan {})",
							orderId, productId, quantity, restante);
					} else {
						log.warn("Pedido {} trae producto desconocido {}", orderId, productId);
					}
				}
			}
		} catch (Exception ex) {
			log.warn("Evento pedido.creado inválido: {}", ex.getMessage());
		}
	}
}
