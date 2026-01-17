package ru.dushesalko;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс приложения Country Quiz Bot
 *
 * @SpringBootApplication объединяет три аннотации:
 * 1. @Configuration - класс является источником конфигурации
 * 2. @EnableAutoConfiguration - автоматическая настройка Spring Boot
 * 3. @ComponentScan - сканирование компонентов в пакете и подпакетах
 */
@SpringBootApplication
public class CountryQuizBotApplication {

    /**
     * Точка входа в приложение
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        // Запуск Spring Boot приложения
        SpringApplication.run(CountryQuizBotApplication.class, args);
    }
}