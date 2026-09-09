package com.mountainbackend.productos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void crearProductoDevuelve201ConRecurso() throws Exception {
		String json = """
			{
			  "name": "Ice Axe Pro",
			  "description": "Piolet técnico para alpinismo.",
			  "price": 199,
			  "image": "https://images.unsplash.com/photo-1551698618-1dfe5d97d256?w=600&h=750&fit=crop",
			  "category": "herramientas",
			  "brand": "Peak Forge",
			  "activity": ["alpinismo"]
			}
			""";

		mockMvc.perform(post("/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(9))
			.andExpect(jsonPath("$.name").value("Ice Axe Pro"))
			.andExpect(jsonPath("$.price").value(199))
			.andExpect(jsonPath("$.category").value("herramientas"));

		mockMvc.perform(get("/products/9"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Ice Axe Pro"));
	}

	@Test
	void crearProductoSinNombreDevuelve400() throws Exception {
		String json = """
			{ "name": "", "price": 100 }
			""";

		mockMvc.perform(post("/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest());
	}

	@Test
	void crearProductoSinPrecioValidoDevuelve400() throws Exception {
		String json = """
			{ "name": "Crampon X", "price": 0 }
			""";

		mockMvc.perform(post("/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest());
	}
}