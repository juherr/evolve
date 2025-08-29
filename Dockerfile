# syntax=docker/dockerfile:1.6
FROM eclipse-temurin:21 AS deps
WORKDIR /code
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8

# Copy only Maven wrapper + .mvn + every pom.xml from the build context,
# without enumerating modules (requires BuildKit).
RUN --mount=type=bind,source=.,target=/src,ro \
    bash -ceu ' \
        mkdir -p /code; \
        cd /src; \
        # copy Maven wrapper if present
        cp mvnw /code/; \
        mkdir -p /code/.mvn && cp -R .mvn/. /code/.mvn/; \
        # copy all pom.xml preserving directory structure
        while IFS= read -r -d "" p; do \
            mkdir -p /code/"$(dirname "$p")"; \
            cp "$p" /code/"$p"; \
        done < <(find . -name pom.xml -type f -print0); \
    '

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -DskipTests \
      -DoutputAbsoluteArtifactFilename=true \
      -DincludePlugins=true \
      org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline

FROM eclipse-temurin:21 AS builder
WORKDIR /code
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8

ARG DB_HOST
ARG DB_PORT
ARG DB_DATABASE
ARG DB_USER
ARG DB_PASSWORD

COPY . /code

# Download and install dockerize.
# Needed so the web container will wait for MariaDB to start.
# Wait for the db to startup(via dockerize), then
# Build steve, requires a db to be available on port 3306
ARG PORT=8180
ARG DOCKERIZE_VERSION=v0.19.0
RUN curl -sfL "https://github.com/powerman/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-$(uname -s)-$(uname -m)" \
    | install /dev/stdin /usr/local/bin/dockerize

RUN --mount=type=cache,target=/root/.m2 \
    dockerize -wait tcp://${DB_HOST}:${DB_PORT} -timeout 60s \
  && ./mvnw clean package \
	  -DskipTests \
	  -Dhttp.port=${PORT} \
	  -Dserver.host="0.0.0.0" \
	  -Ddb.ip="${DB_HOST}" \
	  -Ddb.port=${DB_PORT} \
	  -Ddb.schema="${DB_DATABASE}" \
	  -Ddb.user="${DB_USER}" \
	  -Ddb.password="${DB_PASSWORD}" \
	  -Dserver.gzip.enabled=false \
	  -Dappender="CONSOLE" \
	  -Djdk.tls.client.protocols="TLSv1,TLSv1.1,TLSv1.2"

FROM eclipse-temurin:21
WORKDIR /app
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8

ARG PORT=8180

COPY --from=builder /code/steve/target/steve.jar /app/
COPY --from=builder /code/steve/target/libs/ /app/libs/

EXPOSE ${PORT}
EXPOSE 8443

CMD ["sh", "-c", "exec java -XX:MaxRAMPercentage=85 -jar /app/steve.jar"]
