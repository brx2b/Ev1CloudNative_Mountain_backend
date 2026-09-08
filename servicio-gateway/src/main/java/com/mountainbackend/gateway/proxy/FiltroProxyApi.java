package com.mountainbackend.gateway.proxy;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import com.mountainbackend.gateway.configuracion.PropiedadesGateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Proxy HTTP hacia los microservicios internos. Es el único entrypoint que ve
 * el frontend (AWS API Gateway desplegado, o el gateway local en :9000):
 *
 *   /auth/*   -> servicio-usuarios   (registro/login, público)
 *   /products -> servicio-productos  (catálogo, público)
 *   /orders   -> servicio-pedidos    (protegido con JWT)
 *   /api/endpoints -> este servicio  (registro de endpoints, público)
 *
 * Se reenvía método, headers (Authorization) y body tal cual.
 */
@Component
@Order(1)
public class FiltroProxyApi extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(FiltroProxyApi.class);

	private final PropiedadesGateway propiedades;
	private final RestClient cliente;

	public FiltroProxyApi(PropiedadesGateway propiedades) {
		this.propiedades = propiedades;
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory();
		fabrica.setReadTimeout(Duration.ofMillis(propiedades.getTimeoutMs()));
		this.cliente = RestClient.builder().requestFactory(fabrica).build();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			chain.doFilter(request, response);
			return;
		}

		String ruta = request.getRequestURI();
		Optional<Destino> destino = resolverDestino(ruta);
		if (destino.isEmpty()) {
			chain.doFilter(request, response);
			return;
		}

		reenviar(destino.get(), request, response);
	}

	private Optional<Destino> resolverDestino(String ruta) {
		if (ruta.startsWith("/auth/") || ruta.equals("/auth") || ruta.startsWith("/users")) {
			return Optional.of(new Destino("servicio-usuarios", propiedades.getAuthUrl(), ruta));
		}
		if (ruta.equals("/products") || ruta.startsWith("/products/")) {
			return Optional.of(new Destino("servicio-productos", propiedades.getProductosUrl(), ruta));
		}
		if (ruta.equals("/orders") || ruta.startsWith("/orders/")) {
			return Optional.of(new Destino("servicio-pedidos", propiedades.getPedidosUrl(), ruta));
		}
		return Optional.empty();
	}

	private void reenviar(Destino destino, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		String url = UriComponentsBuilder.fromUriString(destino.base)
			.path(destino.ruta)
			.query(request.getQueryString() != null ? request.getQueryString() : "")
			.build(true)
			.toUriString();

		byte[] cuerpo = leerCuerpo(request);

		try {
			RestClient.RequestBodySpec spec = cliente.method(HttpMethod.valueOf(request.getMethod().toUpperCase()))
				.uri(url)
				.header("Authorization", valorOmitio(request.getHeader("Authorization")));
			if (request.getContentType() != null) {
				spec.contentType(org.springframework.http.MediaType.parseMediaType(request.getContentType()));
			}
			ResponseEntity<byte[]> respuesta;
			if (cuerpo.length > 0) {
				respuesta = spec.body(cuerpo).retrieve().toEntity(byte[].class);
			} else {
				respuesta = spec.retrieve().toEntity(byte[].class);
			}

			response.setStatus(respuesta.getStatusCode().value());
			if (respuesta.getHeaders().getContentType() != null) {
				response.setContentType(respuesta.getHeaders().getContentType().toString());
			}
			byte[] body = respuesta.getBody();
			if (body != null) {
				response.getOutputStream().write(body);
			}
		} catch (org.springframework.web.client.RestClientResponseException ex) {
			response.setStatus(ex.getStatusCode().value());
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			byte[] cuerpoError = ex.getResponseBodyAsByteArray();
			if (cuerpoError != null && cuerpoError.length > 0) {
				response.getOutputStream().write(cuerpoError);
			} else {
				escribirError(response, HttpStatus.valueOf(ex.getStatusCode().value()),
					"Error del servicio destino " + destino.nombre + ".");
			}
			log.info("Gateway -> {} respondió {}: {}", destino.nombre, ex.getStatusCode().value(), ex.getMessage());
		} catch (org.springframework.web.client.RestClientException ex) {
			escribirError(response, HttpStatus.BAD_GATEWAY,
				"El servicio destino " + destino.nombre + " no respondió (" + ex.getClass().getSimpleName() + ").");
			log.error("Gateway -> {} falló: {}", destino.nombre, ex.getMessage());
		} catch (Exception ex) {
			escribirError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del gateway.");
			log.error("Gateway -> {} error inesperado: {}", destino.nombre, ex.getMessage(), ex);
		}
	}

	private String valorOmitio(String valor) {
		return valor != null && !valor.isBlank() ? valor : "";
	}

	private byte[] leerCuerpo(HttpServletRequest request) {
		try {
			ServletInputStream flujo = request.getInputStream();
			try (var salida = new java.io.ByteArrayOutputStream()) {
				byte[] buffer = new byte[4096];
				int leidos;
				while ((leidos = flujo.read(buffer)) != -1) {
					salida.write(buffer, 0, leidos);
				}
				return salida.toByteArray();
			}
		} catch (IOException ex) {
			return new byte[0];
		}
	}

	private void escribirError(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
		response.setStatus(estado.value());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"status\":" + estado.value() + ",\"error\":\"" + mensaje + "\"}");
	}

	private record Destino(String nombre, String base, String ruta) {
	}
}