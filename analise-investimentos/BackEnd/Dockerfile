FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/analise-investimentos-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080
USER 10001

ENTRYPOINT ["java", "-jar", "app.jar"]
