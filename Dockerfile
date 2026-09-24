FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY . .

RUN ./mvnw --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder --chown=10001:10001 \
    /workspace/my-market-app-impl/target/my-market-app-impl-*.jar \
    /app/app.jar

ENV SERVER_PORT=8080
EXPOSE 8080

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
