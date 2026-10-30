FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
RUN ./mvnw --batch-mode --no-transfer-progress -pl payment-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder --chown=10001:10001 /workspace/payment-service/target/payment-service-*.jar /app/app.jar
ENV PAYMENT_SERVER_PORT=8081
EXPOSE 8081
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
