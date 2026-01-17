package ru.dushesalko.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация HTTP клиента
 */
@Configuration
class RestTemplateConfig {

    /**
     * Bean для RestTemplate с таймаутами
     * RestTemplate - Spring класс для HTTP запросов
     *
     * @Bean - метод создаёт Spring Bean
     * Этот объект можно инжектить в другие классы
     */
    @Bean
    public RestTemplate restTemplate() {
        // Создаём фабрику с таймаутами
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();

        // Таймауты в миллисекундах
        factory.setConnectTimeout(5000);  // 5 секунд на подключение
        factory.setReadTimeout(10000);     // 10 секунд на чтение

        return new RestTemplate(factory);
    }
}