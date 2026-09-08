package com.mountainbackend.usuarios.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de emisión de JWT del microservicio de identidad.
 * El token se firma con HS256 usando un secreto compartido que también
 * conoce servicio-pedidos (validación local, además del JWKS de Azure).
 */
@ConfigurationProperties(prefix = "app.auth")
public class PropiedadesAutenticacion {

	/** Secreto HS256 compartido (mínimo 32 bytes recomendado). */
	private String secreto = "";

	/** Emisor (iss) de los tokens emitidos. */
	private String emisor = "pedidos360-usuarios";

	/** Audiencia (aud) de los tokens emitidos. */
	private String audiencia = "pedidos360-api";

	/** Scope (scp) del token local. Debe coincidir con JWT_LOCAL_REQUIRED_SCOPE de servicio-pedidos. */
	private String scope = "orders.write";

	/** Vigencia del token en horas. */
	private long horasExpiracion = 8;

	/** Credenciales del usuario semilla para probar el login sin registrarse. */
	private String usuarioSeedEmail = "demo@summitlab.cl";
	private String usuarioSeedPassword = "demo1234";

	public String getSecreto() {
		return secreto;
	}

	public void setSecreto(String secreto) {
		this.secreto = secreto;
	}

	public String getEmisor() {
		return emisor;
	}

	public void setEmisor(String emisor) {
		this.emisor = emisor;
	}

	public String getAudiencia() {
		return audiencia;
	}

	public void setAudiencia(String audiencia) {
		this.audiencia = audiencia;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public long getHorasExpiracion() {
		return horasExpiracion;
	}

	public void setHorasExpiracion(long horasExpiracion) {
		this.horasExpiracion = horasExpiracion;
	}

	public String getUsuarioSeedEmail() {
		return usuarioSeedEmail;
	}

	public void setUsuarioSeedEmail(String usuarioSeedEmail) {
		this.usuarioSeedEmail = usuarioSeedEmail;
	}

	public String getUsuarioSeedPassword() {
		return usuarioSeedPassword;
	}

	public void setUsuarioSeedPassword(String usuarioSeedPassword) {
		this.usuarioSeedPassword = usuarioSeedPassword;
	}
}