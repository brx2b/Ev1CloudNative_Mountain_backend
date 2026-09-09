package com.mountainbackend.productos.repositorio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.mountainbackend.productos.dto.SolicitudProducto;
import com.mountainbackend.productos.modelo.Producto;

/**
 * Catálogo en memoria de la tienda SummitLab.
 * Se siembra con los mismos 8 productos simulados que usa el frontend
 * (mockProducts.js) para que la integración calce sin cambios.
 *
 * Para producción puede reemplazarse por una base de datos (DynamoDB /
 * RDS-PostgreSQL) sin tocar el controlador.
 */
@Repository
public class RepositorioProductos {

	private final Map<Long, Producto> productos = new ConcurrentHashMap<>();

	public RepositorioProductos() {
		sembrarCatalogo();
	}

	public List<Producto> listarTodos() {
		return List.copyOf(productos.values());
	}

	public Optional<Producto> buscarPorId(long id) {
		return Optional.ofNullable(productos.get(id));
	}

	public Producto guardar(SolicitudProducto solicitud) {
		long id = generarSiguienteId();
		Producto producto = new Producto(id,
			solicitud.name(),
			obligatorioONulo(solicitud.description()),
			solicitud.price() != null ? solicitud.price() : 0,
			obligatorioONulo(solicitud.image()),
			obligatorioONulo(solicitud.membrane()),
			obligatorioONulo(solicitud.tempRating()),
			obligatorioONulo(solicitud.waterproof()),
			obligatorioONulo(solicitud.weight()),
			solicitud.activity() != null ? solicitud.activity() : List.of(),
			obligatorioONulo(solicitud.category()),
			obligatorioONulo(solicitud.brand()),
			solicitud.colors() != null ? solicitud.colors() : List.of());
		productos.put(id, producto);
		return producto;
	}

	private long generarSiguienteId() {
		return productos.keySet().stream()
			.mapToLong(Long::longValue)
			.max()
			.orElse(0L) + 1L;
	}

	private String obligatorioONulo(String valor) {
		return valor != null ? valor : "";
	}

	private void sembrarCatalogo() {
		productos.put(1L, new Producto(1L,
			"Alpha SV Jacket",
			"Hardshell extremo para expediciones alpinas. La capa más resistente de nuestra línea, diseñada para condiciones hostiles sin compromiso.",
			899,
			"https://images.unsplash.com/photo-1551698618-1dfe5d97d256?w=600&h=750&fit=crop",
			"Gore-Tex Pro 3L",
			"-30°C",
			"28,000 mm",
			"430g",
			List.of("alpinismo", "expedicion"),
			"hardshell",
			"Summit Lab",
			List.of("#1a1a2e", "#c0392b", "#2d3436")));

		productos.put(2L, new Producto(2L,
			"Ridge Thermal Down",
			"Chaqueta de pluma 850-fill con aislamiento zonal. Máxima compresibilidad y calor en peso ultraligero para travesías largas.",
			649,
			"https://images.unsplash.com/photo-1544022613-e87ca75a784a?w=600&h=750&fit=crop",
			"DWR Repelente",
			"-18°C",
			"10,000 mm",
			"320g",
			List.of("trekking", "campismo"),
			"aislante",
			"Summit Lab",
			List.of("#2d3436", "#0c2461", "#e17055")));

		productos.put(3L, new Producto(3L,
			"StormBreaker GTX",
			"Hardshell versátil con ventilación completa bajo brazos. Equilibrio perfecto entre protección total y transpirabilidad.",
			549,
			"https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=600&h=750&fit=crop",
			"Gore-Tex Active",
			"-12°C",
			"20,000 mm",
			"370g",
			List.of("alpinismo", "escalada"),
			"hardshell",
			"Peak Forge",
			List.of("#0c2461", "#2d3436", "#e17055")));

		productos.put(4L, new Producto(4L,
			"Cirrus Pro Hoody",
			"Softshell técnico con membrana micro-porosa. Movilidad total y protección contra viento para las vías más exigentes.",
			399,
			"https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=600&h=750&fit=crop",
			"NeoShell",
			"-5°C",
			"15,000 mm",
			"280g",
			List.of("escalada", "trail"),
			"softshell",
			"Peak Forge",
			List.of("#636e72", "#0c2461", "#2d3436")));

		productos.put(5L, new Producto(5L,
			"Expedition Base Layer",
			"Capa base merino técnico-sintético con control de olores y gestión de humedad avanzada. El fundamento de todo sistema de capas.",
			149,
			"https://images.unsplash.com/photo-1483721310020-03333e577078?w=600&h=750&fit=crop",
			"Merino Tech",
			"0°C",
			"3,000 mm",
			"180g",
			List.of("trekking", "campismo", "trail"),
			"capa-base",
			"Summit Lab",
			List.of("#2d3436", "#636e72", "#0c2461")));

		productos.put(6L, new Producto(6L,
			"Pinnacle Shell",
			"Hardshell ligero con construcción minimalista. Diseñado para velocidad y protección en carreras de montaña y skyrunning.",
			459,
			"https://images.unsplash.com/photo-1551028719-00167b16eac5?w=600&h=750&fit=crop",
			"Gore-Tex Shakedry",
			"-8°C",
			"25,000 mm",
			"210g",
			List.of("trail", "skyrunning"),
			"hardshell",
			"Void Alpine",
			List.of("#e17055", "#0c2461", "#2d3436")));

		productos.put(7L, new Producto(7L,
			"Glacier Thermal Pants",
			"Pantalón técnico con aislamiento sintético y membrana impermeable. Protección total para las condiciones más severas.",
			529,
			"https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=600&h=750&fit=crop",
			"Gore-Tex Infinium",
			"-20°C",
			"18,000 mm",
			"520g",
			List.of("alpinismo", "expedicion"),
			"hardshell",
			"Void Alpine",
			List.of("#2d3436", "#636e72", "#0c2461")));

		productos.put(8L, new Producto(8L,
			"Alpine Trail Runner",
			"Zapatilla de trail con vibram mega-grip y protección rock-plate. Agarre extremo en terreno técnico y húmedo.",
			289,
			"https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&h=750&fit=crop",
			"eVent Waterproof",
			"-2°C",
			"12,000 mm",
			"340g",
			List.of("trail", "skyrunning", "trekking"),
			"calzado",
			"Peak Forge",
			List.of("#e17055", "#2d3436", "#0c2461")));
	}
}