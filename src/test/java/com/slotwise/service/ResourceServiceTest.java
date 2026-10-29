package com.slotwise.service;

import com.slotwise.dto.AvailabilityCheckDto;
import com.slotwise.dto.ResourceDto;
import com.slotwise.entity.Resource;
import com.slotwise.entity.ResourceType;
import com.slotwise.repository.BookingRepository;
import com.slotwise.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ResourceService resourceService;

    private Resource resource;

    @BeforeEach
    void setUp() {
        resource = new Resource("Computer Lab 1", ResourceType.ROOM, "40 Workstations", "IT Block", 40, true);
        resource.setId("res1");
    }

    @Test
    @DisplayName("Resource creation and retrieval")
    void testCreateAndRetrieveResource() {
        ResourceDto dto = new ResourceDto();
        dto.setName("Computer Lab 1");
        dto.setType(ResourceType.ROOM);
        dto.setDescription("40 Workstations");
        dto.setLocation("IT Block");
        dto.setCapacity(40);
        dto.setActive(true);

        when(resourceRepository.save(any(Resource.class))).thenReturn(resource);
        when(resourceRepository.findById("res1")).thenReturn(Optional.of(resource));

        Resource created = resourceService.createResource(dto);
        assertNotNull(created);
        assertEquals("Computer Lab 1", created.getName());

        Resource retrieved = resourceService.getById("res1");
        assertNotNull(retrieved);
        assertEquals(40, retrieved.getCapacity());
        assertTrue(retrieved.isActive());
    }

    @Test
    @DisplayName("Deactivate and reactivate resource status")
    void testToggleActiveStatus() {
        when(resourceRepository.findById("res1")).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean statusAfterFirstToggle = resourceService.toggleActiveStatus("res1");
        assertFalse(statusAfterFirstToggle);
        assertFalse(resource.isActive());

        boolean statusAfterSecondToggle = resourceService.toggleActiveStatus("res1");
        assertTrue(statusAfterSecondToggle);
        assertTrue(resource.isActive());
    }

    @Test
    @DisplayName("Search and filter resources")
    void testSearchAndFilterResources() {
        AvailabilityCheckDto filter = new AvailabilityCheckDto();
        filter.setQuery("Lab");
        filter.setType(ResourceType.ROOM);

        when(mongoTemplate.find(any(Query.class), eq(Resource.class)))
                .thenReturn(List.of(resource));

        List<Resource> results = resourceService.search(filter, true);
        assertEquals(1, results.size());
        assertEquals("Computer Lab 1", results.get(0).getName());
    }
}
