FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S serviceops && adduser -S serviceops -G serviceops
WORKDIR /app
COPY --from=build /workspace/target/serviceops-backend-*.jar /app/serviceops.jar
RUN mkdir -p /app/data/uploads && chown -R serviceops:serviceops /app
USER serviceops
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/urandom","-jar","/app/serviceops.jar"]
