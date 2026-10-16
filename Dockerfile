# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -B -DskipTests package \
    && find target -maxdepth 1 -name '*.jar' ! -name '*.original' -exec cp {} /workspace/app.jar \;

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home pokehub
COPY --from=build /workspace/app.jar app.jar
USER pokehub

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
