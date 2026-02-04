package ru.dushesalko.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CountryServiceTest {

    @Autowired
    private CountryService countryService;

    private static final String API_URL = "https://restcountries.com/v3.1/all?fields=name,cca3,flags,capital,region,population";

    @Test
    void testApiHeaderOk() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.HEAD, null, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "API should return 200 OK on HEAD request");
    }

    @Test
    void testLocalFileParsing() {
        try {
            ClassPathResource resource = new ClassPathResource("all.json");
            assertTrue(resource.exists(), "Local file all.json must exist");

            String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(json.isBlank(), "Local JSON file should not be empty");

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var countriesList = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String,Object>>>() {});
            assertFalse(countriesList.isEmpty(), "Parsed country list should not be empty");

        } catch (Exception e) {
            fail("Exception during local file parsing: " + e.getMessage());
        }
    }
}
