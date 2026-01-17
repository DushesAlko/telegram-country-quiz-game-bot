package ru.dushesalko.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity класс для пользователя бота
 * <p>
 * Соответствует таблице "users" в базе данных
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Уникальный идентификатор пользователя
     *
     * @Id - первичный ключ
     * @GeneratedValue - автоматическая генерация значения
     * IDENTITY - использовать AUTO_INCREMENT в БД
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Chat ID из Telegram
     * Это уникальный идентификатор чата пользователя с ботом
     *
     * @Column - настройка колонки в БД
     * nullable = false - NOT NULL в БД
     * unique = true - UNIQUE constraint
     */
    @Column(nullable = false, unique = true)
    private Long chatId;

    /**
     * Username пользователя в Telegram
     * Может быть null, если пользователь не установил username
     */
    @Column(length = 100)
    private String username;

    /**
     * Имя пользователя (First Name в Telegram)
     */
    @Column(length = 100)
    private String firstName;

    /**
     * Фамилия пользователя (Last Name в Telegram)
     */
    @Column(length = 100)
    private String lastName;

    /**
     * Общее количество очков
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer totalScore = 0;

    /**
     * Количество правильных ответов
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer correctAnswers = 0;

    /**
     * Количество неправильных ответов
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer incorrectAnswers = 0;

    /**
     * Дата и время регистрации пользователя
     * Автоматически устанавливается при создании записи
     */
    @CreationTimestamp

    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления
     * Автоматически обновляется при изменении записи
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Вычисляемое поле - процент правильных ответов
     *
     * @Transient - не сохраняется в БД
     */
    @Transient
    public double getAccuracy() {
        int total = correctAnswers + incorrectAnswers;
        if (total == 0) {
            return 0.0;
        }
        return (correctAnswers * 100.0) / total;
    }

    /**
     * Метод для обновления статистики после игры
     *
     * @param isCorrect правильный ли был ответ
     * @param points    количество очков
     */
    public void updateStats(boolean isCorrect, int points) {
        this.totalScore += points;
        if (isCorrect) {
            this.correctAnswers++;
        } else {
            this.incorrectAnswers++;
        }
    }
}