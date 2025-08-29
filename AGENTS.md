# Evolve Project: Agent Instructions

This document provides instructions for AI agents working on the Evolve project.

## Project Overview

Evolve is an open-source platform for managing electric vehicle charging stations. It is a fork of the SteVe (Steckdosenverwaltung) project. It supports various versions of the Open Charge Point Protocol (OCPP) and provides a web-based interface for administration.

The project is built with Java and Maven. It uses a MySQL or MariaDB database for data storage.

## Architecture and Frameworks

Evolve is designed using a hexagonal architecture (also known as Ports and Adapters or Clean Architecture). This separates the core application logic from the services it consumes and the interfaces it provides.

### Core Module

-   `steve-core`: This is the heart of the application. It contains the core business logic and is independent of any specific technology or framework for its external communication. It defines the "ports" (interfaces) for interacting with the outside world.

### Adapter Modules

The other modules act as "adapters" that implement the ports defined in `steve-core` or drive the application.

-   **Database Adapter:** `steve-jooq` implements the database persistence logic using the jOOQ framework. It connects the core application to a MySQL/MariaDB database.
-   **OCPP Adapters:** The `steve-ocpp-*` modules (`steve-ocpp-soap`, `steve-ocpp-websocket`) handle communication with charging stations using different versions and transports of the OCPP protocol.
-   **UI Adapter:** `steve-ui-jsp` provides a web-based user interface using JavaServer Pages (JSP).

### Key Frameworks

-   **Spring Framework:** Used for dependency injection, managing application components, and transaction management.
-   **Jetty:** An embedded web server that runs the application as a standalone JAR file.
-   **jOOQ:** A framework for building type-safe SQL queries in Java.
-   **Lombok:** A library to reduce boilerplate code for model/data objects.

## Getting Started

The easiest way to get the project up and running is by using Docker Compose. This will set up the application and the required database.

To start the project, run:
```bash
docker compose up -d
```
The web interface will be available at `http://localhost:8180`.

## Building the Project

To build the project from source, you will need:
- JDK 21 or newer
- Maven 3.9.0 or newer
- A running MySQL or MariaDB database

**IMPORTANT:** The build process requires a running database. The build connects to the database to run migrations (using Flyway) and then generates Java code from the schema (using jOOQ).

Before building, you need to configure the database connection. The default configuration is in `steve/src/main/resources/config/main.properties`. You can either edit this file or provide the configuration as command-line arguments.

To build the project, run:
```bash
./mvnw package
```

If you need to override the database configuration, you can do so like this:
```bash
./mvnw package -Ddb.ip=<ip> -Ddb.port=<port> -Ddb.schema=<schema> -Ddb.user=<username> -Ddb.password=<password>
```

A runnable JAR file will be created in `steve/target/`.

## Running Tests

To run the test suite, use the following command:
```bash
./mvnw test
```

## Development Workflow

This project uses tools to enforce code style and license headers. Please adhere to the following workflow.

### Code Formatting

The project uses the `spotless-maven-plugin` to enforce a consistent code style. Before committing any changes, run the following command to format your code:
```bash
./mvnw spotless:apply
```

To check for formatting issues, you can run:
```bash
./mvnw spotless:check
```

### License Headers

All source files must include a license header. The `license-maven-plugin` is used to check for this.

To check for missing license headers, run:
```bash
./mvnw license:check
```

To automatically add license headers to your files, run:
```bash
./mvnw license:format
```
