# =====================================================================
#  Dockerfile multi-stage pour core-metier (Quarkus, build GRADLE, mode JVM)
#  Stage 1 : compile avec Gradle (pas besoin de Gradle sur la machine !)
#  Stage 2 : image legere qui execute le "fast-jar"
# =====================================================================

# ---------- Stage 1 : build ----------
FROM gradle:9.3.1-jdk21 AS build
WORKDIR /app

# On copie d'abord les fichiers de build seuls pour profiter du cache Docker :
# tant qu'ils ne changent pas, les dependances ne sont pas retelechargees.
COPY build.gradle settings.gradle gradle.properties ./
RUN gradle build -x test --no-daemon || true

# Puis le code source, et on package (tests ignores ici)
COPY src ./src
RUN gradle build -x test --no-daemon

# ---------- Stage 2 : runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Quarkus produit un "fast-jar" reparti dans build/quarkus-app (Gradle)
COPY --from=build /app/build/quarkus-app/lib/ ./lib/
COPY --from=build /app/build/quarkus-app/*.jar ./
COPY --from=build /app/build/quarkus-app/app/ ./app/
COPY --from=build /app/build/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
