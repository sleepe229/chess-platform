# Деплой на сервер 91.197.97.45

## Быстрый старт

### Вариант 1: Автоматический деплой (рекомендуется)

1. **Скопируйте скрипт на сервер:**
   ```bash
   # На вашем локальном компьютере
   scp deploy-to-server.sh root@91.197.97.45:/root/
   ```

2. **Подключитесь к серверу:**
   ```bash
   ssh root@91.197.97.45
   ```

3. **Запустите скрипт:**
   ```bash
   chmod +x deploy-to-server.sh
   ./deploy-to-server.sh
   ```

4. **Отредактируйте .env файл:**
   ```bash
   nano /opt/chess-platform/docker/.env
   ```
   
   Установите:
   - `CORS_ALLOWED_ORIGINS=http://91.197.97.45,http://91.197.97.45:80` (или ваш домен)
   - Проверьте остальные настройки

5. **Перезапустите сервисы:**
   ```bash
   cd /opt/chess-platform/docker
   docker compose down
   docker compose up -d
   ```

### Вариант 2: Ручной деплой

#### Шаг 1: Подготовка сервера

```bash
ssh root@91.197.97.45
```

#### Шаг 2: Установка Docker и Docker Compose

```bash
# Обновление системы
apt-get update
apt-get upgrade -y

# Установка Docker
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

# Проверка установки
docker --version
docker compose version
```

#### Шаг 3: Настройка Firewall

```bash
# Установка UFW (если не установлен)
apt-get install -y ufw

# Открытие портов
ufw allow 22/tcp   # SSH
ufw allow 80/tcp   # HTTP (Frontend)
ufw allow 443/tcp  # HTTPS (для будущего SSL)
ufw allow 8080/tcp # API Gateway (опционально)

# Включение firewall
ufw --force enable
ufw status
```

#### Шаг 4: Клонирование репозитория

```bash
# Создание директории
mkdir -p /opt/chess-platform
cd /opt/chess-platform

# Клонирование (замените на ваш репозиторий)
# Если репозиторий приватный, используйте SSH ключи или токены
git clone <ваш-repo-url> .

# Или скопируйте проект вручную через scp с локального компьютера:
# scp -r /home/stepan/IdeaProjects/project/chess root@91.197.97.45:/opt/chess-platform
```

#### Шаг 5: Настройка переменных окружения

```bash
cd /opt/chess-platform/docker
cp .env.example .env
nano .env
```

**Обязательно установите:**

```bash
# База данных
DB_USER=postgres
DB_PASSWORD=<сгенерируйте-сильный-пароль>
DB_PORT=5432

# JWT секрет (минимум 32 символа)
JWT_SECRET=<сгенерируйте-через-openssl-rand-base64-32>

# CORS (ваш IP или домен)
CORS_ALLOWED_ORIGINS=http://91.197.97.45,http://91.197.97.45:80

# Spring профиль
SPRING_PROFILES_ACTIVE=docker

# Порты (можно оставить по умолчанию)
GATEWAY_PORT=8080
FRONTEND_PORT=80
AUTH_SERVICE_PORT=8081
USER_SERVICE_PORT=8082
MATCHMAKING_SERVICE_PORT=8083
GAME_SERVICE_PORT=8084
WS_SERVICE_PORT=8085
ANALYTICS_SERVICE_PORT=8086
```

**Генерация секретов:**

```bash
# Генерация JWT_SECRET
openssl rand -base64 32

# Генерация DB_PASSWORD
openssl rand -base64 16
```

#### Шаг 6: Запуск сервисов

```bash
cd /opt/chess-platform/docker
docker compose up -d --build
```

**Первая сборка может занять 10-15 минут.**

#### Шаг 7: Проверка статуса

```bash
# Проверка статуса всех контейнеров
docker compose ps

# Просмотр логов
docker compose logs -f

# Проверка конкретного сервиса
docker compose logs chess-api-gateway
docker compose logs chess-frontend
```

#### Шаг 8: Проверка доступности

