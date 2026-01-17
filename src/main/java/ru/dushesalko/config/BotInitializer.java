package ru.dushesalko.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.dushesalko.bot.TelegramBot;

/**
 * Инициализация и регистрация Telegram бота
 *
 * Этот класс регистрирует бота в Telegram API при старте приложения
 */
@Component
@Slf4j
public class BotInitializer {

    private final TelegramBot telegramBot;

    public BotInitializer(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    /**
     * Регистрация бота при старте приложения
     *
     * @EventListener - метод вызывается когда Spring Context полностью инициализирован
     */
    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        log.info("========================================");
        log.info("Registering Telegram bot...");
        log.info("========================================");

        try {
            // Создать API для работы с Telegram
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Зарегистрировать нашего бота
            botsApi.registerBot(telegramBot);

            log.info("✓ Telegram bot registered successfully!");
            log.info("Bot username: @{}", telegramBot.getBotUsername());
            log.info("Bot is now listening for messages...");
            log.info("========================================");

        } catch (TelegramApiException e) {
            log.error("========================================");
            log.error("✗ Failed to register Telegram bot!", e);
            log.error("Error message: {}", e.getMessage());
            log.error("========================================");
            log.error("Possible reasons:");
            log.error("1. Invalid bot token - check application-secrets.yml");
            log.error("2. Network issues - check internet connection");
            log.error("3. Telegram API is down");
            log.error("========================================");
        }
    }
}