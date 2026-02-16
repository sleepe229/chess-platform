# -----------------------------------------------------------------------------
# Stage: build Stockfish from repo stockfish/
# -----------------------------------------------------------------------------
FROM alpine:3.19 AS stockfish-builder
RUN apk add --no-cache make g++ bash curl
# Copy stockfish source code
COPY stockfish/src /stockfish/src
WORKDIR /stockfish/src
# Set SHELL environment variable
ENV SHELL=/bin/bash
# Download NNUE networks required by this Stockfish version (see src/evaluate.h)
RUN curl -sSL https://tests.stockfishchess.org/api/nn/nn-c288c895ea92.nnue -o nn-c288c895ea92.nnue && \
    curl -sSL https://tests.stockfishchess.org/api/nn/nn-37f18f62d772.nnue -o nn-37f18f62d772.nnue
# Create scripts dir and mock net.sh so "make build" (and net target) can run; then build
RUN mkdir -p /stockfish/scripts && \
    printf '%s\n' '#!/bin/sh' 'echo "Network already downloaded"' > /stockfish/scripts/net.sh && \
    chmod +x /stockfish/scripts/net.sh && \
    make ARCH=x86-64 build -j$(nproc 2>/dev/null || echo 2) EXE=stockfish SUPPORTED_ARCH=true

# -----------------------------------------------------------------------------
# Stage: build Java app
# -----------------------------------------------------------------------------
FROM bellsoft/liberica-openjdk-alpine:21-cds AS builder
RUN apk add --no-cache ca-certificates && update-ca-certificates
WORKDIR /application
COPY . .
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw && ./mvnw clean install -DskipTests -pl chess-analytics-service -am

FROM bellsoft/liberica-openjre-alpine:21-cds AS layers
WORKDIR /application
COPY --from=builder /application/chess-analytics-service/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# -----------------------------------------------------------------------------
# Stage: final image (JRE + Stockfish binary)
# -----------------------------------------------------------------------------
FROM bellsoft/liberica-openjre-alpine:21-cds

VOLUME /tmp

RUN apk add --no-cache ca-certificates && update-ca-certificates
RUN adduser -S spring-user

# Stockfish for analysis
COPY --from=stockfish-builder /stockfish/src/stockfish /application/stockfish
RUN chmod 755 /application/stockfish

USER spring-user

WORKDIR /application

COPY --from=layers /application/extracted/dependencies/ ./
COPY --from=layers /application/extracted/spring-boot-loader/ ./
COPY --from=layers /application/extracted/snapshot-dependencies/ ./
COPY --from=layers /application/extracted/application/ ./

RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar app.jar || true

ENV JAVA_CDS_OPTS="-XX:SharedArchiveFile=app.jsa -Xlog:class+load:file=/tmp/classload.log"
ENV JAVA_ERROR_FILE_OPTS="-XX:ErrorFile=/tmp/java_error.log"
ENV JAVA_HEAP_DUMP_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"
ENV JAVA_ON_OUT_OF_MEMORY_OPTS="-XX:+ExitOnOutOfMemoryError"
# Analysis engine: use Stockfish built into image
ENV ANALYSIS_ENGINE_ENABLED=true
ENV ANALYSIS_ENGINE_PATH=/application/stockfish
EXPOSE 8086

ENTRYPOINT exec java \
    $JAVA_HEAP_DUMP_OPTS \
    $JAVA_ON_OUT_OF_MEMORY_OPTS \
    $JAVA_ERROR_FILE_OPTS \
    $JAVA_CDS_OPTS \
    -jar app.jar
