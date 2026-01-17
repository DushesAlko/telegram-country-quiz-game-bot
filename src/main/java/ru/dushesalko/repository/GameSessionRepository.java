package ru.dushesalko.repository;

import ru.dushesalko.model.GameSession;
import ru.dushesalko.model.GameSession.GameStatus;
import ru.dushesalko.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с игровыми сессиями
 */
@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    /**
     * Найти все сессии пользователя
     *
     * SQL: SELECT * FROM game_sessions WHERE user_id = ?
     *
     * User user - Spring автоматически возьмёт user.id
     */
    List<GameSession> findByUser(User user);

    /**
     * Найти сессии пользователя по статусу
     *
     * SQL: SELECT * FROM game_sessions
     *      WHERE user_id = ? AND status = ?
     */
    List<GameSession> findByUserAndStatus(User user, GameStatus status);

    /**
     * Найти активную (незавершённую) сессию пользователя
     *
     * SQL: SELECT * FROM game_sessions
     *      WHERE user_id = ? AND status = 'IN_PROGRESS'
     *      LIMIT 1
     */
    Optional<GameSession> findFirstByUserAndStatusOrderByPlayedAtDesc(
            User user,
            GameStatus status
    );

    /**
     * Найти все правильные ответы пользователя
     *
     * SQL: SELECT * FROM game_sessions
     *      WHERE user_id = ? AND is_correct = true
     */
    List<GameSession> findByUserAndIsCorrect(User user, Boolean isCorrect);

    /**
     * Подсчитать количество игр пользователя
     *
     * SQL: SELECT COUNT(*) FROM game_sessions WHERE user_id = ?
     */
    long countByUser(User user);

    /**
     * Подсчитать правильные ответы пользователя
     */
    long countByUserAndIsCorrect(User user, Boolean isCorrect);

    /**
     * Найти последние N игр пользователя
     *
     * SQL: SELECT * FROM game_sessions
     *      WHERE user_id = ?
     *      ORDER BY played_at DESC
     *      LIMIT ?
     */
    List<GameSession> findTop10ByUserOrderByPlayedAtDesc(User user);

    /**
     * Найти игры по стране
     *
     * SQL: SELECT * FROM game_sessions WHERE country_code = ?
     */
    List<GameSession> findByCountryCode(String countryCode);

    /**
     * Найти игры за период времени
     *
     * SQL: SELECT * FROM game_sessions
     *      WHERE played_at BETWEEN ? AND ?
     */
    List<GameSession> findByPlayedAtBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Найти игры пользователя за сегодня
     *
     * SQL: SELECT * FROM game_sessions
     *      WHERE user_id = ? AND played_at >= ?
     */
    List<GameSession> findByUserAndPlayedAtAfter(User user, LocalDateTime date);

    /**
     * Пагинация - постраничный вывод
     *
     * Pageable - параметры пагинации (номер страницы, размер)
     * Page<GameSession> - результат с метаданными (всего страниц, элементов)
     *
     * Использование:
     * Pageable pageable = PageRequest.of(0, 10); // страница 0, размер 10
     * Page<GameSession> page = repository.findByUser(user, pageable);
     */
    Page<GameSession> findByUser(User user, Pageable pageable);

    /**
     * Кастомный запрос - статистика по стране
     *
     * Возвращает объект с:
     * - countryCode
     * - totalGames (сколько раз играли эту страну)
     * - correctAnswers (сколько раз угадали)
     *
     * Projection - возврат не Entity, а кастомного объекта
     */
    @Query("SELECT g.countryCode as countryCode, " +
            "COUNT(g) as totalGames, " +
            "SUM(CASE WHEN g.isCorrect = true THEN 1 ELSE 0 END) as correctAnswers " +
            "FROM GameSession g " +
            "GROUP BY g.countryCode " +
            "ORDER BY totalGames DESC")
    List<CountryStatistics> getCountryStatistics();

    /**
     * Статистика пользователя за период
     */
    @Query("SELECT COUNT(g) as totalGames, " +
            "SUM(CASE WHEN g.isCorrect = true THEN 1 ELSE 0 END) as correctAnswers, " +
            "SUM(g.points) as totalPoints, " +
            "AVG(g.timeSpent) as avgTimeSpent " +
            "FROM GameSession g " +
            "WHERE g.user = :user " +
            "AND g.playedAt BETWEEN :startDate AND :endDate")
    UserStatistics getUserStatistics(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Найти самые сложные страны (наименьший процент правильных ответов)
     *
     * Native SQL для сложных вычислений
     */
    @Query(value =
            "SELECT country_code, country_name, " +
                    "COUNT(*) as total_attempts, " +
                    "SUM(CASE WHEN is_correct = true THEN 1 ELSE 0 END) as correct_attempts, " +
                    "ROUND(100.0 * SUM(CASE WHEN is_correct = true THEN 1 ELSE 0 END) / COUNT(*), 2) as success_rate " +
                    "FROM game_sessions " +
                    "GROUP BY country_code, country_name " +
                    "HAVING COUNT(*) >= :minAttempts " +
                    "ORDER BY success_rate ASC " +
                    "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findHardestCountries(
            @Param("minAttempts") int minAttempts,
            @Param("limit") int limit
    );

    /**
     * Interface для Projection результатов
     */
    interface CountryStatistics {
        String getCountryCode();
        Long getTotalGames();
        Long getCorrectAnswers();
    }

    interface UserStatistics {
        Long getTotalGames();
        Long getCorrectAnswers();
        Long getTotalPoints();
        Double getAvgTimeSpent();
    }
}