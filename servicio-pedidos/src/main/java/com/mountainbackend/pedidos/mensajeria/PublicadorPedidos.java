package com.mountainbackend.pedidos.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publica EventoPedidoCreado como JSON plano en el topico pedidos.creados
 * (broker Kafka en backend-3). Si el broker no esta disponible, solo se
 * registra el fallo: el pedido ya quedo creado y la compra (201) nunca se
 * bloquea por la mensajeria.
 */
@Component
public class PublicadorPedidos {

	private static final Logger log = LoggerFactory.getLogger(PublicadorPedidos.class);

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final String topic;

	public PublicadorPedidos(
			KafkaTemplate<String, String> kafkaTemplate,
			ObjectMapper objectMapper,
			@Value("${app.kafka.topic.pedidos:pedidos.creados}") String topic) {
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
		this.topic = topic;
	}

	public void publicarPedidoCreado(EventoPedidoCreado evento) {
		try {
			String json = objectMapper.writeValueAsString(evento);
			String clave = evento.orderId() == null ? "sin-clave" : evento.orderId();
			kafkaTemplate.send(topic, clave, json).whenComplete((resultado, error) -> {
				if (error != null) {
					log.error("No se pudo publicar en {}: {}", topic, error.getMessage());
				} else {
					log.info("Evento pedido.creado publicado en {} key={} offset={}",
						topic, clave, resultado.getRecordMetadata().offset());
				}
			});
		} catch (Exception ex) {
			log.warn("No se pudo publicar pedido.creado para {}: {}",
				evento != null ? evento.orderId() : "?", ex.getMessage());
		}
	}
}
