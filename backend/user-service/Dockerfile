# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache parent and module descriptors
COPY pom.xml .
COPY backend/common-lib/pom.xml backend/common-lib/
COPY backend/api-gateway/pom.xml backend/api-gateway/
COPY backend/auth-service/pom.xml backend/auth-service/
COPY backend/user-service/pom.xml backend/user-service/
COPY backend/course-service/pom.xml backend/course-service/
COPY backend/media-service/pom.xml backend/media-service/

# Build and install common library
COPY backend/common-lib/src backend/common-lib/src
RUN mvn -B -pl backend/common-lib install -DskipTests

# Build User Service
COPY backend/user-service/src backend/user-service/src
RUN mvn -B -pl backend/user-service package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /workspace/backend/user-service/target/*.jar app.jar
USER app

EXPOSE 8082
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
