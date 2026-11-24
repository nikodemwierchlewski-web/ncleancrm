FROM eclipse-temurin:17-jdk@sha256:7a0b2bc7e1372bb05ee361fb79e8a9e822df47343b8716253b3bbaa75060653e AS build
WORKDIR /build
# The Wrapper pins the ZIP checksum. Without unzip it switches to tar.gz,
# whose different checksum must not be mistaken for a broken download.
RUN apt-get update && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/main/ src/main/
COPY scripts/InitializeLocal.java docker/pc/Healthcheck.java tools/
# Build inside Docker; verification is a separate release step, not silently
# represented by this packaging command.
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package \
    && mkdir -p /build/extracted /build/tools/lib \
    && cd /build/extracted && jar -xf ../target/nclean-crm-0.0.1-SNAPSHOT.jar \
    && cp BOOT-INF/lib/spring-security-crypto-*.jar BOOT-INF/lib/spring-jcl-*.jar /build/tools/lib/ \
    && javac -cp '/build/tools/lib/*' -d /build/tools /build/tools/*.java

FROM eclipse-temurin:17-jdk@sha256:7a0b2bc7e1372bb05ee361fb79e8a9e822df47343b8716253b3bbaa75060653e AS runtime
WORKDIR /app
RUN mkdir /workspace
COPY --from=build /build/target/nclean-crm-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --from=build /build/tools/ /opt/nclean-tools/
USER 1000:1000
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=6 \
  CMD ["java", "-cp", "/opt/nclean-tools", "Healthcheck"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
