FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# COPY pom.xml .
# COPY src ./src

# RUN apt-get update && apt-get install -y maven && \
#    mvn clean package -DskipTests -Dmaven.test.skip=true && \
#    apt-get remove -y maven && \
#    apt-get autoremove -y && \
#    rm -rf /var/lib/apt/lists/*

COPY target/utility-explorer-api-1.0.0.jar app.jar

# Download OpenTelemetry Java Agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

EXPOSE 8080

CMD ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
