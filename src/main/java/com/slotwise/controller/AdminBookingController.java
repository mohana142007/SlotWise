package com.slotwise.controller;

import com.slotwise.entity.Booking;
import com.slotwise.entity.BookingStatus;
import com.slotwise.entity.User;
import com.slotwise.service.BookingService;
import com.slotwise.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public AdminBookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @GetMapping
    public String listBookings(@RequestParam(value = "status", required = false) BookingStatus status,
                               Model model) {
        List<Booking> allBookings = bookingService.getAllBookings();
        if (status != null) {
            allBookings = allBookings.stream().filter(b -> b.getStatus() == status).toList();
        }

        model.addAttribute("bookings", allBookings);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", BookingStatus.values());
        return "admin/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable("id") String id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User admin = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Admin not found: " + authentication.getName()));
        try {
            bookingService.cancelBooking(id, admin);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Booking #" + id + " was cancelled by Administrator.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/bookings";
    }
}
