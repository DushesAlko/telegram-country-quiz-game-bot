package ru.dushesalko.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация RestTemplate с увеличенными таймаутами
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Таймаут на установку соединения - 10 секунд (10000 мс)
        factory.setConnectTimeout(5000);

        // Таймаут на чтение данных - 30 секунд (30000 мс)
        // REST Countries API может быть медленным
        factory.setReadTimeout(100000);

        return new RestTemplate(factory);
    }
}