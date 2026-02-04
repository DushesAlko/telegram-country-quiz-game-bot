# Telegram Country Quiz Game Bot

Кратко: Telegram-бот — викторина по странам. Проект на Java (Maven). Можно запускать локально или через Docker + docker-compose.

## Функционал
- Вопросы о странах (столица, флаг, население и т.п.)
- Сохранение результатов в PostgreSQL
- REST-эндпойнт (если есть) для статистики (см. раздел API)

## Требования
- JDK 17
- Maven (если собираешь локально)
- Docker & Docker Compose (для контейнерного разворачивания)
- Telegram Bot Token (от BotFather)
- PostgreSQL (если не используешь docker-compose)

## Быстрый старт (Docker)
1. Создай файл `.env` на основе `.env.example` и заполни значения (особенно `TELEGRAM_BOT_TOKEN`).
2. Запусти:
```bash
docker compose up --build