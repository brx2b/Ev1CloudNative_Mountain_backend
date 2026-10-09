package com.mountainbackend.pedidos.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Topología RabbitMQ de pedidos (declaración idempotente: los demás
 * microservicios declaran lo mismo). Exchange duradero + cola duradera
 * para que los eventos sobrevivan reinicios del broker.
 */
@Configuration
public class ConfiguracionMensajeria {

	public static final String COLA_PEDIDOS_CREADOS = "pedidos.creados";

	private static final Logger log = LoggerFactory.getLogger(ConfiguracionMensajeria.class);

	@Value("${app.mq.exchange:pedidos.eventos}")
	private String exchange;

	@Value("${app.mq.routing-pedido-creado:pedido.creado}")
	private String routingKey;

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

	@Bean
	public Jackson2JsonMessageConverter conversorJson() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
			Jackson2JsonMessageConverter conversorJson) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(conversorJson);
		return template;
	}

	@Bean
	public PublicadorPedidos publicadorPedidos(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
		log.info("Publicador de eventos pedido.creado activo (exchange={}, routing={})", exchange, routingKey);
		return new PublicadorPedidos(rabbitTemplate, objectMapper, exchange, routingKey);
	}
}
