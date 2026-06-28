FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode --quiet -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --quiet -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 --no-create-home ledger
COPY --from=build --chown=10001:0 \
    /workspace/target/fault-tolerant-payment-ledger-*.jar /app/app.jar

USER 10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
