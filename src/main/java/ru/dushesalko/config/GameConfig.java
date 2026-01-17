package ru.dushesalko.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация игры
 */
@Configuration
@ConfigurationProperties(prefix = "game")
@Data
public class GameConfig {

    private int optionsCount = 4;
    private int pointsCorrect = 10;
    private int pointsIncorrect = -5;
}