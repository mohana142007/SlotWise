package com.slotwise.service;

import com.slotwise.dto.UserRegistrationDto;
import com.slotwise.entity.Role;
import com.slotwise.entity.User;
import com.slotwise.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationDto dto;

    @BeforeEach
    void setUp() {
        dto = new UserRegistrationDto();
        dto.setFullName("John Doe");
        dto.setEmail("john@slotwise.edu");
        dto.setRole(Role.ROLE_STUDENT);
        dto.setPassword("Secret@123");
        dto.setConfirmPassword("Secret@123");
    }

    @Test
    @DisplayName("User registration succeeds and securely hashes password")
    void testSuccessfulRegistration() {
        when(userRepository.existsByEmail("john@slotwise.edu")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("hashedSecret123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User registered = userService.registerUser(dto);

        assertNotNull(registered);
        assertEquals("John Doe", registered.getFullName());
        assertEquals("john@slotwise.edu", registered.getEmail());
        assertEquals("hashedSecret123", registered.getPassword());
        assertEquals(Role.ROLE_STUDENT, registered.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("User registration rejects duplicate email")
    void testDuplicateEmailRegistrationRejected() {
        when(userRepository.existsByEmail("john@slotwise.edu")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(dto));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("User registration rejects mismatched passwords")
    void testPasswordMismatchRejected() {
        dto.setConfirmPassword("DifferentPassword");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(dto));

        assertTrue(ex.getMessage().contains("Passwords do not match"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Faculty registration saves the faculty role")
    void testFacultyRegistration() {
        dto.setRole(Role.ROLE_FACULTY);
        when(userRepository.existsByEmail("john@slotwise.edu")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("hashedSecret123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User registered = userService.registerUser(dto);

        assertEquals(Role.ROLE_FACULTY, registered.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Public registration cannot create administrator accounts")
    void testAdministratorRegistrationRejected() {
        dto.setRole(Role.ROLE_ADMIN);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(dto));

        assertTrue(ex.getMessage().contains("Student or Faculty"));
        verify(userRepository, never()).save(any(User.class));
    }
}
