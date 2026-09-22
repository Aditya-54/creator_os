## Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -q clean package -DskipTests

## Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S creatoros && adduser -S creatoros -G creatoros
COPY --from=build /app/target/*.jar app.jar
USER creatoros

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
