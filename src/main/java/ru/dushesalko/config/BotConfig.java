package ru.dushesalko.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация Telegram бота
 */
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Data
public class BotConfig {

    private String username;
    private String token;
}
