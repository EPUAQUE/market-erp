# syntax=docker/dockerfile:1

# --- Build stage -------------------------------------------------------
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app

# Capa de dependencias separada del código: mientras el pom.xml no cambie,
# Docker reusa esta capa (mvn go-offline) aunque el código sí cambie.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S market && adduser -S market -G market
COPY --from=build /app/target/market-backend-*.jar app.jar
RUN chown market:market app.jar
USER market

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
