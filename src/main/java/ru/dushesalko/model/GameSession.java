package ru.dushesalko.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity класс для игровой сессии
 *
 * Хранит информацию о каждой попытке угадать страну
 */
@Entity
@Table(name = "game_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSession {

    /**
     * Уникальный идентификатор сессии
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Связь с пользователем (Many-to-One)
     * Много сессий → один пользователь
     *
     * @ManyToOne - тип связи
     * @JoinColumn - внешний ключ в таблице game_sessions
     *
     * SQL: FOREIGN KEY (user_id) REFERENCES users(id)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Код страны (ISO 3166-1 alpha-3)
     * Примеры: USA, RUS, GBR, FRA
     */
    @Column(nullable = false, length = 3)
    private String countryCode;

    /**
     * Название страны
     */
    @Column(nullable = false)
    private String countryName;

    /**
     * URL флага страны
     */
    @Column(length = 500)
    private String flagUrl;

    /**
     * Ответ пользователя
     */
    @Column(length = 100)
    private String userAnswer;

    /**
     * Правильный ли был ответ
     */
    @Column(nullable = false)
    private Boolean isCorrect;

    /**
     * Количество очков за эту попытку
     */
    @Column(nullable = false)
    private Integer points;

    /**
     * Время, затраченное на ответ (в секундах)
     */
    private Integer timeSpent;

    /**
     * Дата и время начала игры
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime playedAt;

    /**
     * Статус сессии
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GameStatus status = GameStatus.IN_PROGRESS;

    /**
     * Enum для статуса игры
     */
    public enum GameStatus {
        IN_PROGRESS,    // Игра в процессе
        COMPLETED,      // Завершена
        ABANDONED       // Брошена (пользователь не ответил)
    }
}