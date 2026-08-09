FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace
COPY gradle ./gradle
COPY gradlew build.gradle settings.gradle ./
COPY src ./src

ARG VERSION=development
RUN ./gradlew clean bootJar \
    --no-daemon \
    --warning-mode=fail \
    -Pversion="${VERSION}" && \
    install -D -m 0644 \
    "build/libs/open-discogs-batch-${VERSION}.jar" \
    /out/open-discogs-batch.jar

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --gid 65532 nonroot && \
    useradd --uid 65532 --gid 65532 --create-home nonroot && \
    install -d -o 65532 -g 65532 /home/nonroot/.cache/open-discogs-batch

COPY --from=build --chown=65532:65532 \
    /out/open-discogs-batch.jar \
    /app/open-discogs-batch.jar

ENV HOME=/home/nonroot
USER 65532:65532
WORKDIR /home/nonroot
ENTRYPOINT ["java", "-jar", "/app/open-discogs-batch.jar"]
