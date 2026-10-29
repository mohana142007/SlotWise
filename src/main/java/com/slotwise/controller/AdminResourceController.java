package com.slotwise.controller;

import com.slotwise.dto.ResourceDto;
import com.slotwise.entity.Resource;
import com.slotwise.entity.ResourceType;
import com.slotwise.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/resources")
public class AdminResourceController {

    private final ResourceService resourceService;

    public AdminResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public String listResources(Model model) {
        List<Resource> resources = resourceService.findAll();
        model.addAttribute("resources", resources);
        return "admin/resources";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        ResourceDto dto = new ResourceDto();
        dto.setActive(true);
        model.addAttribute("resourceDto", dto);
        model.addAttribute("resourceTypes", ResourceType.values());
        model.addAttribute("isEdit", false);
        return "admin/resource-form";
    }

    @PostMapping("/new")
    public String createResource(@Valid @ModelAttribute("resourceDto") ResourceDto dto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("resourceTypes", ResourceType.values());
            model.addAttribute("isEdit", false);
            return "admin/resource-form";
        }

        Resource created = resourceService.createResource(dto);
        redirectAttributes.addFlashAttribute("successMessage",
                "Resource '" + created.getName() + "' created successfully!");
        return "redirect:/admin/resources";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        Resource resource = resourceService.getById(id);
        ResourceDto dto = new ResourceDto();
        dto.setId(resource.getId());
        dto.setName(resource.getName());
        dto.setType(resource.getType());
        dto.setDescription(resource.getDescription());
        dto.setLocation(resource.getLocation());
        dto.setCapacity(resource.getCapacity());
        dto.setActive(resource.isActive());

        model.addAttribute("resourceDto", dto);
        model.addAttribute("resourceTypes", ResourceType.values());
        model.addAttribute("isEdit", true);
        return "admin/resource-form";
    }

    @PostMapping("/{id}/edit")
    public String updateResource(@PathVariable("id") String id,
                                 @Valid @ModelAttribute("resourceDto") ResourceDto dto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("resourceTypes", ResourceType.values());
            model.addAttribute("isEdit", true);
            return "admin/resource-form";
        }

        Resource updated = resourceService.updateResource(id, dto);
        redirectAttributes.addFlashAttribute("successMessage",
                "Resource '" + updated.getName() + "' updated successfully!");
        return "redirect:/admin/resources";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        boolean active = resourceService.toggleActiveStatus(id);
        Resource resource = resourceService.getById(id);
        String statusText = active ? "activated" : "deactivated";
        redirectAttributes.addFlashAttribute("successMessage",
                "Resource '" + resource.getName() + "' has been " + statusText + ".");
        return "redirect:/admin/resources";
    }
}
