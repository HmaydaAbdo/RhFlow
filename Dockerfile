# ─────────────────────────────────────────────────────────────────────────────
# RH Flow — Backend Spring Boot 3.5.11 / Java 21
#
# Multi-stage build :
#   1. builder  : JDK 21 + Maven, télécharge les deps puis package le JAR
#   2. runtime  : JRE 21 alpine, lance le JAR (image finale ~200 MB)
#
# Cache Docker :
#   - Le pom.xml est copié AVANT le code source → `go-offline` se rejoue
#     uniquement quand les deps changent (pas à chaque modif de code)
#
# Sécurité : runtime tourne avec un user non-root.
# ─────────────────────────────────────────────────────────────────────────────

# ============================================================================
# Stage 1 — Build (image officielle Maven 3.9 + JDK 21)
#
# On utilise l'image maven:* plutôt que le wrapper local (mvnw) — comme ça
# le build ne dépend PAS de l'état de .mvn/wrapper côté hôte (souvent
# incomplet ou non versionné).
# ============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom.xml d'abord pour profiter du layer cache : tant que les deps ne
# changent pas, l'étape go-offline ne sera pas rejouée.
COPY pom.xml .

# Pré-télécharge toutes les deps (-B = batch mode, logs concis pour CI/Docker)
RUN mvn dependency:go-offline -B

# Maintenant on copie le source et on package.
# -Dmaven.test.skip=true : skip à la fois la compilation ET l'exécution des tests.
# Les tests doivent tourner en CI / local — pas dans le build Docker.
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -B

# ============================================================================
# Stage 2 — Runtime (JRE seule, image minimale)
# ============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# User non-root pour réduire la surface d'attaque.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Récupère le JAR construit en stage 1.
COPY --from=builder /build/target/*.jar /app/app.jar

# JVM options ajustables via env (heap, GC…) sans rebuilder l'image.
ENV JAVA_OPTS=""

# Le profile staging est activé par défaut quand l'image tourne en Docker.
# docker-compose pourra le surcharger via SPRING_PROFILES_ACTIVE.
ENV SPRING_PROFILES_ACTIVE=staging

EXPOSE 8080

# Healthcheck via l'endpoint /actuator/health (déjà exposé en management).
# Le context-path est /rh (cf. application.yaml server.servlet.context-path).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/rh/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
