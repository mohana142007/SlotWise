package com.slotwise.controller;

import com.slotwise.entity.Booking;
import com.slotwise.entity.Resource;
import com.slotwise.entity.User;
import com.slotwise.service.BookingService;
import com.slotwise.service.ResourceService;
import com.slotwise.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final BookingService bookingService;
    private final ResourceService resourceService;
    private final UserService userService;

    public DashboardController(BookingService bookingService,
                               ResourceService resourceService,
                               UserService userService) {
        this.bookingService = bookingService;
        this.resourceService = resourceService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        List<Booking> upcomingBookings = bookingService.getUpcomingBookingsForUser(user);
        List<Resource> featuredResources = resourceService.findAllActive();

        model.addAttribute("currentUser", user);
        model.addAttribute("upcomingBookings", upcomingBookings);
        model.addAttribute("featuredResources", featuredResources);
        model.addAttribute("activeResourceCount", resourceService.countActiveResources());
        model.addAttribute("userBookingCount", upcomingBookings.size());

        return "dashboard";
    }
}
