package com.slotwise.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated visitor redirected to login when accessing dashboard")
    void testUnauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Unauthenticated visitor redirected to login when accessing admin portal")
    void testUnauthenticatedAdminPortalAccessDenied() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "student@slotwise.edu", authorities = {"ROLE_STUDENT"})
    @DisplayName("Student user cannot access administrative operations (403 Forbidden)")
    void testStudentCannotAccessAdminOperations() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/resources"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "faculty@slotwise.edu", authorities = {"ROLE_FACULTY"})
    @DisplayName("Faculty can use booking features but cannot access administration")
    void testFacultyAccess() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@slotwise.edu", authorities = {"ROLE_ADMIN"})
    @DisplayName("Administrator can successfully access administrative operations")
    void testAdminCanAccessAdminOperations() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/resources"))
                .andExpect(status().isOk());
    }
}
