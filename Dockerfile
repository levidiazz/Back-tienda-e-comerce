FROM maven:3.9-eclipse-temurin-17 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM tomcat:10.1-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY render/context.xml /usr/local/tomcat/conf/context.xml
COPY --from=compilacion /app/target/tienda-ecommerce-back.war /usr/local/tomcat/webapps/ROOT.war
ENV PORT=8080
ENV CATALINA_OPTS="-XX:MaxRAMPercentage=70"
EXPOSE 8080
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT}\\\"/\" conf/server.xml && exec catalina.sh run"]
