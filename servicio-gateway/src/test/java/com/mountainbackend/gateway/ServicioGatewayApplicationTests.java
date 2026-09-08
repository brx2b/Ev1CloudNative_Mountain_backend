package com.mountainbackend.gateway;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ServicioGatewayApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextoCarga() {
	}

	@Test
	void registroEndpointsExponeTodosLosServicios() throws Exception {
		mockMvc.perform(get("/api/endpoints"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.servicio").value("servicio-gateway"))
			.andExpect(jsonPath("$.descripcion").value("PEDIDOS360 - API Gateway (entrypoint único del frontend)"))
			.andExpect(jsonPath("$.servicios", org.hamcrest.Matchers.hasSize(3)))
			.andExpect(jsonPath("$.servicios[0].servicio").value("servicio-usuarios"))
			.andExpect(jsonPath("$.servicios[1].servicio").value("servicio-productos"))
			.andExpect(jsonPath("$.servicios[2].servicio").value("servicio-pedidos"))
			.andExpect(jsonPath("$.gateway[0].path").value("/api/endpoints"));
	}

	@Test
	void healthCheckExpuesto() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk());
	}
}