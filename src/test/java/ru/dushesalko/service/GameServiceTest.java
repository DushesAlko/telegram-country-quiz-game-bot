package ru.dushesalko.service;

import ru.dushesalko.config.GameConfig;
import ru.dushesalko.dto.CountryDTO;
import ru.dushesalko.model.GameSession;
import ru.dushesalko.model.GameSession.GameStatus;
import ru.dushesalko.model.User;
import ru.dushesalko.repository.GameSessionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private CountryService countryService;

    @Mock
    private UserService userService;

    @Mock
    private GameConfig gameConfig;

    @InjectMocks
    private GameService gameService;

    private User user;
    private CountryDTO country;
    private GameSession session;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .chatId(100L)
                .firstName("Test")
                .build();

        country = CountryDTO.builder()
                .name("Germany")
                .code("DEU")
                .flagUrl("flag.png")
                .build();

        session = GameSession.builder()
                .id(1L)
                .user(user)
                .countryName("Germany")
                .countryCode("DEU")
                .status(GameStatus.IN_PROGRESS)
                .build();
    }

    @Test
    void startNewGame_shouldCreateSession() {
        when(userService.getUserByChatId(100L)).thenReturn(user);
        when(countryService.getRandomCountry()).thenReturn(country);
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GameSession result = gameService.startNewGame(100L);

        assertNotNull(result);
        assertEquals("Germany", result.getCountryName());
        assertEquals(GameStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void checkAnswer_correctAnswer_shouldGiveCorrectPoints() {
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(gameConfig.getPointsCorrect()).thenReturn(10);
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GameSession result = gameService.checkAnswer(1L, "Germany");

        assertTrue(result.getIsCorrect());
        assertEquals(10, result.getPoints());
        assertEquals(GameStatus.COMPLETED, result.getStatus());

        verify(userService).updateUserStats(user, true, 10);
    }

    @Test
    void checkAnswer_wrongAnswer_shouldGiveNegativePoints() {
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(gameConfig.getPointsIncorrect()).thenReturn(-5);
        when(gameSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GameSession result = gameService.checkAnswer(1L, "France");

        assertFalse(result.getIsCorrect());
        assertEquals(-5, result.getPoints());
        assertEquals(GameStatus.COMPLETED, result.getStatus());

        verify(userService).updateUserStats(user, false, -5);
    }
}
