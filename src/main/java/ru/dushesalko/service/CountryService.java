package ru.dushesalko.service;

import ru.dushesalko.dto.CountryDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private static final String API_URL = "https://restcountries.com/v3.1/all?fields=name,cca3,flags,capital,region,population";
    private static final String LOCAL_JSON_FILE = "all.json"; // В resources
    private static final int MAX_RETRIES = 2;
    private static final int RETRY_DELAY_MS = 2000;

    private List<CountryDTO> countriesCache = null;

    @PostConstruct
    public void init() {
        // Убрали синхронный тест API, потому что он блокирует старт приложения и вызывает ошибки при
        // сетевых проблемах. Теперь создаём fallback и в отдельном потоке пытаемся загрузить API.
        log.info("========================================");
        log.info("Initializing CountryService...");
        log.info("========================================");

        createFallbackCountries();
        log.info("Loaded {} fallback countries for immediate use", countriesCache.size());

        new Thread(() -> {
            try {
                log.info("Attempting to load from API...");
                loadCountriesWithRetry();
            } catch (Exception e) {
                log.warn("API failed, trying local file...");
                try {
                    loadCountriesFromLocalFile();
                } catch (Exception fileException) {
                    log.error("Local file also failed, using fallback countries", fileException);
                }
            }
        }, "CountryService-API-Loader").start();

        log.info("========================================");
    }

    /**
     * Загрузка из локального JSON файла
     */
    private void loadCountriesFromLocalFile() {
        try {
            log.info("Loading countries from local file: {}", LOCAL_JSON_FILE);

            ClassPathResource resource = new ClassPathResource(LOCAL_JSON_FILE);
            if (!resource.exists()) {
                log.warn("Local file {} not found in resources", LOCAL_JSON_FILE);
                return;
            }

            String jsonResponse = new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            parseAndCacheCountries(jsonResponse);
            log.info("✓ Successfully loaded countries from local file");

        } catch (IOException e) {
            log.error("Error reading local file: {}", e.getMessage());
            throw new RuntimeException("Failed to read local file", e);
        }
    }

    /**
     * Загрузка стран с повторными попытками
     */
    private void loadCountriesWithRetry() {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                log.info("Loading from API (attempt {}/{})", attempt, MAX_RETRIES);
                loadCountriesFromAPI();
                log.info("✓ Successfully loaded countries from API on attempt {}", attempt);
                return;
            } catch (ResourceAccessException e) {
                lastException = e;
                log.warn("Attempt {}/{} failed: {}",
                        attempt, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        log.info("Waiting {} ms before retry...", RETRY_DELAY_MS);
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                lastException = e;
                log.error("Unexpected error: {}", e.getMessage());
                break;
            }
        }

        log.error("Failed to load from API after {} attempts", MAX_RETRIES);
        throw new RuntimeException("API loading failed", lastException);
    }

    /**
     * Загрузить страны из API — потоковое чтение и парсинг Jackson'ом (не держим весь ответ в String)
     */
    private void loadCountriesFromAPI() {
        try {
            log.info("Requesting API: {}", API_URL);

            String json = restTemplate.getForObject(API_URL, String.class);

            if (json == null || json.isBlank()) {
                throw new RuntimeException("API returned empty response");
            }

            parseAndCacheCountries(json);

            log.info("✓ Successfully loaded countries from API, {} cached", countriesCache.size());

        } catch (Exception e) {
            log.error("Failed to load countries from API", e);
            throw new RuntimeException("Failed to load countries from API", e);
        }
    }


    /**
     * Парсинг JSON и обновление кэша (оставлен как есть — принимает String)
     */
    private void parseAndCacheCountries(String jsonResponse) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();

            java.util.List<java.util.Map<String, Object>> countriesList =
                    objectMapper.readValue(jsonResponse,
                            new com.fasterxml.jackson.core.type.TypeReference<
                                    java.util.List<java.util.Map<String, Object>>>() {
                            });

            log.info("Parsed {} countries from JSON", countriesList.size());

            List<CountryDTO> newCache = countriesList.stream()
                    .map(this::mapToCountryDTO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!newCache.isEmpty()) {
                countriesCache = newCache;
                log.info("✓ Cached {} countries", countriesCache.size());
            } else {
                log.warn("No valid countries parsed");
            }

        } catch (Exception e) {
            log.error("JSON parsing error: {}", e.getMessage(), e);
            throw new RuntimeException("JSON parsing failed", e);
        }
    }

    public List<CountryDTO> getAllCountries() {
        if (countriesCache == null || countriesCache.isEmpty()) {
            log.warn("Cache is empty!");
            createFallbackCountries();
        }
        return countriesCache;
    }

    @SuppressWarnings("unchecked")
    private CountryDTO mapToCountryDTO(Map<String, Object> countryData) {
        try {
            Map<String, Object> nameObj = (Map<String, Object>) countryData.get("name");
            if (nameObj == null) return null;

            String commonName = (String) nameObj.get("common");
            String code = (String) countryData.get("cca3");

            Map<String, Object> flagsObj = (Map<String, Object>) countryData.get("flags");
            if (flagsObj == null) return null;

            String flagUrl = (String) flagsObj.get("png");

            List<String> capitals = (List<String>) countryData.get("capital");
            String capital = (capitals != null && !capitals.isEmpty()) ? capitals.get(0) : null;

            String region = (String) countryData.get("region");

            Long population = null;
            Object populationObj = countryData.get("population");
            if (populationObj instanceof Number) {
                population = ((Number) populationObj).longValue();
            }

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
            log.debug("Failed to parse country: {}", e.getMessage());
            return null;
        }
    }

    private void createFallbackCountries() {
        countriesCache = Arrays.asList(
                CountryDTO.builder()
                        .name("United States")
                        .code("USA")
                        .flagUrl("https://flagcdn.com/w320/us.png")
                        .capital("Washington D.C.")
                        .region("Americas")
                        .population(340110988L)
                        .build(),
                CountryDTO.builder()
                        .name("Russia")
                        .code("RUS")
                        .flagUrl("https://flagcdn.com/w320/ru.png")
                        .capital("Moscow")
                        .region("Europe")
                        .population(146028325L)
                        .build(),
                CountryDTO.builder()
                        .name("China")
                        .code("CHN")
                        .flagUrl("https://flagcdn.com/w320/cn.png")
                        .capital("Beijing")
                        .region("Asia")
                        .population(1408280000L)
                        .build(),
                CountryDTO.builder()
                        .name("Germany")
                        .code("DEU")
                        .flagUrl("https://flagcdn.com/w320/de.png")
                        .capital("Berlin")
                        .region("Europe")
                        .population(83491249L)
                        .build(),
                CountryDTO.builder()
                        .name("Japan")
                        .code("JPN")
                        .flagUrl("https://flagcdn.com/w320/jp.png")
                        .capital("Tokyo")
                        .region("Asia")
                        .population(123210000L)
                        .build(),
                CountryDTO.builder()
                        .name("Brazil")
                        .code("BRA")
                        .flagUrl("https://flagcdn.com/w320/br.png")
                        .capital("Brasília")
                        .region("Americas")
                        .population(213421037L)
                        .build(),
                CountryDTO.builder()
                        .name("United Kingdom")
                        .code("GBR")
                        .flagUrl("https://flagcdn.com/w320/gb.png")
                        .capital("London")
                        .region("Europe")
                        .population(69281437L)
                        .build(),
                CountryDTO.builder()
                        .name("France")
                        .code("FRA")
                        .flagUrl("https://flagcdn.com/w320/fr.png")
                        .capital("Paris")
                        .region("Europe")
                        .population(66351959L)
                        .build(),
                CountryDTO.builder()
                        .name("Italy")
                        .code("ITA")
                        .flagUrl("https://flagcdn.com/w320/it.png")
                        .capital("Rome")
                        .region("Europe")
                        .population(58927633L)
                        .build(),
                CountryDTO.builder()
                        .name("Canada")
                        .code("CAN")
                        .flagUrl("https://flagcdn.com/w320/ca.png")
                        .capital("Ottawa")
                        .region("Americas")
                        .population(41651653L)
                        .build(),
                CountryDTO.builder()
                        .name("Australia")
                        .code("AUS")
                        .flagUrl("https://flagcdn.com/w320/au.png")
                        .capital("Canberra")
                        .region("Oceania")
                        .population(27536874L)
                        .build(),
                CountryDTO.builder()
                        .name("India")
                        .code("IND")
                        .flagUrl("https://flagcdn.com/w320/in.png")
                        .capital("New Delhi")
                        .region("Asia")
                        .population(1417492000L)
                        .build()
        );
        log.info("Fallback countries created: {} countries", countriesCache.size());
    }

    public CountryDTO getRandomCountry() {
        List<CountryDTO> countries = getAllCountries();
        if (countries.isEmpty()) {
            throw new RuntimeException("No countries available");
        }
        Random random = new Random();
        return countries.get(random.nextInt(countries.size()));
    }

    public List<CountryDTO> getRandomCountries(int count, String excludeCode) {
        List<CountryDTO> countries = getAllCountries();
        List<CountryDTO> filtered = countries.stream()
                .filter(c -> !c.getCode().equals(excludeCode))
                .collect(Collectors.toList());
        Collections.shuffle(filtered);
        return filtered.stream().limit(count).collect(Collectors.toList());
    }

    public List<CountryDTO> getGameOptions(CountryDTO correctCountry, int optionsCount) {
        List<CountryDTO> options = getRandomCountries(optionsCount - 1, correctCountry.getCode());
        options.add(correctCountry);
        Collections.shuffle(options);
        return options;
    }

    public CountryDTO findByCode(String code) {
        return getAllCountries().stream()
                .filter(c -> c.getCode().equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }

    public CountryDTO findByName(String name) {
        return getAllCountries().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public List<CountryDTO> searchCountries(String query) {
        String lowerQuery = query.toLowerCase();
        return getAllCountries().stream()
                .filter(c -> c.getName().toLowerCase().contains(lowerQuery))
                .limit(10).collect(Collectors.toList());
    }

    public int getCountriesCount() {
        return getAllCountries().size();
    }

    public void clearCache() {
        log.info("Clearing cache");
        countriesCache = null;
    }

    // Оставил метод теста (не вызывается автоматически).
    public void testApi() {
        try {
            String json = restTemplate.getForObject(API_URL, String.class);
            System.out.println("API response length: " + (json != null ? json.length() : 0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}