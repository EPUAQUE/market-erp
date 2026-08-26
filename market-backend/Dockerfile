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
# Creado y con dueño acá (no en el volumen) para que el volumen nombrado de
# docker-compose.yml herede este owner al inicializarse la primera vez — evita
# el mismo permission denied que tuvo el mount de deploy/certs (ver deploy/README.md).
RUN mkdir -p /data/productos-imagenes && chown market:market /data/productos-imagenes
USER market

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
