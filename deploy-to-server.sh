#!/bin/bash
# Скрипт для деплоя Chess Platform на сервер
# Использование: ./deploy-to-server.sh

set -e

echo "=========================================="
echo "Chess Platform - Деплой на сервер"
echo "=========================================="

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Проверка, что скрипт запущен от root или с sudo
if [ "$EUID" -ne 0 ]; then 
    echo -e "${YELLOW}Внимание: Рекомендуется запускать от root или с sudo${NC}"
fi

echo ""
echo "Шаг 1: Проверка Docker..."
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}Docker не установлен. Устанавливаю...${NC}"
    apt-get update
    apt-get install -y ca-certificates curl gnupg
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      tee /etc/apt/sources.list.d/docker.list > /dev/null
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    echo -e "${GREEN}Docker установлен${NC}"
else
    echo -e "${GREEN}Docker уже установлен${NC}"
fi

echo ""
echo "Шаг 2: Проверка Docker Compose..."
if ! docker compose version &> /dev/null; then
    echo -e "${RED}Docker Compose не найден. Установите его вручную.${NC}"
    exit 1
else
    echo -e "${GREEN}Docker Compose установлен${NC}"
fi

echo ""
echo "Шаг 3: Настройка firewall..."
if command -v ufw &> /dev/null; then
    echo "Открываю необходимые порты..."
    ufw allow 22/tcp   # SSH
    ufw allow 80/tcp   # HTTP (frontend)
    ufw allow 443/tcp  # HTTPS (если будет настроен)
    ufw allow 8080/tcp # API Gateway (если нужен прямой доступ)
    echo -e "${GREEN}Firewall настроен${NC}"
else
    echo -e "${YELLOW}UFW не установлен. Настройте firewall вручную.${NC}"
fi

echo ""
echo "Шаг 4: Создание директории для проекта..."
PROJECT_DIR="/opt/chess-platform"
mkdir -p "$PROJECT_DIR"
cd "$PROJECT_DIR"
echo -e "${GREEN}Директория создана: $PROJECT_DIR${NC}"

echo ""
echo "Шаг 5: Клонирование репозитория..."
if [ -d ".git" ]; then
    echo -e "${YELLOW}Репозиторий уже существует. Обновляю...${NC}"
    git pull
else
    read -p "Введите URL репозитория Git (или нажмите Enter для ручного клонирования): " REPO_URL
    if [ ! -z "$REPO_URL" ]; then
        git clone "$REPO_URL" .
    else
        echo -e "${YELLOW}Пропускаю клонирование. Скопируйте проект вручную в $PROJECT_DIR${NC}"
    fi
fi

echo ""
echo "Шаг 6: Настройка переменных окружения..."
cd "$PROJECT_DIR/docker"
if [ ! -f ".env" ]; then
    echo "Создаю .env файл из примера..."
    cp .env.example .env
    
    echo ""
    echo -e "${YELLOW}=== НАСТРОЙКА ПЕРЕМЕННЫХ ОКРУЖЕНИЯ ===${NC}"
    echo "Генерирую случайные значения для секретов..."
    
    # Генерация JWT_SECRET
    JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n')
    
    # Генерация DB_PASSWORD
    DB_PASSWORD=$(openssl rand -base64 16 | tr -d '\n')
    
    # Обновление .env файла
    sed -i "s|DB_PASSWORD=.*|DB_PASSWORD=$DB_PASSWORD|" .env
    sed -i "s|JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|" .env
    
    echo ""
    echo -e "${GREEN}Сгенерированы случайные значения:${NC}"
    echo "DB_PASSWORD: [установлен]"
    echo "JWT_SECRET: [установлен]"
    echo ""
    echo -e "${YELLOW}ВАЖНО: Отредактируйте файл $PROJECT_DIR/docker/.env и установите:${NC}"
    echo "  - CORS_ALLOWED_ORIGINS (ваш домен или IP)"
    echo "  - Другие настройки при необходимости"
    echo ""
    read -p "Нажмите Enter после редактирования .env файла..."
else
    echo -e "${GREEN}.env файл уже существует${NC}"
fi

echo ""
echo "Шаг 7: Запуск сервисов..."
echo "Это может занять несколько минут при первой сборке..."
docker compose up -d --build

echo ""
echo "Шаг 8: Ожидание запуска сервисов..."
sleep 10

echo ""
echo "Шаг 9: Проверка статуса..."
docker compose ps

echo ""
echo "=========================================="
echo -e "${GREEN}Деплой завершен!${NC}"
echo "=========================================="
echo ""
echo "Проверка логов:"
echo "  docker compose -f $PROJECT_DIR/docker/docker-compose.yaml logs -f"
echo ""
echo "Проверка статуса:"
echo "  docker compose -f $PROJECT_DIR/docker/docker-compose.yaml ps"
echo ""
echo "Остановка сервисов:"
echo "  docker compose -f $PROJECT_DIR/docker/docker-compose.yaml down"
echo ""
echo "Frontend доступен на: http://$(hostname -I | awk '{print $1}'):80"
echo "API Gateway доступен на: http://$(hostname -I | awk '{print $1}'):8080"
echo ""
