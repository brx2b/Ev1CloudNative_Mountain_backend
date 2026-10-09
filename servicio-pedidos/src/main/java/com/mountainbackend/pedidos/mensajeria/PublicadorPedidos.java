package com.mountainbackend.pedidos.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publica EventoPedidoCreado como JSON plano (String). Si el broker no
 * está disponible, solo se registra el fallo: el pedido ya quedó
 * creado y la compra (201) nunca se bloquea por la mensajería.
 */
public class PublicadorPedidos {

	private static final Logger log = LoggerFactory.getLogger(PublicadorPedidos.class);

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;
	private final String exchange;
	private final String routingKey;

	public PublicadorPedidos(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
			String exchange, String routingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
		this.exchange = exchange;
		this.routingKey = routingKey;
	}

	public void publicarPedidoCreado(EventoPedidoCreado evento) {
		try {
			// Bytes crudos text/plain: si se usa convertAndSend con el
			// conversor Jackson, el String se re-serializa escapado y los
			// consumidores lo ven vacío.
			byte[] cuerpo = objectMapper.writeValueAsBytes(evento);
			MessageProperties props = new MessageProperties();
			props.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
			rabbitTemplate.send(exchange, routingKey, new Message(cuerpo, props));
			log.info("Evento pedido.creado publicado para {}", evento.orderId());
		} catch (Exception ex) {
			log.warn("No se pudo publicar pedido.creado para {}: {}",
				evento != null ? evento.orderId() : "?", ex.getMessage());
		}
	}
}
