# Etapa 1: compilar el proyecto con Maven y Java 17 (sin instalar Maven en tu SO)
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Cachear dependencias primero para reconstrucciones rápidas
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copiar código fuente y generar el archivo WAR
COPY src ./src
RUN mvn -B package -DskipTests

# Etapa 2: imagen ligera de ejecución con Apache Tomcat 10.1 (Jakarta Servlet 6.0)
FROM tomcat:10.1-jdk17-temurin

# Limpiar aplicaciones por defecto si existieran y desplegar el WAR
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=builder /app/target/tienda-ecommerce-back.war /usr/local/tomcat/webapps/tienda-ecommerce-back.war

# Script de arranque que genera db.properties desde variables de entorno
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN sed -i 's/\r$//' /usr/local/bin/docker-entrypoint.sh && \
    chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["catalina.sh", "run"]
