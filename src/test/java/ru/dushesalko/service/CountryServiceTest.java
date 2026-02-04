package ru.dushesalko.service;

import ru.dushesalko.dto.CountryDTO;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CountryServiceTest {

    private final CountryService countryService =
            new CountryService(Mockito.mock(RestTemplate.class));

    @Test
    void getRandomCountry_shouldReturnCountry() {
        CountryDTO country = countryService.getRandomCountry();
        assertNotNull(country);
        assertNotNull(country.getName());
    }

    @Test
    void getGameOptions_shouldContainCorrectCountry() {
        CountryDTO correct = countryService.getRandomCountry();
        List<CountryDTO> options = countryService.getGameOptions(correct, 4);

        assertEquals(4, options.size());
        assertTrue(options.stream()
                .anyMatch(c -> c.getCode().equals(correct.getCode())));
    }

    @Test
    void findByCode_shouldReturnCountry() {
        CountryDTO country = countryService.findByCode("DEU");
        assertNotNull(country);
        assertEquals("DEU", country.getCode());
    }
}
