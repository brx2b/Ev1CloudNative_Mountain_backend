package com.mountainbackend.gateway.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del enrutamiento del gateway: URLs internas de los
 * microservicios y timeout del proxy HTTP.
 */
@ConfigurationProperties(prefix = "app.servicios")
public class PropiedadesGateway {

	private String authUrl = "http://localhost:8083";
	private String productosUrl = "http://localhost:8081";
	private String pedidosUrl = "http://localhost:8082";
	private long timeoutMs = 10_000;

	public String getAuthUrl() {
		return authUrl;
	}

	public void setAuthUrl(String authUrl) {
		this.authUrl = authUrl;
	}

	public String getProductosUrl() {
		return productosUrl;
	}

	public void setProductosUrl(String productosUrl) {
		this.productosUrl = productosUrl;
	}

	public String getPedidosUrl() {
		return pedidosUrl;
	}

	public void setPedidosUrl(String pedidosUrl) {
		this.pedidosUrl = pedidosUrl;
	}

	public long getTimeoutMs() {
		return timeoutMs;
	}

	public void setTimeoutMs(long timeoutMs) {
		this.timeoutMs = timeoutMs;
	}
}