package ru.dushesalko.service;

import ru.dushesalko.model.User;
import ru.dushesalko.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getOrCreateUser_shouldCreateNewUser() {
        when(userRepository.findByChatId(1L)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User user = userService.getOrCreateUser(1L, "user", "Test", "User");

        assertNotNull(user);
        assertEquals("Test", user.getFirstName());
        verify(userRepository).save(any());
    }

    @Test
    void updateUserStats_shouldUpdateScore() {
        User user = User.builder()
                .totalScore(0)
                .correctAnswers(0)
                .incorrectAnswers(0)
                .build();

        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User updated = userService.updateUserStats(user, true, 10);

        assertEquals(10, updated.getTotalScore());
        assertEquals(1, updated.getCorrectAnswers());
    }
}
