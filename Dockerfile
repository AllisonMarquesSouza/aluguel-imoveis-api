FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build --chown=10001:10001 \
    /workspace/target/meu-imovel-0.0.1-SNAPSHOT.jar /app/app.jar
RUN mkdir -p /app/uploads && chown -R 10001:10001 /app/uploads

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
