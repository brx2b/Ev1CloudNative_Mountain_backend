package com.mountainbackend.usuarios.configuracion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Registro de endpoints en vivo: cada microservicio expone el inventario de sus
 * rutas reales (método + path + handler) en GET /api/endpoints. Se construye de
 * forma dinámica recorriendo todos los RequestMappingHandlerMapping registrados
 * (el de controllers y, si existe, el de Actuator).
 */
@Component
public class RegistroEndpoints {

	private static final Logger log = LoggerFactory.getLogger(RegistroEndpoints.class);

	private final ApplicationContext contexto;

	public RegistroEndpoints(ApplicationContext contexto) {
		this.contexto = contexto;
	}

	public List<Map<String, Object>> listarEndpoints() {
		Set<String> incluidos = new HashSet<>();
		List<Map<String, Object>> resultado = new ArrayList<>();

		for (RequestMappingHandlerMapping mapeo : contexto.getBeansOfType(RequestMappingHandlerMapping.class).values()) {
			for (Map.Entry<RequestMappingInfo, HandlerMethod> entrada : mapeo.getHandlerMethods().entrySet()) {
				RequestMappingInfo info = entrada.getKey();
				HandlerMethod metodo = entrada.getValue();

				String path = extraerPath(info);
				String clave = metodoHttp(info) + " " + path;
				if (incluidos.add(clave)) {
					resultado.add(Map.of(
						"method", metodoHttp(info),
						"path", path,
						"handler", metodo.getBeanType().getSimpleName() + "." + metodo.getMethod().getName()));
				}
			}
		}

		resultado.sort(Comparator.comparing(e -> e.get("path").toString()));
		return resultado;
	}

	private String metodoHttp(RequestMappingInfo info) {
		Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
		if (metodos.isEmpty()) {
			return "GET";
		}
		for (RequestMethod metodo : metodos) {
			return metodo.name();
		}
		return "GET";
	}

	private String extraerPath(RequestMappingInfo info) {
		if (info.getPathPatternsCondition() != null && !info.getPathPatternsCondition().getPatterns().isEmpty()) {
			return info.getPathPatternsCondition().getPatterns().iterator().next().getPatternString();
		}
		if (!info.getDirectPaths().isEmpty()) {
			return info.getDirectPaths().iterator().next();
		}
		return "/";
	}
}