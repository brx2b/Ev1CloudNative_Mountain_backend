package com.mountainbackend.pedidos.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de seguridad del microservicio de pedidos.
 * Con los datos de Azure AD / Entra ID se habilita la validación JWT en el servicio
 * (defensa en profundidad, además del JWT Authorizer del AWS API Gateway).
 */
@ConfigurationProperties(prefix = "app.seguridad")
public class PropiedadesSeguridad {

	/** Activa/desactiva la validación JWT (false = modo demo sin token). */
	private boolean habilitado = false;

	/** URL del JWKS de Azure. Ej: https://login.microsoftonline.com/{tenant-id}/discovery/v2.0/keys */
	private String jwksUri = "";

	/** Issuer esperado. Ej: https://login.microsoftonline.com/{tenant-id}/v2.0 */
	private String emisorUri = "";

	/** Audiencias aceptadas (separadas por coma). Ej: api://{app-client-id} */
	private String audiencias = "";

	/** Scopes requeridos (separados por coma). Ej: orders.write */
	private String scopesRequeridos = "";

	/** Roles de aplicación requeridos (separados por coma). Ej: Orders.Write */
	private String rolesRequeridos = "";

	public boolean isHabilitado() {
		return habilitado;
	}

	public void setHabilitado(boolean habilitado) {
		this.habilitado = habilitado;
	}

	public String getJwksUri() {
		return jwksUri;
	}

	public void setJwksUri(String jwksUri) {
		this.jwksUri = jwksUri;
	}

	public String getEmisorUri() {
		return emisorUri;
	}

	public void setEmisorUri(String emisorUri) {
		this.emisorUri = emisorUri;
	}

	public String getAudiencias() {
		return audiencias;
	}

	public void setAudiencias(String audiencias) {
		this.audiencias = audiencias;
	}

	public String getScopesRequeridos() {
		return scopesRequeridos;
	}

	public void setScopesRequeridos(String scopesRequeridos) {
		this.scopesRequeridos = scopesRequeridos;
	}

	public String getRolesRequeridos() {
		return rolesRequeridos;
	}

	public void setRolesRequeridos(String rolesRequeridos) {
		this.rolesRequeridos = rolesRequeridos;
	}
}