# ===== Build stage =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r bookstore && useradd -r -g bookstore bookstore
RUN mkdir -p /app/uploads/images /app/logs && chown -R bookstore:bookstore /app

COPY --from=build /app/target/*.jar app.jar
RUN chown bookstore:bookstore app.jar

USER bookstore

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar", \
    "--spring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}"]
