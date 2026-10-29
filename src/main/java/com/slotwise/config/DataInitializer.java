package com.slotwise.config;

import com.slotwise.entity.Resource;
import com.slotwise.entity.ResourceType;
import com.slotwise.entity.Role;
import com.slotwise.entity.User;
import com.slotwise.repository.ResourceRepository;
import com.slotwise.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@slotwise.edu}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@1234}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    public DataInitializer(UserRepository userRepository,
                           ResourceRepository resourceRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initializeAdminUser();
        initializeDemoStudent();
        initializeSampleResources();
    }

    private void initializeAdminUser() {
        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (!userRepository.existsByEmail(normalizedEmail)) {
            User admin = new User(
                    adminName,
                    normalizedEmail,
                    passwordEncoder.encode(adminPassword),
                    Role.ROLE_ADMIN
            );
            userRepository.save(admin);
            logger.info("SlotWise: Initialized default administrator account: {}", normalizedEmail);
        }
    }

    private void initializeDemoStudent() {
        String demoStudentEmail = "student@slotwise.edu";
        if (!userRepository.existsByEmail(demoStudentEmail)) {
            User student = new User(
                    "Alex Johnson",
                    demoStudentEmail,
                    passwordEncoder.encode("Student@1234"),
                    Role.ROLE_STUDENT
            );
            userRepository.save(student);
            logger.info("SlotWise: Initialized demo student account: {}", demoStudentEmail);
        }
    }

    private void initializeSampleResources() {
        if (resourceRepository.count() == 0) {
            List<Resource> sampleResources = List.of(
                    new Resource("Seminar Room A", ResourceType.ROOM,
                            "Large air-conditioned seminar room equipped with smart podium and dual displays.",
                            "Building 1, 2nd Floor (Room 204)", 80, true),

                    new Resource("Conference Room", ResourceType.ROOM,
                            "Executive conference room featuring video conferencing setup, whiteboard, and round table.",
                            "Administration Wing, 1st Floor (Room 102)", 20, true),

                    new Resource("Computer Lab 1", ResourceType.ROOM,
                            "High-performance computer lab with 40 desktop workstations, gigabit network, and overhead projector.",
                            "IT Block, Ground Floor (Lab 003)", 40, true),

                    new Resource("Projector 1", ResourceType.EQUIPMENT,
                            "Portable 4K Laser Projector with HDMI, USB-C, and wireless screen casting.",
                            "AV Media Center, Counter A", null, true),

                    new Resource("Portable Speaker 1", ResourceType.EQUIPMENT,
                            "High-output portable PA speaker with 2 wireless microphones and Bluetooth connectivity.",
                            "Student Activities Office, Counter B", null, true)
            );

            resourceRepository.saveAll(sampleResources);
            logger.info("SlotWise: Loaded {} sample resources into persistent storage.", sampleResources.size());
        }
    }
}
