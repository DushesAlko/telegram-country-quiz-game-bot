package ru.dushesalko.service;

import ru.dushesalko.dto.CountryDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service для работы с REST Countries API
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CountryService {

    private final RestTemplate restTemplate;

    // URL REST Countries API с явным указанием полей
    private static final String API_URL = "https://restcountries.com/v3.1/all?fields=name,cca3,flags,capital,region,population";

    // Кэш стран (чтобы не запрашивать API каждый раз)
    private List<CountryDTO> countriesCache = null;

    /**
     * Инициализация - загрузить страны при старте приложения
     * @PostConstruct - метод вызывается автоматически после создания bean
     */
    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("Initializing CountryService...");
        log.info("========================================");

        // Загружаем fallback страны сразу, чтобы бот мог работать
        createFallbackCountries();
        log.info("Loaded {} fallback countries for immediate use", countriesCache.size());

        // Пытаемся загрузить полный список в фоновом режиме
        new Thread(() -> {
            try {
                log.info("Attempting to load full country list from API...");
                loadCountries();
            } catch (Exception e) {
                log.warn("Could not load full country list, using fallback: {}", e.getMessage());
            }
        }).start();

        log.info("========================================");
    }

    /**
     * Получить все страны из API
     *
     * @return список всех стран
     */
    public List<CountryDTO> getAllCountries() {
        // Если кэш пустой - загрузить страны
        if (countriesCache == null || countriesCache.isEmpty()) {
            log.info("Loading countries from API...");
            loadCountries();
        }
        return countriesCache;
    }

    /**
     * Загрузить страны из API
     */
    private void loadCountries() {
        try {
            log.info("Loading countries from API: {}", API_URL);

            long startTime = System.currentTimeMillis();

            // Используем String.class вместо Map[].class
            String jsonResponse = restTemplate.getForObject(API_URL, String.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("API response received in {} ms", duration);

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                log.warn("Empty response from API");
                return;
            }

            // Парсим JSON вручную используя Jackson
            com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();

            java.util.List<java.util.Map<String, Object>> countriesList =
                    objectMapper.readValue(jsonResponse,
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {});

            log.info("Received {} countries from API", countriesList.size());

            // Преобразовать в DTO
            List<CountryDTO> newCache = countriesList.stream()
                    .map(this::mapToCountryDTO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Обновить кэш только если получили данные
            if (!newCache.isEmpty()) {
                countriesCache = newCache;
                log.info("✓ Successfully loaded {} countries from API", countriesCache.size());
            } else {
                log.warn("Failed to parse any countries from API response");
            }

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Connection timeout or network error: {}", e.getMessage());
            log.info("Using fallback countries");
        } catch (Exception e) {
            log.error("Error loading countries from API: {}", e.getMessage());
            log.info("Using fallback countries");
        }
    }

    /**
     * Создать базовый список стран (на случай недоступности API)
     */
    private void createFallbackCountries() {
        countriesCache = Arrays.asList(
                CountryDTO.builder()
                        .name("United States")
                        .code("USA")
                        .flagUrl("https://flagcdn.com/w320/us.png")
                        .capital("Washington D.C.")
                        .region("Americas")
                        .build(),
                CountryDTO.builder()
                        .name("Russia")
                        .code("RUS")
                        .flagUrl("https://flagcdn.com/w320/ru.png")
                        .capital("Moscow")
                        .region("Europe")
                        .build(),
                CountryDTO.builder()
                        .name("China")
                        .code("CHN")
                        .flagUrl("https://flagcdn.com/w320/cn.png")
                        .capital("Beijing")
                        .region("Asia")
                        .build(),
                CountryDTO.builder()
                        .name("Germany")
                        .code("DEU")
                        .flagUrl("https://flagcdn.com/w320/de.png")
                        .capital("Berlin")
                        .region("Europe")
                        .build(),
                CountryDTO.builder()
                        .name("Japan")
                        .code("JPN")
                        .flagUrl("https://flagcdn.com/w320/jp.png")
                        .capital("Tokyo")
                        .region("Asia")
                        .build(),
                CountryDTO.builder()
                        .name("Brazil")
                        .code("BRA")
                        .flagUrl("https://flagcdn.com/w320/br.png")
                        .capital("Brasília")
                        .region("Americas")
                        .build(),
                CountryDTO.builder()
                        .name("United Kingdom")
                        .code("GBR")
                        .flagUrl("https://flagcdn.com/w320/gb.png")
                        .capital("London")
                        .region("Europe")
                        .build(),
                CountryDTO.builder()
                        .name("France")
                        .code("FRA")
                        .flagUrl("https://flagcdn.com/w320/fr.png")
                        .capital("Paris")
                        .region("Europe")
                        .build(),
                CountryDTO.builder()
                        .name("Italy")
                        .code("ITA")
                        .flagUrl("https://flagcdn.com/w320/it.png")
                        .capital("Rome")
                        .region("Europe")
                        .build(),
                CountryDTO.builder()
                        .name("Canada")
                        .code("CAN")
                        .flagUrl("https://flagcdn.com/w320/ca.png")
                        .capital("Ottawa")
                        .region("Americas")
                        .build(),
                CountryDTO.builder()
                        .name("Australia")
                        .code("AUS")
                        .flagUrl("https://flagcdn.com/w320/au.png")
                        .capital("Canberra")
                        .region("Oceania")
                        .build(),
                CountryDTO.builder()
                        .name("India")
                        .code("IND")
                        .flagUrl("https://flagcdn.com/w320/in.png")
                        .capital("New Delhi")
                        .region("Asia")
                        .build()
        );
        log.info("Fallback countries created: {} countries", countriesCache.size());
    }

    /**
     * Преобразовать Map из API в CountryDTO
     *
     * @param countryData данные о стране из API
     * @return CountryDTO или null если данные некорректны
     */
    @SuppressWarnings("unchecked")
    private CountryDTO mapToCountryDTO(Map<String, Object> countryData) {
        try {
            // Получить название (структура: name.common)
            Map<String, Object> nameObj = (Map<String, Object>) countryData.get("name");
            if (nameObj == null) {
                return null;
            }
            String commonName = (String) nameObj.get("common");

            // Получить код страны (cca3)
            String code = (String) countryData.get("cca3");

            // Получить флаг (структура: flags.png)
            Map<String, Object> flagsObj = (Map<String, Object>) countryData.get("flags");
            if (flagsObj == null) {
                return null;
            }
            String flagUrl = (String) flagsObj.get("png");

            // Получить столицу (массив capital)
            List<String> capitals = (List<String>) countryData.get("capital");
            String capital = (capitals != null && !capitals.isEmpty())
                    ? capitals.get(0)
                    : null;

            // Получить регион
            String region = (String) countryData.get("region");

            // Получить население
            Object populationObj = countryData.get("population");
            Long population = null;
            if (populationObj instanceof Number) {
                population = ((Number) populationObj).longValue();
            }

            // Проверка обязательных полей
            if (commonName == null || code == null || flagUrl == null) {
                return null;
            }

            return CountryDTO.builder()
                    .name(commonName)
                    .code(code)
                    .flagUrl(flagUrl)
                    .capital(capital)
                    .region(region)
                    .population(population)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse country data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Получить случайную страну
     *
     * @return случайная страна
     */
    public CountryDTO getRandomCountry() {
        List<CountryDTO> countries = getAllCountries();

        if (countries.isEmpty()) {
            throw new RuntimeException("No countries available");
        }

        Random random = new Random();
        int index = random.nextInt(countries.size());

        CountryDTO selectedCountry = countries.get(index);
        log.debug("Selected random country: {}", selectedCountry.getName());

        return selectedCountry;
    }

    /**
     * Получить N случайных стран для вариантов ответа
     *
     * @param count количество стран
     * @param excludeCode код страны для исключения (правильный ответ)
     * @return список случайных стран
     */
    public List<CountryDTO> getRandomCountries(int count, String excludeCode) {
        List<CountryDTO> countries = getAllCountries();

        // Убрать правильный ответ из списка
        List<CountryDTO> filtered = countries.stream()
                .filter(c -> !c.getCode().equals(excludeCode))
                .collect(Collectors.toList());

        // Перемешать и взять N первых
        Collections.shuffle(filtered);

        return filtered.stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Получить варианты ответа для игры
     *
     * @param correctCountry правильная страна
     * @param optionsCount общее количество вариантов
     * @return список стран (включая правильную)
     */
    public List<CountryDTO> getGameOptions(CountryDTO correctCountry, int optionsCount) {
        log.debug("Generating {} options for country: {}",
                optionsCount, correctCountry.getName());

        // Получить неправильные варианты
        List<CountryDTO> options = getRandomCountries(
                optionsCount - 1,
                correctCountry.getCode()
        );

        // Добавить правильный ответ
        options.add(correctCountry);

        // Перемешать, чтобы правильный ответ был в случайной позиции
        Collections.shuffle(options);

        return options;
    }

    /**
     * Найти страну по коду
     *
     * @param code код страны (ISO 3166-1 alpha-3)
     * @return страна или null если не найдена
     */
    public CountryDTO findByCode(String code) {
        return getAllCountries().stream()
                .filter(c -> c.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * Найти страну по названию
     *
     * @param name название страны
     * @return страна или null если не найдена
     */
    public CountryDTO findByName(String name) {
        return getAllCountries().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Поиск стран по части названия
     *
     * @param query поисковый запрос
     * @return список найденных стран
     */
    public List<CountryDTO> searchCountries(String query) {
        String lowerQuery = query.toLowerCase();

        return getAllCountries().stream()
                .filter(c -> c.getName().toLowerCase().contains(lowerQuery))
                .limit(10)  // Максимум 10 результатов
                .collect(Collectors.toList());
    }

    /**
     * Получить количество стран в кэше
     *
     * @return количество стран
     */
    public int getCountriesCount() {
        return getAllCountries().size();
    }

    /**
     * Очистить кэш (для обновления данных)
     */
    public void clearCache() {
        log.info("Clearing countries cache");
        countriesCache = null;
    }
}