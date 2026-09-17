FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --chown=10001:10001 my-market-app-impl/target/my-market-app-impl-*.jar app.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
