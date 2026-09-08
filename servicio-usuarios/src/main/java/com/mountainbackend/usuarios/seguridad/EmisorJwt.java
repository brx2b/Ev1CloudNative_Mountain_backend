package com.mountainbackend.usuarios.seguridad;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.mountainbackend.usuarios.configuracion.PropiedadesAutenticacion;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Emite y verifica los JWT HS256 del flujo propio de la tienda (registro/login).
 * El secreto se comparte con servicio-pedidos para que valide el mismo token
 * cuando la validación local está activa (JWT_LOCAL_ENABLED=true).
 */
@Component
public class EmisorJwt {

	private final PropiedadesAutenticacion propiedades;

	public EmisorJwt(PropiedadesAutenticacion propiedades) {
		this.propiedades = propiedades;
	}

	public String emitir(String idUsuario, String email, String nombre, String rol) {
		try {
			long ahoraMs = System.currentTimeMillis();
			long expiraMs = ahoraMs + propiedades.getHorasExpiracion() * 60L * 60L * 1000L;

			JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(idUsuario)
				.issuer(propiedades.getEmisor())
				.audience(propiedades.getAudiencia())
				.issueTime(new Date(ahoraMs))
				.expirationTime(new Date(expiraMs))
				.claim("email", email)
				.claim("name", nombre)
				.claim("roles", rol)
				.claim("scp", propiedades.getScope())
				.build();

			JWSHeader cabecera = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build();
			SignedJWT firmado = new SignedJWT(cabecera, claims);
			firmado.sign(new MACSigner(bytesSecret()));
			return firmado.serialize();
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo generar el token JWT: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Verifica firma, vigencia, emisor y audiencia. Devuelve los claims o lanza
	 * excepción si el token es inválido.
	 */
	public JWTClaimsSet verificar(String token) throws Exception {
		SignedJWT firmado = SignedJWT.parse(token);
		if (!firmado.verify(new MACVerifier(bytesSecret()))) {
			throw new IllegalStateException("Firma JWT no válida");
		}
		JWTClaimsSet claims = firmado.getJWTClaimsSet();
		Date ahora = new Date();
		if (claims.getExpirationTime() == null || claims.getExpirationTime().before(ahora)) {
			throw new IllegalStateException("Token expirado");
		}
		String emisor = claims.getIssuer();
		if (!propiedades.getEmisor().equals(emisor)) {
			throw new IllegalStateException("Emisor inesperado: " + emisor);
		}
		if (claims.getAudience() == null || !claims.getAudience().contains(propiedades.getAudiencia())) {
			throw new IllegalStateException("Audiencia inesperada");
		}
		return claims;
	}

	private byte[] bytesSecret() {
		return propiedades.getSecreto().getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}
}