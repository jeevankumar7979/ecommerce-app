FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget && addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --chown=app:app api-gateway/build/libs/api-gateway-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
