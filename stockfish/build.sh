#!/bin/sh
# Build Stockfish from source. Run from repo root: ./stockfish/build.sh
# Requires: make, C++17 compiler (g++, clang).
set -e
cd "$(dirname "$0")/src"
make build -j$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 2)
echo "Built: $(pwd)/stockfish"
