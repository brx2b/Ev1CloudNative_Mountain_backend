package com.mountainbackend.pedidos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Prueba la validación del JWT local emitido por servicio-usuarios (HS256).
 * Se activa el validador local (sin Azure) y se verifica el flujo completo:
 * 401 sin token, 403 sin scope, 201 con token válido.
 */
@SpringBootTest(properties = {
		"app.seguridad.local-habilitado=true",
		"app.seguridad.local-secreto=cambiar-en-produccion-secreto-compartido-32bytes",
		"app.seguridad.local-emisor=pedidos360-usuarios",
		"app.seguridad.local-audiencia=pedidos360-api",
		"app.seguridad.local-scope-requerido=orders.write"
})
@AutoConfigureMockMvc
class SeguridadLocalJwtTests {

	private static final String SECRETO = "cambiar-en-produccion-secreto-compartido-32bytes";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void sinTokenDevuelve401() throws Exception {
		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"id\":1,\"name\":\"Alpha SV Jacket\",\"price\":899,\"quantity\":1}]}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenConFirmaIncorrectaDevuelve401() throws Exception {
		String token = emitir("otro-secreto-diferente-para-firma-incorrecta-123456", claimsConScope());
		mockMvc.perform(get("/orders")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenLocalSinScopeDevuelve403() throws Exception {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.subject("1")
			.issuer("pedidos360-usuarios")
			.audience("pedidos360-api")
			.issueTime(new Date())
			.expirationTime(new Date(System.currentTimeMillis() + 60_000))
			.claim("email", "demo@summitlab.cl")
			.claim("scp", "catalogo.lectura")
			.build();
		mockMvc.perform(post("/orders")
				.header("Authorization", "Bearer " + emitir(SECRETO, claims))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"id\":1,\"price\":899,\"quantity\":1}]}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void tokenLocalValidoCreaPedido() throws Exception {
		String token = emitir(SECRETO, claimsConScope());
		mockMvc.perform(post("/orders")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"id\":1,\"name\":\"Alpha SV Jacket\",\"price\":899,\"quantity\":2}]}"))
			.andExpect(status().isCreated());
	}

	private JWTClaimsSet claimsConScope() {
		return new JWTClaimsSet.Builder()
			.subject("1")
			.issuer("pedidos360-usuarios")
			.audience("pedidos360-api")
			.issueTime(new Date())
			.expirationTime(new Date(System.currentTimeMillis() + 60_000))
			.claim("email", "demo@summitlab.cl")
			.claim("name", "Demo SummitLab")
			.claim("roles", "CLIENTE")
			.claim("scp", "orders.write")
			.build();
	}

	private String emitir(String secreto, JWTClaimsSet claims) throws Exception {
		JWSHeader cabecera = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build();
		SignedJWT firmado = new SignedJWT(cabecera, claims);
		firmado.sign(new MACSigner(secreto.getBytes(StandardCharsets.UTF_8)));
		return firmado.serialize();
	}
}