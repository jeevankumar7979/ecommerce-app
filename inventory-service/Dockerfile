FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget && addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --chown=app:app inventory-service/build/libs/inventory-service-1.0.0.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
