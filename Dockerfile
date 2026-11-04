# Version web (wasmJs) d'aglae-form pour Railway.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
# Railway transmet les variables du service déclarées ici.
ARG AGLAE_API_URL
ARG DASHBOARD_API_URL
RUN chmod +x gradlew && AGLAE_API_URL="${AGLAE_API_URL:-https://web-production-b371d.up.railway.app}" DASHBOARD_API_URL="${DASHBOARD_API_URL:-https://sdp-dashboard-back-production.up.railway.app}" ./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon --console=plain

FROM nginx:1.27-alpine
ENV PORT=8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/composeApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html
