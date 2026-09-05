package com.mountainbackend.pedidos.controlador;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mountainbackend.pedidos.dto.SolicitudPedido;
import com.mountainbackend.pedidos.modelo.Pedido;
import com.mountainbackend.pedidos.seguridad.FiltroValidacionJwt;
import com.mountainbackend.pedidos.servicio.ServicioPedidos;
import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Ruta privada /orders. Protegida por el JWT Authorizer del API Gateway y,
 * de forma adicional, por el FiltroValidacionJwt del propio microservicio.
 * Exige scopes/roles (por defecto scope "orders.write").
 */
@RestController
@RequestMapping("/orders")
public class ControladorPedidos {

	private final ServicioPedidos servicioPedidos;

	public ControladorPedidos(ServicioPedidos servicioPedidos) {
		this.servicioPedidos = servicioPedidos;
	}

	@PostMapping
	public ResponseEntity<Pedido> crear(@RequestBody SolicitudPedido solicitud, HttpServletRequest http) {
		JWTClaimsSet claims = (JWTClaimsSet) http.getAttribute(FiltroValidacionJwt.ATRIBUTO_CLAIMS_JWT);

		String correoCliente = extraerPrincipal(claims);
		String nombreCliente = claims != null ? stringONulo(claims.getClaim("name")) : null;

		Pedido pedido = servicioPedidos.crear(solicitud, correoCliente, nombreCliente);
		return ResponseEntity.status(HttpStatus.CREATED)
			.location(URI.create("/orders/" + pedido.id()))
			.body(pedido);
	}

	@GetMapping
	public List<Pedido> listar() {
		return servicioPedidos.listarTodos();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Pedido> obtenerPorId(@PathVariable("id") String id) {
		return servicioPedidos.buscarPorId(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> alErrorDeArgumento(IllegalArgumentException ex) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("status", 400);
		cuerpo.put("error", ex.getMessage());
		return ResponseEntity.badRequest().body(cuerpo);
	}

	private String extraerPrincipal(JWTClaimsSet claims) {
		if (claims == null) {
			return null;
		}
		Object principal = claims.getClaim("preferred_username");
		if (principal == null) {
			principal = claims.getClaim("upn");
		}
		if (principal == null) {
			principal = claims.getSubject();
		}
		return principal != null ? String.valueOf(principal) : null;
	}

	private String stringONulo(Object valor) {
		return valor != null ? String.valueOf(valor) : null;
	}
}