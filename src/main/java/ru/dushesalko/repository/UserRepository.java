package ru.dushesalko.repository;

import ru.dushesalko.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с пользователями
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Найти пользователя по chat ID из Telegram
     */
    Optional<User> findByChatId(Long chatId);

    /**
     * Проверить существование пользователя по chat ID
     */
    boolean existsByChatId(Long chatId);

    /**
     * Найти пользователя по username
     */
    Optional<User> findByUsername(String username);

    /**
     * Найти всех пользователей с количеством очков больше указанного
     */
    List<User> findByTotalScoreGreaterThan(Integer score);

    /**
     * Топ игроков по очкам
     */
    List<User> findTop10ByOrderByTotalScoreDesc();

    /**
     * Найти пользователей по части имени (поиск)
     */
    List<User> findByFirstNameContainingIgnoreCase(String name);

    /**
     * Найти пользователей с определённым диапазоном очков
     */
    List<User> findByTotalScoreBetween(Integer minScore, Integer maxScore);

    /**
     * Кастомный JPQL запрос
     */
    @Query("SELECT u FROM User u WHERE u.chatId = :chatId")
    Optional<User> findUserByChatId(@Param("chatId") Long chatId);

    /**
     * Кастомный SQL запрос (native query)
     */
    @Query(value = "SELECT * FROM users WHERE total_score > :minScore ORDER BY total_score DESC LIMIT :limit",
            nativeQuery = true)
    List<User> findTopPlayersByScore(@Param("minScore") Integer minScore,
                                     @Param("limit") Integer limit);

    /**
     * Подсчёт пользователей с очками выше заданного
     */
    long countByTotalScoreGreaterThan(Integer score);

    /**
     * Удалить пользователей с нулевыми очками
     */
    @Query("DELETE FROM User u WHERE u.totalScore = 0")
    void deleteUsersWithZeroScore();
}