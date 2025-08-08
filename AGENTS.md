# Evolve Agent Guide

This document provides instructions for agents to understand, build, test, and run the Evolve project.

## Project Overview

Evolve is an open-source management system for electric vehicle charging stations. It supports various OCPP versions and provides basic functions for administering charge points, users, and RFID cards.

- **Language:** Java 17
- **Build Tool:** Maven
- **Database:** MySQL or MariaDB

## Building the Project

The project is built using the Maven wrapper (`mvnw`).

**Important:** The build process requires Docker to be installed and running. This is because the build uses Testcontainers to spin up a database for jOOQ source code generation.

To compile the project and package it into a runnable JAR file, run the following command from the root directory:

```bash
./mvnw package
```

This will create the file `target/steve.jar`.

## Testing the Project

The project has a suite of integration tests that require a database connection. The tests are configured to run against a test-specific database schema.

To run the tests, you first need to set up a database and then run the Maven `verify` command.

### 1. Database Setup

The tests expect a MySQL or MariaDB database. The following environment variables must be set to configure the database connection for the tests:

- `DB_USER`: The username for the database.
- `DB_PASSWORD`: The password for the database user.
- `DB_SCHEMA`: The name of the database schema to use for testing.

The tests will automatically create the schema, but the user must have the necessary permissions. The following MySQL commands can be used to create the user and grant the required privileges:

```sql
CREATE USER 'testuser'@'%' IDENTIFIED BY 'testpass';
CREATE DATABASE test_schema;
GRANT ALL PRIVILEGES ON test_schema.* TO 'testuser'@'%';
```

### 2. Running the Tests

With the database set up and the environment variables exported, run the following command to execute the test suite:

```bash
export DB_USER=testuser
export DB_PASSWORD=testpass
export DB_SCHEMA=test_schema

./mvnw -B -V -Dmaven.javadoc.skip=true \
  -Ddb-user=$DB_USER \
  -Ddb-password=$DB_PASSWORD \
  -Ddb-schema=$DB_SCHEMA \
  -Ptest verify --file pom.xml
```

## Running the Application

To run the application locally after building it, use the following command:

```bash
java -jar target/steve.jar
```

The application will start and be accessible at `http://localhost:8080/steve/manager` by default.

## Running with Docker

The project includes a `docker-compose.yml` file for easy setup with Docker.

1.  **Configuration:** Before running, you may need to adjust the settings in `src/main/resources/application-docker.properties`.
2.  **Run:** Use Docker Compose to build and start the application and a database container:

    ```bash
    docker compose up -d
    ```

## Configuration

- **Main Configuration:** `src/main/resources/application.properties`
- **Docker Configuration:** `src/main/resources/application-docker.properties`
- **Kubernetes Configuration:** `src/main/resources/application-kubernetes.properties`
- **Database Migrations:** `src/main/resources/db/migration/`
- **Maven Project Configuration:** `pom.xml`
- **CI/CD Pipeline:** `.github/workflows/main.yml`
