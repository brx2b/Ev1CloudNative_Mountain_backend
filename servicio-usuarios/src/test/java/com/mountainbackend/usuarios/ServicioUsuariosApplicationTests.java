package com.mountainbackend.usuarios;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@SpringBootTest
@AutoConfigureMockMvc
class ServicioUsuariosApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextoCarga() {
	}

	@Test
	void registroCreaUsuarioYDevuelveToken() throws Exception {
		String cuerpo = "{"
			+ "\"name\":\"Andrés Montaña\","
			+ "\"email\":\"andres@summitlab.cl\","
			+ "\"password\":\"secreto123\""
			+ "}";
		mockMvc.perform(post("/auth/registro")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.user.email").value("andres@summitlab.cl"))
			.andExpect(jsonPath("$.user.roles[0]").value("CLIENTE"))
			.andExpect(jsonPath("$.user.passwordHash").doesNotExist());
	}

	@Test
	void registroRechazaEmailDuplicado() throws Exception {
		String cuerpo = "{"
			+ "\"name\":\"Demo Dup\","
			+ "\"email\":\"demo@summitlab.cl\","
			+ "\"password\":\"secreto123\""
			+ "}";
		mockMvc.perform(post("/auth/registro")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo))
			.andExpect(status().isBadRequest());
	}

	@Test
	void ingresoConUsuarioSeedDevuelveToken() throws Exception {
		MvcResult resultado = mockMvc.perform(post("/auth/ingreso")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"demo@summitlab.cl\",\"password\":\"demo1234\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.user.email").value("demo@summitlab.cl"))
			.andExpect(jsonPath("$.user.roles[0]").value("CLIENTE"))
			.andReturn();

		String token = JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
		JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
		assertEquals("pedidos360-usuarios", claims.getIssuer());
		assertEquals(List.of("pedidos360-api"), claims.getAudience());
		assertEquals("demo@summitlab.cl", claims.getStringClaim("email"));
		assertEquals("orders.write", claims.getStringClaim("scp"));
	}

	@Test
	void ingresoConPasswordIncorrectaDevuelve401() throws Exception {
		mockMvc.perform(post("/auth/ingreso")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"demo@summitlab.cl\",\"password\":\"mala\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void listaUsuariosYRegistroEndpointsDisponibles() throws Exception {
		mockMvc.perform(get("/users"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(get("/api/endpoints"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.servicio").value("servicio-usuarios"))
			.andExpect(jsonPath("$.endpoints", hasSize(6)));
	}
}