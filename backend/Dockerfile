FROM maven:3.9.9-eclipse-temurin-17
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests package

RUN mkdir -p /app/data/index /data/documents /app/db \
    && cp /app/target/mail-search-backend-0.0.1-SNAPSHOT.jar /app/app.jar

ENV APP_EML_ROOT=/data/documents
ENV APP_INDEX_DIR=/app/data/index
ENV APP_DB_PATH=/app/db/mailsearch
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
