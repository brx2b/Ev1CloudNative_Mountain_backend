# Imagen de ejecución. Requiere el jar generado por Maven:
#   mvn -pl servicio-gateway package          (o mvn package en la raíz)
#   docker build -t pedidos360/servicio-gateway:latest servicio-gateway/
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
LABEL org.opencontainers.image.title="pedidos360-servicio-gateway"
LABEL org.opencontainers.image.description="Pedidos360 - API Gateway (enrutamiento + registro de endpoints)"
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]