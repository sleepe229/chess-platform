@echo off
REM Build Stockfish on Windows. Requires make + g++ (e.g. from MSYS2 or MinGW).
cd /d "%~dp0src"
make build
echo Built: %~dp0src\stockfish.exe
