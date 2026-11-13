package com.mountainbackend.pedidos.configuracion;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.config.TopicBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuracion Kafka del servicio de pedidos (reemplaza a RabbitMQ).
 * Los eventos viajan como JSON plano (String) para no acoplar clases entre
 * microservicios. La clave del mensaje es el orderId, de modo que todos los
 * eventos de un mismo pedido caen en la misma particion (orden garantizado).
 * El topico pedidos.creados se declara de forma idempotente: si backend-3
 * (que aloja el broker) ya lo creo, la declaracion no tiene efecto.
 */
@Configuration
public class KafkaConfig {

	@Value("${app.kafka.bootstrap-servers:localhost:29092}")
	private String bootstrapServers;

	@Value("${app.kafka.topic.pedidos:pedidos.creados}")
	private String topicPedidos;

	@Bean
	public ProducerFactory<String, String> producerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		props.put(ProducerConfig.RETRIES_CONFIG, 3);
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
		return new DefaultKafkaProducerFactory<>(props);
	}

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}

	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> configs = new HashMap<>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		return new KafkaAdmin(configs);
	}

	@Bean
	public NewTopic topicPedidosCreados() {
		return TopicBuilder.name(topicPedidos).partitions(3).replicas(1).build();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
