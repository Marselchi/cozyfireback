# syntax=docker/dockerfile:1


# === Build ===
FROM eclipse-temurin:27-jdk-alpine AS build
WORKDIR /build

# Fail early if the supplied image is not actually Java 27.
RUN java -XshowSettings:properties -version 2>&1 \
    | grep -Eq '^[[:space:]]*java\.specification\.version = 27$'

# Copy build metadata before source code to preserve dependency caching.
COPY pom.xml ./
COPY --chmod=0755 mvnw ./mvnw
COPY .mvn/ .mvn/

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -ntp dependency:go-offline

COPY src/ src/

# Use an explicit artifact instead of an ambiguous target/*.jar.
ARG JAR_FILE=target/app.jar

# Tests run by default.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -ntp package \
    && cp "${JAR_FILE}" application.jar \
    && java -Djarmode=tools -jar application.jar \
         extract --layers --destination extracted

# === Runtime ===
FROM eclipse-temurin:27-jre-alpine AS runtime
WORKDIR /app

RUN java -XshowSettings:properties -version 2>&1 \
    | grep -Eq '^[[:space:]]*java\.specification\.version = 27$' \
    && mkdir -p /app/logs \
    && chown 1001:1001 /app/logs

# Least frequently changing content first; application code last.
# Application files remain root-owned and readable by the runtime user.
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=production

USER 1001:1001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]