```bash
# Проверка health checks
curl http://localhost:8080/actuator/health
curl http://localhost:80/

# Проверка извне (с вашего локального компьютера)
curl http://91.197.97.45:80/
curl http://91.197.97.45:8080/actuator/health
```

## Управление сервисами

### Просмотр логов

```bash
cd /opt/chess-platform/docker

# Все логи
docker compose logs -f

# Конкретный сервис
docker compose logs -f chess-api-gateway
docker compose logs -f chess-frontend
docker compose logs -f chess-auth-service
```

### Остановка/Запуск

```bash
cd /opt/chess-platform/docker

# Остановка
docker compose down

# Запуск
docker compose up -d

# Перезапуск
docker compose restart

# Перезапуск конкретного сервиса
docker compose restart chess-api-gateway
```

### Обновление

```bash
cd /opt/chess-platform

# Обновление кода
git pull

# Пересборка и перезапуск
cd docker
docker compose up -d --build
```

## Troubleshooting

### Сервис не запускается

```bash
# Проверка логов
docker compose logs [service-name]

# Проверка статуса
docker compose ps

# Проверка ресурсов
docker stats
```

### Проблемы с портами

```bash
# Проверка занятых портов
netstat -tulpn | grep LISTEN

# Проверка firewall
ufw status
```

### Проблемы с базой данных

```bash
# Проверка подключения к БД
docker exec -it chess-postgres-auth psql -U postgres -d chess_auth

# Проверка логов БД
docker compose logs postgres-auth
```

### Очистка и пересборка

```bash
cd /opt/chess-platform/docker

# Остановка и удаление контейнеров
docker compose down

# Удаление образов (опционально)
docker compose down --rmi all

# Очистка volumes (ОСТОРОЖНО: удалит данные БД!)
# docker compose down -v

# Пересборка с нуля
docker compose build --no-cache
docker compose up -d
```

## Бэкапы

### Создание бэкапа БД

```bash
# Создание директории для бэкапов
mkdir -p /opt/backups/chess-platform

# Бэкап всех баз данных
docker exec chess-postgres-auth pg_dump -U postgres chess_auth > /opt/backups/chess-platform/auth_$(date +%Y%m%d_%H%M%S).sql
docker exec chess-postgres-user pg_dump -U postgres chess_user > /opt/backups/chess-platform/user_$(date +%Y%m%d_%H%M%S).sql
docker exec chess-postgres-game pg_dump -U postgres chess_game > /opt/backups/chess-platform/game_$(date +%Y%m%d_%H%M%S).sql
docker exec chess-postgres-matchmaking pg_dump -U postgres chess_matchmaking > /opt/backups/chess-platform/matchmaking_$(date +%Y%m%d_%H%M%S).sql
docker exec chess-postgres-analytics pg_dump -U postgres chess_analytics > /opt/backups/chess-platform/analytics_$(date +%Y%m%d_%H%M%S).sql
```

### Восстановление из бэкапа

```bash
# Восстановление БД
docker exec -i chess-postgres-auth psql -U postgres chess_auth < /opt/backups/chess-platform/auth_YYYYMMDD_HHMMSS.sql
```

## Настройка домена (опционально)

Если у вас есть домен, настройте DNS:

1. Создайте A-запись, указывающую на `91.197.97.45`
2. Установите nginx как reverse proxy:

```bash
apt-get install -y nginx certbot python3-certbot-nginx

# Создайте конфигурацию nginx
nano /etc/nginx/sites-available/chess-platform
```

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ws/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

```bash
# Активация конфигурации
ln -s /etc/nginx/sites-available/chess-platform /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx

# Получение SSL сертификата
certbot --nginx -d your-domain.com
```

## Мониторинг

### Проверка использования ресурсов

```bash
docker stats
```

### Проверка метрик Prometheus

```bash
curl http://localhost:8080/actuator/prometheus
```

## Контакты и поддержка

При возникновении проблем проверьте:
1. Логи сервисов: `docker compose logs`
2. Статус контейнеров: `docker compose ps`
3. Использование ресурсов: `docker stats`
4. Firewall: `ufw status`
