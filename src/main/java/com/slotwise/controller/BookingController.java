package com.slotwise.controller;

import com.slotwise.dto.BookingRequestDto;
import com.slotwise.entity.Booking;
import com.slotwise.entity.Resource;
import com.slotwise.entity.User;
import com.slotwise.exception.BookingConflictException;
import com.slotwise.exception.InvalidBookingException;
import com.slotwise.service.BookingService;
import com.slotwise.service.ResourceService;
import com.slotwise.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final ResourceService resourceService;
    private final UserService userService;

    public BookingController(BookingService bookingService,
                             ResourceService resourceService,
                             UserService userService) {
        this.bookingService = bookingService;
        this.resourceService = resourceService;
        this.userService = userService;
    }

    @GetMapping("/resources/{id}/book")
    public String showBookingForm(@PathVariable("id") String id,
                                  @RequestParam(value = "date", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @RequestParam(value = "startTime", required = false)
                                  @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
                                  @RequestParam(value = "endTime", required = false)
                                  @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
                                  Model model) {
        Resource resource = resourceService.getById(id);
        if (!resource.isActive()) {
            throw new InvalidBookingException("Cannot book an inactive resource.");
        }

        BookingRequestDto bookingRequest = new BookingRequestDto();
        bookingRequest.setResourceId(id);
        bookingRequest.setBookingDate(date != null ? date : LocalDate.now().plusDays(1));
        bookingRequest.setStartTime(startTime != null ? startTime : LocalTime.of(10, 0));
        bookingRequest.setEndTime(endTime != null ? endTime : LocalTime.of(11, 0));

        model.addAttribute("resource", resource);
        model.addAttribute("bookingRequest", bookingRequest);
        return "bookings/form";
    }

    @PostMapping("/bookings")
    public String submitBooking(@Valid @ModelAttribute("bookingRequest") BookingRequestDto bookingRequest,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Resource resource = resourceService.getById(bookingRequest.getResourceId());

        if (bindingResult.hasErrors()) {
            model.addAttribute("resource", resource);
            return "bookings/form";
        }

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + authentication.getName()));

        try {
            bookingService.createBooking(bookingRequest, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Success! Your booking for " + resource.getName() + " on " +
                    bookingRequest.getBookingDate() + " (" + bookingRequest.getStartTime() + " - " +
                    bookingRequest.getEndTime() + ") has been confirmed.");
            return "redirect:/my-bookings";
        } catch (BookingConflictException | InvalidBookingException ex) {
            model.addAttribute("resource", resource);
            model.addAttribute("errorMessage", ex.getMessage());
            return "bookings/form";
        }
    }

    @GetMapping("/my-bookings")
    public String myBookings(Authentication authentication, Model model) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + authentication.getName()));
        List<Booking> bookings = bookingService.getBookingsByUser(user);
        model.addAttribute("bookings", bookings);
        return "bookings/my-bookings";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable("id") String id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + authentication.getName()));
        try {
            bookingService.cancelBooking(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Booking #" + id + " has been cancelled successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/my-bookings";
    }
}
