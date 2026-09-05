package com.mountainbackend.pedidos.seguridad;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mountainbackend.pedidos.configuracion.PropiedadesSeguridad;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Valida en el microservicio los tokens JWT firmados por Azure AD / Entra ID:
 *  - Firma y vigencia contra el JWKS remoto (RS256/RS384/RS512).
 *  - Issuer y audience esperados.
 *  - Scopes (claim scp/scope/scopes) y roles de aplicación (claim roles).
 *  - 401 sin token o token inválido/expirado; 403 sin scope/rol requerido.
 *
 * Se desactiva con app.seguridad.habilitado=false (modo demo local). Cuando se
 * conecte Azure basta con activarlo y setear jwks-uri, emisor, audiencias,
 * scopes/roles requeridos.
 */
@Component
@Order(1)
public class FiltroValidacionJwt extends OncePerRequestFilter {

	public static final String ATRIBUTO_CLAIMS_JWT = "jwt.claims";

	private static final Logger log = LoggerFactory.getLogger(FiltroValidacionJwt.class);

	private final PropiedadesSeguridad propiedades;
	private final JWKSource<SecurityContext> fuenteClaves;

	public FiltroValidacionJwt(PropiedadesSeguridad propiedades) {
		this.propiedades = propiedades;
		this.fuenteClaves = construirFuenteClaves();
	}

	private JWKSource<SecurityContext> construirFuenteClaves() {
		try {
			String jwksUri = recortarONulo(propiedades.getJwksUri());
			if (propiedades.isHabilitado() && jwksUri != null) {
				JWKSource<SecurityContext> fuente = new RemoteJWKSet<>(URI.create(jwksUri).toURL());
				log.info("Validación JWT activa contra jwks-uri={}", jwksUri);
				return fuente;
			}
		} catch (Exception ex) {
			log.error("No se pudo inicializar el JWKS ({})", ex.getMessage());
		}
		log.warn("Validación JWT DESACTIVADA: setea app.seguridad.habilitado=true y app.seguridad.jwks-uri.");
		return null;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !propiedades.isHabilitado() || "OPTIONS".equalsIgnoreCase(request.getMethod());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (fuenteClaves == null) {
			escribirError(response, 500, "La validación JWT no está inicializada: revise app.seguridad.jwks-uri.");
			return;
		}

		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			escribirError(response, 401, "Falta el token JWT en el header Authorization.");
			return;
		}

		String token = header.substring(7).trim();
		JWTClaimsSet claims;
		try {
			claims = verificarYParsear(token);
		} catch (Exception ex) {
			log.debug("Token rechazado: {}", ex.getMessage());
			escribirError(response, 401, "Token JWT inválido o expirado.");
			return;
		}

		if (!emisorValido(claims)) {
			escribirError(response, 401, "El issuer del token no es el esperado.");
			return;
		}

		if (!audienciaValida(claims)) {
			escribirError(response, 401, "La audience del token no es la esperada.");
			return;
		}

		if (!tieneAutoridadRequerida(claims)) {
			escribirError(response, 403, "No tienes los scopes/roles requeridos para esta operación.");
			return;
		}

