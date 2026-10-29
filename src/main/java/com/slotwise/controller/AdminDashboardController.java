package com.slotwise.controller;

import com.slotwise.dto.DashboardStatsDto;
import com.slotwise.entity.Booking;
import com.slotwise.entity.Resource;
import com.slotwise.service.BookingService;
import com.slotwise.service.ResourceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final BookingService bookingService;
    private final ResourceService resourceService;

    public AdminDashboardController(BookingService bookingService, ResourceService resourceService) {
        this.bookingService = bookingService;
        this.resourceService = resourceService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStatsDto stats = bookingService.getDashboardStatistics();
        List<Booking> recentBookings = bookingService.getAllBookings().stream().limit(10).toList();
        List<Resource> resources = resourceService.findAll();

        model.addAttribute("stats", stats);
        model.addAttribute("recentBookings", recentBookings);
        model.addAttribute("resources", resources);

        return "admin/dashboard";
    }
}
