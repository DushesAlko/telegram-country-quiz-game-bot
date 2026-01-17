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
 *
 * JpaRepository<User, Long>:
 * - User - тип Entity
 * - Long - тип ID (primary key)
 *
 * JpaRepository предоставляет методы:
 * - save(user) - сохранить/обновить
 * - findById(id) - найти по ID
 * - findAll() - получить всех
 * - delete(user) - удалить
 * - count() - количество записей
 * - existsById(id) - проверить существование
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Найти пользователя по chat ID из Telegram
     *
     * Spring автоматически создаст SQL:
     * SELECT * FROM users WHERE chat_id = ?
     *
     * Naming convention:
     * findBy + ИмяПоля + Условие
     *
     * Optional<User> - может быть null, безопасная обработка
     */
    Optional<User> findByChatId(Long chatId);

    /**
     * Проверить существование пользователя по chat ID
     *
     * SQL: SELECT COUNT(*) > 0 FROM users WHERE chat_id = ?
     *
     * Возвращает true/false
     */
    boolean existsByChatId(Long chatId);

    /**
     * Найти пользователя по username
     *
     * SQL: SELECT * FROM users WHERE username = ?
     */
    Optional<User> findByUsername(String username);

    /**
     * Найти всех пользователей с количеством очков больше указанного
     *
     * SQL: SELECT * FROM users WHERE total_score > ?
     *
     * GreaterThan - больше чем
     * LessThan - меньше чем
     * Between - между значениями
     */
    List<User> findByTotalScoreGreaterThan(Integer score);

    /**
     * Топ игроков по очкам
     *
     * SQL: SELECT * FROM users
     *      ORDER BY total_score DESC
     *      LIMIT ?
     *
     * OrderBy + ИмяПоля + Desc/Asc
     * Desc - по убыванию, Asc - по возрастанию
     */
    List<User> findTop10ByOrderByTotalScoreDesc();

    /**
     * Найти пользователей по части имени (поиск)
     *
     * SQL: SELECT * FROM users
     *      WHERE first_name LIKE %?%
     *
     * Containing - содержит подстроку (LIKE %text%)
     * StartingWith - начинается с (LIKE text%)
     * EndingWith - заканчивается на (LIKE %text)
     * IgnoreCase - игнорировать регистр
     */
    List<User> findByFirstNameContainingIgnoreCase(String name);

    /**
     * Найти пользователей с определённым диапазоном очков
     *
     * SQL: SELECT * FROM users
     *      WHERE total_score BETWEEN ? AND ?
     */
    List<User> findByTotalScoreBetween(Integer minScore, Integer maxScore);

    /**
     * Кастомный JPQL запрос
     *
     * JPQL (Java Persistence Query Language) - SQL для Entity
     * Используем имена классов и полей, а не таблиц!
     *
     * :chatId - именованный параметр
     * @Param("chatId") - связь с параметром метода
     */
    @Query("SELECT u FROM User u WHERE u.chatId = :chatId")
    Optional<User> findUserByChatId(@Param("chatId") Long chatId);

    /**
     * Кастомный SQL запрос (native query)
     *
     * Используется когда JPQL не подходит
     * nativeQuery = true - это чистый SQL
     */
    @Query(value = "SELECT * FROM users WHERE total_score > :minScore ORDER BY total_score DESC LIMIT :limit",
            nativeQuery = true)
    List<User> findTopPlayersByScore(@Param("minScore") Integer minScore,
                                     @Param("limit") Integer limit);

    /**
     * Подсчёт пользователей с очками выше заданного
     *
     * SQL: SELECT COUNT(*) FROM users WHERE total_score > ?
     */
    long countByTotalScoreGreaterThan(Integer score);

    /**
     * Удалить пользователей с нулевыми очками
     *
     * SQL: DELETE FROM users WHERE total_score = 0
     *
     * @Modifying - указывает что запрос изменяет данные
     * @Transactional - нужна транзакция (добавим в Service)
     */
    @Query("DELETE FROM User u WHERE u.totalScore = 0")
    void deleteUsersWithZeroScore();
}