# Multi-stage: the build stage carries Maven and the full JDK, neither of which the
# running application needs. Only the second stage's layers ship in the final image.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies copied and resolved before the source, so an edit to application code
# doesn't invalidate Docker's dependency-download layer cache on the next build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Tests are not run here: they need a live Postgres (Testcontainers), which a Docker
# image build has no access to, and CI already runs the full suite before this image
# is ever built. Skipping them here answers "does this compile and package," not
# "is it correct" — that question is answered elsewhere.
RUN mvn -B -DskipTests package

# Runtime stage: a JRE, not a JDK, since nothing here compiles Java. Alpine keeps the
# image small; the application has no native-library dependency that would need glibc.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Runs as a non-root user: the default Alpine JRE image runs as root, and a container
# with no reason to hold root privileges should not have them.
RUN addgroup -S tamp && adduser -S tamp -G tamp
COPY --from=build /build/target/backend-*.jar app.jar
USER tamp

# No ENV or ARG defaults for DB_URL, DB_USERNAME, DB_PASSWORD, SERVER_PORT, or
# MATCHING_SERVICE_URL: application.yml already reads every one of these from the
# environment with a localhost-friendly default for bare `java -jar` use outside
# Docker. Compose supplies the container-network values; nothing is baked in here.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
