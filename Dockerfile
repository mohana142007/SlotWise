FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/slotwise-1.0.0.jar /app/slotwise.jar

ENV PORT=8081 \
    SERVER_ADDRESS=0.0.0.0

EXPOSE 8081
USER 10001

ENTRYPOINT ["java", "-jar", "/app/slotwise.jar"]
