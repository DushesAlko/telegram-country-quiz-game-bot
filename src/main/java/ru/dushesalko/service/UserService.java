package ru.dushesalko.service;

import ru.dushesalko.model.User;
import ru.dushesalko.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service для управления пользователями
 *
 * @Service - помечает класс как Service компонент Spring
 * @Slf4j - Lombok: создаёт logger (log.info, log.error и т.д.)
 * @RequiredArgsConstructor - Lombok: создаёт конструктор для final полей
 * @Transactional - все методы выполняются в транзакции
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserService {

    /**
     * Dependency Injection через конструктор
     * final - поле инициализируется один раз
     * @RequiredArgsConstructor создаст конструктор автоматически
     */
    private final UserRepository userRepository;

    /**
     * Получить пользователя по chat ID или создать нового
     *
     * Этот метод вызывается при каждом сообщении от пользователя
     *
     * @param chatId Telegram chat ID
     * @param username Telegram username (может быть null)
     * @param firstName Имя пользователя
     * @param lastName Фамилия пользователя
     * @return существующий или новый пользователь
     */
    public User getOrCreateUser(Long chatId, String username,
                                String firstName, String lastName) {
        log.debug("Getting or creating user with chatId: {}", chatId);

        return userRepository.findByChatId(chatId)
                .orElseGet(() -> {
                    log.info("Creating new user with chatId: {}", chatId);

                    User newUser = User.builder()
                            .chatId(chatId)
                            .username(username)
                            .firstName(firstName)
                            .lastName(lastName)
                            .totalScore(0)
                            .correctAnswers(0)
                            .incorrectAnswers(0)
                            .build();

                    return userRepository.save(newUser);
                });
    }

    /**
     * Найти пользователя по chat ID
     *
     * @param chatId Telegram chat ID
     * @return Optional с пользователем или пустой
     */
    @Transactional(readOnly = true)  // Оптимизация для чтения
    public Optional<User> findByChatId(Long chatId) {
        log.debug("Finding user by chatId: {}", chatId);
        return userRepository.findByChatId(chatId);
    }

    /**
     * Получить пользователя по chat ID (с исключением если не найден)
     *
     * @param chatId Telegram chat ID
     * @return пользователь
     * @throws RuntimeException если пользователь не найден
     */
    @Transactional(readOnly = true)
    public User getUserByChatId(Long chatId) {
        log.debug("Getting user by chatId: {}", chatId);

        return userRepository.findByChatId(chatId)
                .orElseThrow(() -> {
                    log.error("User not found with chatId: {}", chatId);
                    return new RuntimeException("User not found with chatId: " + chatId);
                });
    }

    /**
     * Обновить статистику пользователя после игры
     *
     * @param user пользователь
     * @param isCorrect правильный ли был ответ
     * @param points количество очков
     * @return обновлённый пользователь
     */
    public User updateUserStats(User user, boolean isCorrect, int points) {
        log.debug("Updating stats for user {}: correct={}, points={}",
                user.getId(), isCorrect, points);

        // Используем метод из Entity
        user.updateStats(isCorrect, points);

        // Сохраняем изменения
        User updatedUser = userRepository.save(user);

        log.info("User {} stats updated: score={}, accuracy={}%",
                user.getId(), updatedUser.getTotalScore(),
                updatedUser.getAccuracy());

        return updatedUser;
    }

    /**
     * Получить топ игроков по очкам
     *
     * @param limit количество игроков
     * @return список топ игроков
     */
    @Transactional(readOnly = true)
    public List<User> getTopPlayers(int limit) {
        log.debug("Getting top {} players", limit);

        // Используем limit через naming convention
        return userRepository.findTop10ByOrderByTotalScoreDesc();
    }

    /**
     * Получить всех пользователей
     *
     * @return список всех пользователей
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        log.debug("Getting all users");
        return userRepository.findAll();
    }

    /**
     * Получить статистику пользователя в виде строки
     *
     * @param chatId Telegram chat ID
     * @return форматированная статистика
     */
    @Transactional(readOnly = true)
    public String getUserStatistics(Long chatId) {
        User user = getUserByChatId(chatId);

        int totalGames = user.getCorrectAnswers() + user.getIncorrectAnswers();

        return String.format(
                "📊 *Твоя статистика:*\n\n" +
                        "🎮 Всего игр: %d\n" +
                        "✅ Правильных ответов: %d\n" +
                        "❌ Неправильных ответов: %d\n" +
                        "🎯 Точность: %.1f%%\n" +
                        "⭐ Общий счёт: %d",
                totalGames,
                user.getCorrectAnswers(),
                user.getIncorrectAnswers(),
                user.getAccuracy(),
                user.getTotalScore()
        );
    }

    /**
     * Проверить существование пользователя
     *
     * @param chatId Telegram chat ID
     * @return true если пользователь существует
     */
    @Transactional(readOnly = true)
    public boolean userExists(Long chatId) {
        return userRepository.existsByChatId(chatId);
    }

    /**
     * Сбросить статистику пользователя
     *
     * @param chatId Telegram chat ID
     */
    public void resetUserStats(Long chatId) {
        log.info("Resetting stats for user with chatId: {}", chatId);

        User user = getUserByChatId(chatId);
        user.setTotalScore(0);
        user.setCorrectAnswers(0);
        user.setIncorrectAnswers(0);

        userRepository.save(user);
        log.info("Stats reset for user: {}", user.getId());
    }

    /**
     * Удалить пользователя
     *
     * @param chatId Telegram chat ID
     */
    public void deleteUser(Long chatId) {
        log.info("Deleting user with chatId: {}", chatId);

        User user = getUserByChatId(chatId);
        userRepository.delete(user);

        log.info("User deleted: {}", user.getId());
    }
}