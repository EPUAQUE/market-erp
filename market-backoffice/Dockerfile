# syntax=docker/dockerfile:1

# --- Build stage -------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /app

RUN npm install -g pnpm@11

# Capa de dependencias separada del código: mientras package.json/lockfile
# no cambien, Docker reusa esta capa aunque el código sí cambie.
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .

# Vite empaca VITE_* en el bundle en build-time (no en runtime) — debe ser
# la URL PÚBLICA del backend (la que el navegador del usuario puede
# resolver), nunca un nombre de servicio interno de docker compose como
# "backend". Ver deploy/README.md.
ARG VITE_API_BASE_URL
ARG VITE_API_TIMEOUT=15000
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_API_TIMEOUT=${VITE_API_TIMEOUT}
RUN pnpm run build

# --- Runtime stage -------------------------------------------------------
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