		request.setAttribute(ATRIBUTO_CLAIMS_JWT, claims);
		chain.doFilter(request, response);
	}

	/**
	 * Verifica firma contra el JWKS remoto y valida vigencia (exp/nbf).
	 */
	private JWTClaimsSet verificarYParsear(String token) throws Exception {
		SignedJWT firmado = SignedJWT.parse(token);

		JWSHeader cabeceraJws = firmado.getHeader();
		JWSAlgorithm algoritmo = cabeceraJws.getAlgorithm();
		if (algoritmo == null || !JWSAlgorithm.Family.RSA.contains(algoritmo) && !JWSAlgorithm.Family.EC.contains(algoritmo)) {
			throw new IllegalStateException("Algoritmo de firma no soportado: " + algoritmo);
		}

		KeyType tipoClave = JWSAlgorithm.Family.RSA.contains(algoritmo) ? KeyType.RSA : KeyType.EC;
		JWKMatcher.Builder coincidencia = new JWKMatcher.Builder()
			.keyType(tipoClave)
			.algorithm(algoritmo);
		if (cabeceraJws.getKeyID() != null) {
			coincidencia.keyID(cabeceraJws.getKeyID());
		}
		List<JWK> claves = fuenteClaves.get(new JWKSelector(coincidencia.build()), (SecurityContext) null);
		if (claves == null || claves.isEmpty()) {
			throw new IllegalStateException("No se encontró clave de verificación en el JWKS");
		}

		boolean verificada = false;
		for (JWK clave : claves) {
			JWSVerifier verificador = null;
			if (clave instanceof RSAKey rsa) {
				verificador = new RSASSAVerifier(rsa);
			} else if (clave instanceof ECKey ec) {
				verificador = new ECDSAVerifier(ec);
			}
			if (verificador != null && firmado.verify(verificador)) {
				verificada = true;
				break;
			}
		}
		if (!verificada) {
			throw new IllegalStateException("Firma JWT no válida");
		}

		JWTClaimsSet claims = firmado.getJWTClaimsSet();
		Date ahora = new Date();
		Date expiracion = claims.getExpirationTime();
		if (expiracion == null || expiracion.before(ahora)) {
			throw new IllegalStateException("Token expirado");
		}
		Date noAntesDe = claims.getNotBeforeTime();
		if (noAntesDe != null && noAntesDe.after(ahora)) {
			throw new IllegalStateException("Token aún no es válido (nbf)");
		}
		return claims;
	}

	private boolean emisorValido(JWTClaimsSet claims) {
		String esperado = recortarONulo(propiedades.getEmisorUri());
		return esperado == null || esperado.equals(claims.getIssuer());
	}

	private boolean audienciaValida(JWTClaimsSet claims) {
		List<String> esperadas = dividir(propiedades.getAudiencias());
		if (esperadas.isEmpty()) {
			return true;
		}
		List<String> audienciasToken = claims.getAudience();
		if (audienciasToken == null || audienciasToken.isEmpty()) {
			return false;
		}
		for (String audienciaEsperada : esperadas) {
			if (audienciasToken.contains(audienciaEsperada)) {
				return true;
			}
		}
		return false;
	}

	private boolean tieneAutoridadRequerida(JWTClaimsSet claims) {
		List<String> scopesRequeridos = dividir(propiedades.getScopesRequeridos());
		List<String> rolesRequeridos = dividir(propiedades.getRolesRequeridos());
		if (scopesRequeridos.isEmpty() && rolesRequeridos.isEmpty()) {
			return true;
		}

		Set<String> scopesToken = new HashSet<>();
		for (String nombreClaim : List.of("scp", "scope", "scopes")) {
			Object valor = claims.getClaim(nombreClaim);
			if (valor instanceof List<?> lista) {
				for (Object item : lista) {
					scopesToken.add(String.valueOf(item));
				}
			} else if (valor != null) {
				scopesToken.add(String.valueOf(valor));
			}
		}

		Set<String> rolesToken = new HashSet<>();
		Object valorRoles = claims.getClaim("roles");
		if (valorRoles instanceof List<?> roles) {
			for (Object rol : roles) {
				rolesToken.add(String.valueOf(rol));
			}
		} else if (valorRoles != null) {
			rolesToken.add(String.valueOf(valorRoles));
		}

		for (String requerido : scopesRequeridos) {
			if (scopesToken.contains(requerido)) {
				return true;
			}
		}
		for (String requerido : rolesRequeridos) {
			if (rolesToken.contains(requerido)) {
				return true;
			}
		}
		return false;
	}

	private List<String> dividir(String csv) {
		List<String> resultado = new ArrayList<>();
		String valor = recortarONulo(csv);
		if (valor == null) {
			return resultado;
		}
		for (String parte : valor.split(",")) {
			String recortada = parte.trim();
			if (!recortada.isEmpty()) {
				resultado.add(recortada);
			}
		}
		return resultado;
	}

	private String recortarONulo(String valor) {
		if (valor == null) {
			return null;
		}
		String recortado = valor.trim();
		return recortado.isEmpty() ? null : recortado;
	}

	private void escribirError(HttpServletResponse response, int status, String mensaje) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + mensaje + "\"}");
	}
}