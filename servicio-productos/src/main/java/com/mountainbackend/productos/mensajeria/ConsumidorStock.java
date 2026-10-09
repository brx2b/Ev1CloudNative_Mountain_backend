package com.mountainbackend.productos.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mountainbackend.productos.repositorio.RepositorioProductos;

/**
 * Consume pedido.creado y descuenta stock. Declara la misma topología
 * que pedidos (idempotente). El payload llega como String JSON.
 */
@Configuration
public class ConsumidorStock {

	public static final String COLA_PEDIDOS_CREADOS = "pedidos.creados";

	private static final Logger log = LoggerFactory.getLogger(ConsumidorStock.class);

	private final RepositorioProductos repositorio;
	private final ObjectMapper objectMapper;

	@Value("${app.mq.exchange:pedidos.eventos}")
	private String exchange;

	@Value("${app.mq.routing-pedido-creado:pedido.creado}")
	private String routingKey;

	public ConsumidorStock(RepositorioProductos repositorio, ObjectMapper objectMapper) {
		this.repositorio = repositorio;
		this.objectMapper = objectMapper;
	}

	@Bean
	public DirectExchange exchangePedidos() {
		return new DirectExchange(exchange, true, false);
	}

	@Bean
	public Queue colaPedidosCreados() {
		return new Queue(COLA_PEDIDOS_CREADOS, true);
	}

	@Bean
	public Binding bindingPedidosCreados(DirectExchange exchangePedidos, Queue colaPedidosCreados) {
		return BindingBuilder.bind(colaPedidosCreados).to(exchangePedidos).with(routingKey);
	}

	@RabbitListener(queues = COLA_PEDIDOS_CREADOS)
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
