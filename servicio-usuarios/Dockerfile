# Imagen de ejecución. Requiere el jar generado por Maven:
#   mvn -pl servicio-usuarios package          (o mvn package en la raíz)
#   docker build -t pedidos360/servicio-usuarios:latest servicio-usuarios/
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
LABEL org.opencontainers.image.title="pedidos360-servicio-usuarios"
LABEL org.opencontainers.image.description="Pedidos360 - Microservicio de Identidad (registro/login y JWT)"
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]