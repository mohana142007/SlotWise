package com.slotwise.controller;

import com.slotwise.entity.Booking;
import com.slotwise.entity.Resource;
import com.slotwise.entity.ResourceType;
import com.slotwise.entity.Role;
import com.slotwise.entity.User;
import com.slotwise.repository.ResourceRepository;
import com.slotwise.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @BeforeEach
    @AfterEach
    void cleanUpTestUsers() {
        userRepository.findByEmail("newstudent@slotwise.edu").ifPresent(userRepository::delete);
        userRepository.findByEmail("newfaculty@slotwise.edu").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("Public index page renders with brand information")
    void testIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SlotWise")))
                .andExpect(content().string(containsString("Campus Resource Portal")));
    }

    @Test
    @DisplayName("Login page displays evaluation credentials")
    void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin@slotwise.edu")))
                .andExpect(content().string(containsString("student@slotwise.edu")));
    }

    @Test
    @DisplayName("Registration page loads with form inputs")
    void testRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create an Account")))
                .andExpect(content().string(containsString("Student")))
                .andExpect(content().string(containsString("Faculty")));
    }

    @Test
    @DisplayName("Student registration via HTTP POST succeeds and redirects to login")
    void testRegisterUserPost() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Test Student")
                        .param("email", "newstudent@slotwise.edu")
                        .param("role", "ROLE_STUDENT")
                        .param("password", "Pass@123")
                        .param("confirmPassword", "Pass@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));
    }

    @Test
    @DisplayName("Faculty registration via HTTP POST stores the selected role")
    void testFacultyRegisterUserPost() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Test Faculty")
                        .param("email", "newfaculty@slotwise.edu")
                        .param("role", "ROLE_FACULTY")
                        .param("password", "Pass@123")
                        .param("confirmPassword", "Pass@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        assertEquals(Role.ROLE_FACULTY,
                userRepository.findByEmail("newfaculty@slotwise.edu").orElseThrow().getRole());
    }

    @Test
    @WithMockUser(username = "student@slotwise.edu", authorities = {"ROLE_STUDENT"})
    @DisplayName("Authenticated student can access dashboard and view sample resources")
    void testDashboardAccess() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Seminar Room A")))
                .andExpect(content().string(containsString("Dashboard")));
    }

    @Test
    @WithMockUser(username = "student@slotwise.edu", authorities = {"ROLE_STUDENT"})
    @DisplayName("Authenticated student can browse resource catalog")
    void testResourceCatalogAccess() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Campus Resources")))
                .andExpect(content().string(containsString("Computer Lab 1")));
    }

    @Test
    @WithMockUser(username = "student@slotwise.edu", authorities = {"ROLE_STUDENT"})
    @DisplayName("Authenticated student can view My Bookings page")
    void testMyBookingsAccess() throws Exception {
        mockMvc.perform(get("/my-bookings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My Bookings")));
    }

    @Test
    @WithMockUser(username = "admin@slotwise.edu", authorities = {"ROLE_ADMIN"})
    @DisplayName("Administrator can view admin dashboard and statistics")
    void testAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Administrator Control Center")))
                .andExpect(content().string(containsString("Active Resources")));
    }

    @Test
    @WithMockUser(username = "admin@slotwise.edu", authorities = {"ROLE_ADMIN"})
    @DisplayName("Administrator can view all system bookings")
    void testAdminBookings() throws Exception {
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("All System Bookings")));
    }

    @Test
    @WithMockUser(username = "admin@slotwise.edu", authorities = {"ROLE_ADMIN"})
    @DisplayName("Administrator can load new resource form")
    void testAdminNewResourceForm() throws Exception {
        mockMvc.perform(get("/admin/resources/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add New Resource")));
    }
}
