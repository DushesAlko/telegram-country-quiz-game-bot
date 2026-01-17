package ru.dushesalko.service;

import ru.dushesalko.config.GameConfig;
import ru.dushesalko.dto.CountryDTO;
import ru.dushesalko.model.GameSession;
import ru.dushesalko.model.GameSession.GameStatus;
import ru.dushesalko.model.User;
import ru.dushesalko.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service для управления игровыми сессиями
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class GameService {

    private final GameSessionRepository gameSessionRepository;
    private final CountryService countryService;
    private final UserService userService;
    private final GameConfig gameConfig;

    /**
     * Начать новую игру для пользователя
     *
     * @param chatId Telegram chat ID
     * @return игровая сессия
     */
    public GameSession startNewGame(Long chatId) {
        log.info("Starting new game for chatId: {}", chatId);

        // Получить пользователя
        User user = userService.getUserByChatId(chatId);

        // Получить случайную страну
        CountryDTO country = countryService.getRandomCountry();

        // Создать игровую сессию
        GameSession session = GameSession.builder()
                .user(user)
                .countryCode(country.getCode())
                .countryName(country.getName())
                .flagUrl(country.getFlagUrl())
                .isCorrect(false)
                .points(0)
                .status(GameStatus.IN_PROGRESS)
                .build();

        GameSession savedSession = gameSessionRepository.save(session);

        log.info("New game session created: id={}, country={}",
                savedSession.getId(), country.getName());

        return savedSession;
    }

    /**
     * Проверить ответ пользователя
     *
     * @param sessionId  ID игровой сессии
     * @param userAnswer ответ пользователя
     * @return обновлённая игровая сессия
     */
    public GameSession checkAnswer(Long sessionId, String userAnswer) {
        log.debug("Checking answer for session: {}", sessionId);

        // Найти сессию
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Game session not found"));

        // Проверить что игра ещё не завершена
        if (session.getStatus() != GameStatus.IN_PROGRESS) {
            throw new RuntimeException("Game already completed");
        }

        // Проверить ответ (игнорируем регистр)
        boolean isCorrect = session.getCountryName()
                .equalsIgnoreCase(userAnswer.trim());

        // Вычислить очки
        int points = isCorrect ?
                gameConfig.getPointsCorrect() :
                gameConfig.getPointsIncorrect();

        // Вычислить время (если есть)
        Integer timeSpent = null;
        if (session.getPlayedAt() != null) {
            timeSpent = (int) ChronoUnit.SECONDS.between(
                    session.getPlayedAt(),
                    LocalDateTime.now()
            );
        }

        // Обновить сессию
        session.setUserAnswer(userAnswer);
        session.setIsCorrect(isCorrect);
        session.setPoints(points);
        session.setTimeSpent(timeSpent);
        session.setStatus(GameStatus.COMPLETED);

        GameSession updatedSession = gameSessionRepository.save(session);

        // Обновить статистику пользователя
        userService.updateUserStats(session.getUser(), isCorrect, points);

        log.info("Answer checked for session {}: correct={}, points={}",
                sessionId, isCorrect, points);

        return updatedSession;
    }

    /**
     * Получить активную сессию пользователя
     *
     * @param chatId Telegram chat ID
     * @return активная сессия или пустой Optional
     */
    @Transactional(readOnly = true)
    public Optional<GameSession> getActiveSession(Long chatId) {
        User user = userService.getUserByChatId(chatId);
        return gameSessionRepository.findFirstByUserAndStatusOrderByPlayedAtDesc(
                user,
                GameStatus.IN_PROGRESS
        );
    }

    /**
     * Получить все сессии пользователя
     *
     * @param chatId Telegram chat ID
     * @return список сессий
     */
    @Transactional(readOnly = true)
    public List<GameSession> getUserSessions(Long chatId) {
        User user = userService.getUserByChatId(chatId);
        return gameSessionRepository.findByUser(user);
    }

    /**
     * Получить последние N игр пользователя
     *
     * @param chatId Telegram chat ID
     * @param limit  количество игр
     * @return список последних игр
     */
    @Transactional(readOnly = true)
    public List<GameSession> getRecentGames(Long chatId, int limit) {
        User user = userService.getUserByChatId(chatId);
        return gameSessionRepository.findTop10ByUserOrderByPlayedAtDesc(user);
    }

    /**
     * Подсчитать общее количество игр пользователя
     *
     * @param chatId Telegram chat ID
     * @return количество игр
     */
    @Transactional(readOnly = true)
    public long countUserGames(Long chatId) {
        User user = userService.getUserByChatId(chatId);
        return gameSessionRepository.countByUser(user);
    }

    /**
     * Отменить активную игру (если пользователь не ответил)
     *
     * @param chatId Telegram chat ID
     */
    public void abandonActiveGame(Long chatId) {
        log.info("Abandoning active game for chatId: {}", chatId);

        getActiveSession(chatId).ifPresent(session -> {
            session.setStatus(GameStatus.ABANDONED);
            gameSessionRepository.save(session);
            log.info("Game session {} abandoned", session.getId());
        });
    }

    /**
     * Получить статистику по странам
     *
     * @return список статистики по странам
     */
    @Transactional(readOnly = true)
    public List<GameSessionRepository.CountryStatistics> getCountryStatistics() {
        return gameSessionRepository.getCountryStatistics();
    }

    /**
     * Получить самые сложные страны
     *
     * @param minAttempts минимальное количество попыток
     * @param limit       количество стран
     * @return список самых сложных стран
     */
    @Transactional(readOnly = true)
    public List<Object[]> getHardestCountries(int minAttempts, int limit) {
        return gameSessionRepository.findHardestCountries(minAttempts, limit);
    }

    /**
     * Получить игры пользователя за сегодня
     *
     * @param chatId Telegram chat ID
     * @return список игр за сегодня
     */
    @Transactional(readOnly = true)
    public List<GameSession> getTodayGames(Long chatId) {
        User user = userService.getUserByChatId(chatId);
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return gameSessionRepository.findByUserAndPlayedAtAfter(user, startOfDay);
    }

    /**
     * Форматировать результат игры для отправки пользователю
     *
     * @param session завершённая игровая сессия
     * @return форматированное сообщение
     */
    public String formatGameResult(GameSession session) {
        if (session.getIsCorrect()) {
            return String.format(
                    "✅ *Правильно!*\n\n" +
                            "🏳️ Страна: %s\n" +
                            "⭐ Очки: +%d\n" +
                            "⏱️ Время: %d сек.",
                    session.getCountryName(),
                    session.getPoints(),
                    session.getTimeSpent() != null ? session.getTimeSpent() : 0
            );
        } else {
            return String.format(
                    "❌ *Неправильно!*\n\n" +
                            "🏳️ Правильный ответ: %s\n" +
                            "💭 Твой ответ: %s\n" +
                            "⭐ Очки: %d",
                    session.getCountryName(),
                    session.getUserAnswer(),
                    session.getPoints()
            );
        }
    }
}