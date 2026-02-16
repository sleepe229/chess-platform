# Руководство по деплою Chess Platform

## Подготовка к production деплою

### 1. Переменные окружения

Создайте файл `.env` в директории `docker/` на основе `docker/.env.example` и установите **безопасные значения** для production:

```bash
# Обязательные переменные для production:
DB_PASSWORD=<сильный-пароль-минимум-16-символов>
JWT_SECRET=<случайная-строка-минимум-32-символа-256-бит>
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
SPRING_PROFILES_ACTIVE=prod,docker
```

**ВАЖНО:**
- Никогда не коммитьте файл `.env` в git (он уже в `.gitignore`)
- Используйте сильные пароли и секреты
- Генерируйте JWT_SECRET с помощью: `openssl rand -base64 32`

### 2. Безопасность

#### NATS порты
По умолчанию NATS порты (4222, 8222) **закомментированы** в `docker-compose.yaml` для безопасности. 
Если нужен внешний доступ для мониторинга, раскомментируйте и используйте firewall/VPN.

#### Порты сервисов
В production рекомендуется:
- Открывать только порты Gateway (8080) и Frontend (3000/80)
- Остальные сервисы должны быть доступны только внутри Docker network
- Используйте reverse proxy (nginx/traefik) перед Gateway

### 3. Health Checks

Все сервисы имеют настроенные health checks:
- Spring Boot сервисы: `/actuator/health`
- Frontend (nginx): проверка доступности главной страницы
- Инфраструктура (PostgreSQL, Redis, NATS): встроенные проверки

Проверить статус:
```bash
docker compose -f docker/docker-compose.yaml ps
```

### 4. Мониторинг

Все Spring Boot сервисы экспортируют метрики Prometheus:
- Endpoint: `/actuator/prometheus`
- Health: `/actuator/health`

Настройте сбор метрик в Prometheus/Grafana для production.

### 5. Логирование

Логи доступны через:
```bash
docker compose -f docker/docker-compose.yaml logs -f [service-name]
```

Для production рекомендуется настроить централизованное логирование (ELK, Loki, etc.).

### 6. Базы данных

#### Backup
Настройте регулярные бэкапы для всех PostgreSQL баз данных:
```bash
# Пример бэкапа
docker exec chess-postgres-auth pg_dump -U postgres chess_auth > backup_auth.sql
```

#### Миграции
Flyway автоматически применяет миграции при старте сервисов. Убедитесь, что:
- Все миграции протестированы
- Есть план отката при необходимости
- Бэкапы созданы перед деплоем

### 7. Деплой

#### Локальный деплой (для тестирования)
```bash
cd docker
cp .env.example .env
# Отредактируйте .env с production значениями
docker compose up -d --build
```

#### Production деплой

1. **Подготовка сервера:**
   - Docker и Docker Compose установлены
   - Порты открыты в firewall (только необходимые)
   - SSL сертификаты настроены (через reverse proxy)

2. **Клонирование репозитория:**
   ```bash
   git clone <repo-url>
   cd chess/docker
   ```

3. **Настройка переменных:**
   ```bash
   cp .env.example .env
   # Отредактируйте .env
   ```

4. **Сборка и запуск:**
   ```bash
   docker compose up -d --build
   ```

5. **Проверка:**
   ```bash
   docker compose ps
   docker compose logs -f
   ```

### 8. Обновление

Для обновления:
```bash
git pull
docker compose -f docker/docker-compose.yaml up -d --build
```

### 9. Откат

Если что-то пошло не так:
```bash
# Остановить все сервисы
docker compose -f docker/docker-compose.yaml down

# Восстановить из бэкапа БД
docker exec -i chess-postgres-auth psql -U postgres chess_auth < backup_auth.sql

# Вернуться к предыдущей версии через git
git checkout <previous-commit>
docker compose -f docker/docker-compose.yaml up -d --build
```

### 10. Масштабирование

Для горизонтального масштабирования:
- Используйте Docker Swarm или Kubernetes
- Настройте load balancer перед Gateway
- Используйте внешний Redis для shared state
- Настройте shared storage для PostgreSQL (если нужно)

### 11. Troubleshooting

#### Сервис не запускается
```bash
docker compose logs [service-name]
docker compose ps
```

#### Health check падает
```bash
docker exec [container-name] curl -f http://localhost:[port]/actuator/health
```

#### Проблемы с подключением к БД
- Проверьте переменные окружения
- Убедитесь, что БД запущена и здорова
- Проверьте сеть Docker: `docker network inspect chess-network`

### 12. Production Checklist

- [ ] Все секреты установлены в `.env`
- [ ] JWT_SECRET достаточно длинный (минимум 32 символа)
- [ ] DB_PASSWORD сильный
- [ ] CORS настроен правильно
- [ ] NATS порты закрыты от внешнего доступа
- [ ] Health checks работают
- [ ] Метрики собираются
- [ ] Логи настроены
- [ ] Бэкапы БД настроены
- [ ] SSL сертификаты настроены
- [ ] Firewall настроен
- [ ] Мониторинг настроен
- [ ] Документация обновлена
