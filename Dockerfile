FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
