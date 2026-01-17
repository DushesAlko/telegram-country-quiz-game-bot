package ru.dushesalko.bot;

import ru.dushesalko.config.BotConfig;
import ru.dushesalko.dto.CountryDTO;
import ru.dushesalko.model.GameSession;
import ru.dushesalko.model.User;
import ru.dushesalko.service.CountryService;
import ru.dushesalko.service.GameService;
import ru.dushesalko.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Главный класс Telegram бота
 * <p>
 * TelegramLongPollingBot - базовый класс для Long Polling режима
 * Long Polling - бот постоянно опрашивает Telegram сервер на наличие новых сообщений
 * <p>
 * Альтернатива: TelegramWebhookBot (для Webhook режима)
 */
@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final UserService userService;
    private final GameService gameService;
    private final CountryService countryService;

    /**
     * Конструктор бота
     */
    public TelegramBot(BotConfig botConfig,
                       UserService userService,
                       GameService gameService,
                       CountryService countryService) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.userService = userService;
        this.gameService = gameService;
        this.countryService = countryService;

        log.info("========================================");
        log.info("Telegram bot initialized: {}", botConfig.getUsername());
        log.info("Bot token: {}...", botConfig.getToken().substring(0, Math.min(10, botConfig.getToken().length())));
        log.info("========================================");
    }

    /**
     * Получить имя бота
     * Обязательный метод от TelegramLongPollingBot
     */
    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    /**
     * Главный метод обработки обновлений
     * Вызывается автоматически при получении нового сообщения/callback
     *
     * @param update объект с данными от Telegram
     */
    @Override
    public void onUpdateReceived(Update update) {
        log.info("========================================");
        log.info("Received update: {}", update.getUpdateId());
        log.info("========================================");

        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", update, e);

            Long chatId = null;
            if (update.hasMessage()) {
                chatId = update.getMessage().getChatId();
            } else if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
            }

            if (chatId != null) {
                sendMessage(chatId, "Произошла ошибка. Попробуйте ещё раз.");
            }
        }
    }

    /**
     * Обработка текстовых сообщений
     *
     * @param message сообщение от пользователя
     */
    private void handleMessage(Message message) {
        String text = message.getText();
        Long chatId = message.getChatId();

        log.info("Received message from {}: {}", chatId, text);

        // Получить или создать пользователя
        User user = userService.getOrCreateUser(
                chatId,
                message.getFrom().getUserName(),
                message.getFrom().getFirstName(),
                message.getFrom().getLastName()
        );

        // Обработка команд
        switch (text) {
            case "/start":
                handleStartCommand(chatId, user.getFirstName());
                break;

            case "/play":
                handlePlayCommand(chatId);
                break;

            case "/stats":
                handleStatsCommand(chatId);
                break;

            case "/help":
                handleHelpCommand(chatId);
                break;

            case "/leaderboard":
                handleLeaderboardCommand(chatId);
                break;

            default:
                sendMessage(chatId, "Неизвестная команда. Используй /help");
        }
    }

    /**
     * Команда /start - приветствие
     */
    private void handleStartCommand(Long chatId, String firstName) {
        String welcomeMessage = String.format(
                "👋 Привет, *%s*!\n\n" +
                        "Добро пожаловать в игру *Угадай страну по флагу*!\n\n" +
                        "🎮 Правила просты:\n" +
                        "1. Я покажу тебе флаг\n" +
                        "2. Ты выбираешь правильное название страны\n" +
                        "3. Набираешь очки и становишься лучшим!\n\n" +
                        "Используй /play чтобы начать игру\n" +
                        "Используй /help чтобы увидеть все команды",
                firstName
        );

        sendMessage(chatId, welcomeMessage);
    }

    /**
     * Команда /play - начать игру
     */
    private void handlePlayCommand(Long chatId) {
        try {
            gameService.getActiveSession(chatId).ifPresent(session -> {
                gameService.abandonActiveGame(chatId);
            });

            GameSession session = gameService.startNewGame(chatId);

            CountryDTO correctCountry = countryService.findByCode(session.getCountryCode());
            List<CountryDTO> options = countryService.getGameOptions(correctCountry, 4);

            sendGameQuestion(chatId, session, options);

        } catch (Exception e) {
            log.error("Error starting game for chatId: {}", chatId, e);
            sendMessage(chatId, "Ошибка при запуске игры. Попробуй ещё раз.");
        }
    }

    /**
     * Отправить вопрос с флагом и вариантами ответа
     */
    private void sendGameQuestion(Long chatId, GameSession session, List<CountryDTO> options) {
        try {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new InputFile(session.getFlagUrl()));
            photo.setCaption("🏳️ Что это за страна?");

            InlineKeyboardMarkup keyboard = createAnswerKeyboard(session.getId(), options);
            photo.setReplyMarkup(keyboard);

            execute(photo);

        } catch (TelegramApiException e) {
            log.error("Error sending game question", e);
        }
    }

    /**
     * Создать клавиатуру с вариантами ответа
     */
    private InlineKeyboardMarkup createAnswerKeyboard(Long sessionId, List<CountryDTO> options) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (int i = 0; i < options.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(options.get(i).getName());
            button1.setCallbackData("answer:" + sessionId + ":" + options.get(i).getName());
            row.add(button1);

            if (i + 1 < options.size()) {
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText(options.get(i + 1).getName());
                button2.setCallbackData("answer:" + sessionId + ":" + options.get(i + 1).getName());
                row.add(button2);
            }

            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);
        return markup;
    }

    /**
     * Обработка нажатий на кнопки (callback query)
     */
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("Received callback from {}: {}", chatId, data);

        if (data.equals("play_again")) {
            removeKeyboard(chatId, messageId);
            handlePlayCommand(chatId);
            return;
        }

        String[] parts = data.split(":", 3);

        if (parts.length == 3 && parts[0].equals("answer")) {
            Long sessionId = Long.parseLong(parts[1]);
            String userAnswer = parts[2];

            handleAnswer(chatId, messageId, sessionId, userAnswer);
        }
    }

    /**
     * Обработка ответа пользователя
     */
    private void handleAnswer(Long chatId, Integer messageId, Long sessionId, String userAnswer) {
        try {
            GameSession session = gameService.checkAnswer(sessionId, userAnswer);

            removeKeyboard(chatId, messageId);

            String resultMessage = gameService.formatGameResult(session);
            sendMessage(chatId, resultMessage);

            sendPlayAgainButton(chatId);

        } catch (Exception e) {
            log.error("Error handling answer", e);
            sendMessage(chatId, "Ошибка при проверке ответа.");
        }
    }

    /**
     * Удалить клавиатуру из сообщения
     */
    private void removeKeyboard(Long chatId, Integer messageId) {
        try {
            var editMessage = new org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setReplyMarkup(null);
            execute(editMessage);
        } catch (TelegramApiException e) {
            log.warn("Could not remove keyboard", e);
        }
    }

    /**
     * Отправить кнопку "Играть ещё"
     */
    private void sendPlayAgainButton(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Хочешь сыграть ещё? 🎮");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton playButton = new InlineKeyboardButton();
        playButton.setText("▶️ Играть ещё");
        playButton.setCallbackData("play_again");
        row.add(playButton);

        keyboard.add(row);
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending play again button", e);
        }
    }

    /**
     * Команда /stats - показать статистику
     */
    private void handleStatsCommand(Long chatId) {
        String stats = userService.getUserStatistics(chatId);
        sendMessage(chatId, stats);
    }

    /**
     * Команда /help - показать помощь
     */
    private void handleHelpCommand(Long chatId) {
        String helpMessage =
                "📚 *Доступные команды:*\n\n" +
                        "/start - Начать работу с ботом\n" +
                        "/play - Начать новую игру\n" +
                        "/stats - Твоя статистика\n" +
                        "/leaderboard - Топ игроков\n" +
                        "/help - Показать эту справку\n\n" +
                        "🎯 *Правила игры:*\n" +
                        "• За правильный ответ: +10 очков\n" +
                        "• За неправильный ответ: -5 очков\n" +
                        "• Цель: набрать максимум очков!";

        sendMessage(chatId, helpMessage);
    }

    /**
     * Команда /leaderboard - показать топ игроков
     */
    private void handleLeaderboardCommand(Long chatId) {
        List<User> topPlayers = userService.getTopPlayers(10);

        if (topPlayers.isEmpty()) {
            sendMessage(chatId, "Пока нет игроков в таблице лидеров.");
            return;
        }

        StringBuilder message = new StringBuilder("🏆 *Топ 10 игроков:*\n\n");

        for (int i = 0; i < topPlayers.size(); i++) {
            User player = topPlayers.get(i);
            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : String.format("%d.", i + 1);

            message.append(String.format(
                    "%s *%s* - %d очков (%.1f%%)\n",
                    medal,
                    player.getFirstName() != null ? player.getFirstName() : player.getUsername(),
                    player.getTotalScore(),
                    player.getAccuracy()
            ));
        }

        sendMessage(chatId, message.toString());
    }

    /**
     * Отправить текстовое сообщение
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");  // Поддержка форматирования

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message to {}: {}", chatId, text, e);
        }
    }
}