# Dedicated Stockfish image. Build once; analytics copies the binary (no C++ in analytics build).
# Build from repo root: docker build -t chess-stockfish:latest -f docker/stockfish/Dockerfile .

# -----------------------------------------------------------------------------
# Stage: build Stockfish from repo stockfish/
# -----------------------------------------------------------------------------
FROM alpine:3.19 AS builder
RUN apk add --no-cache make g++
COPY stockfish /stockfish
WORKDIR /stockfish/src
RUN chmod +x ../scripts/*.sh 2>/dev/null || true
RUN make net || true
RUN make build -j$(nproc 2>/dev/null || echo 2) EXE=stockfish

# -----------------------------------------------------------------------------
# Stage: minimal image with only the binary (for COPY --from= in analytics)
# -----------------------------------------------------------------------------
FROM alpine:3.19
COPY --from=builder /stockfish/src/stockfish /stockfish
RUN chmod 755 /stockfish
# Image is only used as binary source; no need to run Stockfish as a service.
ENTRYPOINT ["/stockfish"]
