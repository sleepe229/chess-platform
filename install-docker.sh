#!/bin/bash
# Скрипт для установки Docker и Docker Compose на Ubuntu

set -e

echo "=========================================="
echo "Установка Docker и Docker Compose"
echo "=========================================="

# Проверка, что скрипт запущен от root
if [ "$EUID" -ne 0 ]; then 
    echo "Ошибка: Запустите скрипт от root или с sudo"
    exit 1
fi

echo ""
echo "Шаг 1: Обновление системы..."
apt-get update
apt-get upgrade -y

echo ""
echo "Шаг 2: Установка зависимостей..."
apt-get install -y ca-certificates curl gnupg lsb-release

echo ""
echo "Шаг 3: Добавление официального GPG ключа Docker..."
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo ""
echo "Шаг 4: Настройка репозитория Docker..."
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

echo ""
echo "Шаг 5: Установка Docker Engine..."
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo ""
echo "Шаг 6: Проверка установки..."
docker --version
docker compose version

echo ""
echo "Шаг 7: Запуск Docker..."
systemctl start docker
systemctl enable docker

echo ""
echo "=========================================="
echo "Docker успешно установлен!"
echo "=========================================="
echo ""
echo "Проверка:"
echo "  docker --version"
echo "  docker compose version"
echo "  docker ps"
echo ""
