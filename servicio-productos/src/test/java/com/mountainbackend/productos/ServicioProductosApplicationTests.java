package com.mountainbackend.productos;

import static org.hamcrest.Matchers.hasSize;
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
class ServicioProductosApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextoCarga() {
	}

	@Test
	void listarProductosDevuelveCatalogoSemilla() throws Exception {
		mockMvc.perform(get("/products"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(8)))
			.andExpect(jsonPath("$[0].id").value(1))
			.andExpect(jsonPath("$[0].name").value("Alpha SV Jacket"))
			.andExpect(jsonPath("$[0].price").value(899));
	}

	@Test
	void obtenerProductoPorIdDevuelveDetalle() throws Exception {
		mockMvc.perform(get("/products/2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(2))
			.andExpect(jsonPath("$.category").value("aislante"))
			.andExpect(jsonPath("$.brand").value("Summit Lab"));
	}

	@Test
	void obtenerProductoInexistenteDevuelve404() throws Exception {
		mockMvc.perform(get("/products/999"))
			.andExpect(status().isNotFound());
	}
}