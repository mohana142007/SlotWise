package com.slotwise.service;

import com.slotwise.dto.UserRegistrationDto;
import com.slotwise.entity.Role;
import com.slotwise.entity.User;
import com.slotwise.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(UserRegistrationDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (dto.getRole() != Role.ROLE_STUDENT && dto.getRole() != Role.ROLE_FACULTY) {
            throw new IllegalArgumentException("Please select Student or Faculty");
        }

        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with email " + normalizedEmail + " already exists");
        }

        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        User user = new User(dto.getFullName().trim(), normalizedEmail, hashedPassword, dto.getRole());
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public User createAdminIfNotExist(String fullName, String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> existing = userRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            return existing.get();
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);
        User admin = new User(fullName, normalizedEmail, hashedPassword, Role.ROLE_ADMIN);
        return userRepository.save(admin);
    }

    public long countUsers() {
        return userRepository.count();
    }
}
