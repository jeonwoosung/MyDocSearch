FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN mkdir -p /app/data/index /data/documents
COPY --from=build /app/target/mail-search-backend-0.0.1-SNAPSHOT.jar /app/app.jar

ENV APP_EML_ROOT=/data/documents
ENV APP_INDEX_DIR=/app/data/index
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
