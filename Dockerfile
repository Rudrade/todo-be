# Run
FROM amazoncorretto:21
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
EXPOSE 8080

#HealthCheck
HEALTHCHECK CMD curl -f -v http://localhost:8080/health || exit 1