package ru.dushesalko.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.dushesalko.config.GameConfig;
import ru.dushesalko.dto.CountryDTO;
import ru.dushesalko.model.GameSession;
import ru.dushesalko.model.GameSession.GameStatus;
import ru.dushesalko.model.User;
import ru.dushesalko.repository.GameSessionRepository;
import ru.dushesalko.repository.UserRepository;
import ru.dushesalko.service.CountryService;
import ru.dushesalko.service.GameService;
import ru.dushesalko.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TelegramBotIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("quiz_test")
            .withUsername("test_user")
            .withPassword("test_pass");
    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private GameService gameService;

    @Autowired
    private CountryService countryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private GameConfig gameConfig;

    @BeforeEach
    void cleanDatabase() {
        gameSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    void testDatabaseConnection() {
        assertTrue(postgres.isRunning());
        assertNotNull(postgres.getJdbcUrl());

        long userCount = userRepository.count();
        assertEquals(0, userCount);
    }

    @Test
    @Order(2)
    @DisplayName("CountryService должен загрузить страны")
    void testCountryServiceInitialization() {
        List<CountryDTO> countries = countryService.getAllCountries();

        assertNotNull(countries);
        assertFalse(countries.isEmpty());
        assertTrue(countries.size() >= 12);
    }

    @Test
    @Order(3)
    @DisplayName("Должен создать нового пользователя при первом взаимодействии")
    void testCreateNewUser() {
        Long chatId = 123456789L;
        String username = "testuser";
        String firstName = "Test";
        String lastName = "User";

        User user = userService.getOrCreateUser(chatId, username, firstName, lastName);

        assertNotNull(user.getId());
        assertEquals(chatId, user.getChatId());
        assertEquals(username, user.getUsername());
        assertEquals(firstName, user.getFirstName());
        assertEquals(lastName, user.getLastName());
        assertEquals(0, user.getTotalScore());
        assertEquals(0, user.getCorrectAnswers());
        assertEquals(0, user.getIncorrectAnswers());
        assertNotNull(user.getCreatedAt());

        Optional<User> savedUser = userRepository.findByChatId(chatId);
        assertTrue(savedUser.isPresent());
        assertEquals(user.getId(), savedUser.get().getId());
    }

    @Test
    @Order(4)
    void testGetExistingUser() {
        Long chatId = 987654321L;
        User existingUser = User.builder()
                .chatId(chatId)
                .username("existing")
                .firstName("Existing")
                .lastName("User")
                .totalScore(100)
                .correctAnswers(10)
                .incorrectAnswers(5)
                .build();
        userRepository.save(existingUser);

        // Act
        User retrievedUser = userService.getOrCreateUser(chatId, "new_name", "New", "Name");

        // Assert
        assertEquals(existingUser.getId(), retrievedUser.getId());
        assertEquals(100, retrievedUser.getTotalScore());
        assertEquals("existing", retrievedUser.getUsername());
        assertEquals(1, userRepository.count());
    }

    @Test
    @Order(5)
    void testUpdateUserStatsCorrectAnswer() {
        // Arrange
        User user = User.builder()
                .chatId(111222333L)
                .username("player1")
                .firstName("Player")
                .lastName("One")
                .totalScore(0)
                .correctAnswers(0)
                .incorrectAnswers(0)
                .build();
        userRepository.save(user);

        int pointsCorrect = gameConfig.getPointsCorrect();

        User updatedUser = userService.updateUserStats(user, true, pointsCorrect);

        assertEquals(pointsCorrect, updatedUser.getTotalScore());
        assertEquals(1, updatedUser.getCorrectAnswers());
        assertEquals(0, updatedUser.getIncorrectAnswers());

        double expectedAccuracy = 100.0;
        assertEquals(expectedAccuracy, updatedUser.getAccuracy(), 0.01);

        User dbUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(pointsCorrect, dbUser.getTotalScore());
        assertEquals(1, dbUser.getCorrectAnswers());
    }

    @Test
    @Order(6)
    void testUpdateUserStatsIncorrectAnswer() {
        User user = User.builder()
                .chatId(444555666L)
                .username("player2")
                .firstName("Player")
                .lastName("Two")
                .totalScore(50)
                .correctAnswers(5)
                .incorrectAnswers(0)
                .build();
        userRepository.save(user);

        int pointsIncorrect = gameConfig.getPointsIncorrect();

        User updatedUser = userService.updateUserStats(user, false, pointsIncorrect);

        assertEquals(50 + pointsIncorrect, updatedUser.getTotalScore());
        assertEquals(5, updatedUser.getCorrectAnswers());
        assertEquals(1, updatedUser.getIncorrectAnswers());

        double expectedAccuracy = (5.0 / 6.0) * 100;
        assertEquals(expectedAccuracy, updatedUser.getAccuracy(), 0.01);
    }

    @Test
    @Order(7)
    void testGetTopPlayers() {
        // Arrange
        createUserWithScore(100L, "player1", 500);
        createUserWithScore(200L, "player2", 1000);
        createUserWithScore(300L, "player3", 750);
        createUserWithScore(400L, "player4", 250);
        createUserWithScore(500L, "player5", 900);

        List<User> topPlayers = userService.getTopPlayers(10);

        assertNotNull(topPlayers);
        assertFalse(topPlayers.isEmpty());
        assertTrue(topPlayers.size() <= 5);

        assertEquals("player2", topPlayers.get(0).getUsername());
        assertEquals(1000, topPlayers.get(0).getTotalScore());

        assertEquals("player5", topPlayers.get(1).getUsername());
        assertEquals(900, topPlayers.get(1).getTotalScore());

        assertEquals("player3", topPlayers.get(2).getUsername());
        assertEquals(750, topPlayers.get(2).getTotalScore());
    }

    @Test
    @Order(8)
    void testStartNewGame() {
        Long chatId = 11111L;
        User user = createUserWithScore(chatId, "gamer", 0);

        GameSession session = gameService.startNewGame(chatId);

        assertNotNull(session.getId());
        assertEquals(user.getId(), session.getUser().getId());
        assertNotNull(session.getCountryCode());
        assertNotNull(session.getCountryName());
        assertNotNull(session.getFlagUrl());
        assertEquals(GameStatus.IN_PROGRESS, session.getStatus());
        assertEquals(0, session.getPoints());
        assertFalse(session.getIsCorrect());
        assertNotNull(session.getPlayedAt());

        Optional<GameSession> savedSession = gameSessionRepository.findById(session.getId());
        assertTrue(savedSession.isPresent());
    }

    @Test
    @Order(9)
    void testCheckAnswerCorrect() {
        Long chatId = 22222L;
        User user = createUserWithScore(chatId, "smart_player", 0);
        GameSession session = gameService.startNewGame(chatId);
        String correctAnswer = session.getCountryName();

        GameSession updatedSession = gameService.checkAnswer(session.getId(), correctAnswer);

        assertTrue(updatedSession.getIsCorrect());
        assertEquals(correctAnswer, updatedSession.getUserAnswer());
        assertEquals(gameConfig.getPointsCorrect(), updatedSession.getPoints());
        assertEquals(GameStatus.COMPLETED, updatedSession.getStatus());
        assertNotNull(updatedSession.getTimeSpent());

        User updatedUser = userRepository.findByChatId(chatId).orElseThrow();
        assertEquals(gameConfig.getPointsCorrect(), updatedUser.getTotalScore());
        assertEquals(1, updatedUser.getCorrectAnswers());
        assertEquals(0, updatedUser.getIncorrectAnswers());
    }

    @Test
    @Order(10)
    void testCheckAnswerIncorrect() {
        Long chatId = 33333L;
        User user = createUserWithScore(chatId, "unlucky_player", 100);
        GameSession session = gameService.startNewGame(chatId);
        String wrongAnswer = "NonExistentCountry123";

        GameSession updatedSession = gameService.checkAnswer(session.getId(), wrongAnswer);

        assertFalse(updatedSession.getIsCorrect());
        assertEquals(wrongAnswer, updatedSession.getUserAnswer());
        assertEquals(gameConfig.getPointsIncorrect(), updatedSession.getPoints());
        assertEquals(GameStatus.COMPLETED, updatedSession.getStatus());

        User updatedUser = userRepository.findByChatId(chatId).orElseThrow();
        assertEquals(100 + gameConfig.getPointsIncorrect(), updatedUser.getTotalScore());
        assertEquals(0, updatedUser.getCorrectAnswers());
        assertEquals(1, updatedUser.getIncorrectAnswers());
    }

    @Test
    @Order(11)
    void testFullGameScenario() {
        Long chatId = 131313L;
        User user = userService.getOrCreateUser(chatId, "fulltest", "Full", "Test");
        assertEquals(0, user.getTotalScore());

        GameSession game1 = gameService.startNewGame(chatId);
        gameService.checkAnswer(game1.getId(), game1.getCountryName());

        GameSession game2 = gameService.startNewGame(chatId);
        gameService.checkAnswer(game2.getId(), "WrongAnswer");

        GameSession game3 = gameService.startNewGame(chatId);
        gameService.checkAnswer(game3.getId(), game3.getCountryName());

        User finalUser = userService.getUserByChatId(chatId);
        assertEquals(2, finalUser.getCorrectAnswers());
        assertEquals(1, finalUser.getIncorrectAnswers());

        int expectedScore = gameConfig.getPointsCorrect() * 2 + gameConfig.getPointsIncorrect();
        assertEquals(expectedScore, finalUser.getTotalScore());

        double expectedAccuracy = (2.0 / 3.0) * 100;
        assertEquals(expectedAccuracy, finalUser.getAccuracy(), 0.01);
    }

    private User createUserWithScore(Long chatId, String username, int score) {
        User user = User.builder()
                .chatId(chatId)
                .username(username)
                .firstName("Test")
                .lastName("User")
                .totalScore(score)
                .correctAnswers(0)
                .incorrectAnswers(0)
                .build();
        return userRepository.save(user);
    }
}