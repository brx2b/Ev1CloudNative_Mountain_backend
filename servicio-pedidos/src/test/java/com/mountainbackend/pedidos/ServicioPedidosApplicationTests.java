package com.mountainbackend.pedidos;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class ServicioPedidosApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextoCarga() {
	}

	@Test
	void crearPedidoCalculaTotalesEnServidor() throws Exception {
		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"id\":1,\"name\":\"Alpha SV Jacket\",\"price\":899,\"quantity\":2}]}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id", startsWith("PEDIDO-")))
			.andExpect(jsonPath("$.status").value("RECIBIDO"))
			.andExpect(jsonPath("$.totals.count").value(2))
			.andExpect(jsonPath("$.totals.subtotal").value(1798));
	}

	@Test
	void crearPedidoRechazaItemsVacios() throws Exception {
		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[]}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void pedidoPuedeRecuperarsePorId() throws Exception {
		MvcResult creado = mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"productId\":3,\"price\":549,\"quantity\":1}]}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.items[0].productId").value(3))
			.andReturn();

		String id = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/orders/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.items[0].name").value(""));

		mockMvc.perform(get("/orders/{id}", "PEDIDO-999999"))
			.andExpect(status().isNotFound());
	}
}