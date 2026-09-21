# === Build ===
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

# === Run ===
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs

COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Xss256k"
ENV SPRING_PROFILES_ACTIVE=production

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]