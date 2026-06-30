# =====================================================================
#  Dockerfile multi-stage pour le backend Quarkus (mode JVM)
#  Stage 1 : build Maven (pas besoin de Maven installé sur la machine)
#  Stage 2 : image légère qui exécute le "fast-jar"
# =====================================================================

# ---------- Stage 1 : build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Le pom seul d'abord → cache des dépendances tant qu'il ne change pas.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2 : runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/quarkus-app/lib/ ./lib/
COPY --from=build /app/target/quarkus-app/*.jar ./
COPY --from=build /app/target/quarkus-app/app/ ./app/
COPY --from=build /app/target/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
