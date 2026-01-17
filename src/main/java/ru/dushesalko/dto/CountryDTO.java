package ru.dushesalko.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) для данных о стране
 * <p>
 * Используется для передачи данных из REST Countries API
 * <p>
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
     */
    private String name;

    /**
     * Код страны (ISO 3166-1 alpha-3)
     */
    private String code;

    /**
     * URL флага в формате PNG
     */
    private String flagUrl;

    /**
     * Столица
     */
    private String capital;

    /**
     * Регион
     */
    private String region;

    /**
     * Население
     */
    private Long population;
}