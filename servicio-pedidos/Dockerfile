# Imagen de ejecución. Requiere el jar generado por Maven:
#   mvn -pl servicio-pedidos package          (o mvn package en la raíz)
#   docker build -t pedidos360/servicio-pedidos:latest servicio-pedidos/
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
LABEL org.opencontainers.image.title="pedidos360-servicio-pedidos"
LABEL org.opencontainers.image.description="Pedidos360 - Microservicio de Carrito/Órdenes (protegido JWT)"
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENV SERVER_PORT=8080
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]