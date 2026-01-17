package ru.dushesalko.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) для данных о стране
 *
 * Используется для передачи данных из REST Countries API
 *
 * DTO vs Entity:
 * - Entity - данные для БД
 * - DTO - данные для передачи между слоями/системами
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryDTO {

    /**
     * Название страны
     * Пример: "United States"
     */
    private String name;

    /**
     * Код страны (ISO 3166-1 alpha-3)
     * Пример: "USA", "RUS", "GBR"
     */
    private String code;

    /**
     * URL флага в формате PNG
     */
    private String flagUrl;

    /**
     * Столица
     * Может быть null для некоторых территорий
     */
    private String capital;

    /**
     * Регион
     * Примеры: "Europe", "Asia", "Americas"
     */
    private String region;

    /**
     * Население
     */
    private Long population;
}