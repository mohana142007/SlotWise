package com.slotwise.controller;

import com.slotwise.dto.AvailabilityCheckDto;
import com.slotwise.entity.Booking;
import com.slotwise.entity.Resource;
import com.slotwise.entity.ResourceType;
import com.slotwise.service.ResourceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public String listResources(@ModelAttribute("filter") AvailabilityCheckDto filter, Model model) {
        List<Resource> resources = resourceService.search(filter, true);
        model.addAttribute("resources", resources);
        model.addAttribute("resourceTypes", ResourceType.values());
        model.addAttribute("hasTimeFilter", filter.hasTimeSlot());
        return "resources/list";
    }

    @GetMapping("/{id}")
    public String viewResource(@PathVariable("id") String id,
                               @RequestParam(value = "date", required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {
        Resource resource = resourceService.getById(id);
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        List<Booking> bookingsForDay = resourceService.getConfirmedBookingsForDate(id, targetDate);

        model.addAttribute("resource", resource);
        model.addAttribute("targetDate", targetDate);
        model.addAttribute("bookingsForDay", bookingsForDay);

        return "resources/detail";
    }
}
