package com.mountainbackend.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ServicioPedidosApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServicioPedidosApplication.class, args);
	}

}