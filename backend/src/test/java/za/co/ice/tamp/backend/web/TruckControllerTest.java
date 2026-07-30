package za.co.ice.tamp.backend.web;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.entity.Truck;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.TruckRepository;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;
import za.co.ice.tamp.backend.web.dto.CreateTruckRequest;
import za.co.ice.tamp.backend.web.dto.TruckResponse;
import za.co.ice.tamp.backend.web.dto.UpdateTruckRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class TruckControllerTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * trucks.transporter_id is a real foreign key to users(id), so every truck in these
     * tests needs a persisted user first — a random UUID is rejected by the database.
     * Email is randomised because users.email is unique and the container is shared
     * across all test classes.
     */
    private UUID persistTransporter() {
        User user = new User("Test Transporter", "transporter-" + UUID.randomUUID() + "@example.com",
                "not-a-real-hash", "TRANSPORTER");
        return userRepository.save(user).getId();
    }

    @Test
    void postTruck_createsAndReturns201() throws Exception {
        UUID transporterId = persistTransporter();
        CreateTruckRequest request = new CreateTruckRequest(
                transporterId,
                "CONTAINER",
                new BigDecimal("5000"),
                new BigDecimal("20"),
                "Johannesburg",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(48));

        String response = mockMvc.perform(post("/trucks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        TruckResponse truckResponse = objectMapper.readValue(response, TruckResponse.class);
        assertNotNull(truckResponse.id());
        assertEquals(transporterId, truckResponse.transporterId());
        assertEquals("CONTAINER", truckResponse.vehicleType());
        assertEquals("Johannesburg", truckResponse.currentCity());
    }

    @Test
    void getTruck_returnsExistingTruck() throws Exception {
        UUID transporterId = persistTransporter();
        Truck truck = new Truck(
                transporterId,
                "FLATBED",
                new BigDecimal("8000"),
                null,
                "Cape Town",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(72));
        Truck saved = truckRepository.save(truck);

        mockMvc.perform(get("/trucks/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.vehicleType").value("FLATBED"));
    }

    @Test
    void getTruck_notFound_returns404() throws Exception {
        UUID nonExistent = UUID.randomUUID();
        mockMvc.perform(get("/trucks/" + nonExistent))
                .andExpect(status().isNotFound());
    }

    @Test
    void listTrucksByTransporter_returnsMatchingTrucks() throws Exception {
        UUID transporterId1 = persistTransporter();
        UUID transporterId2 = persistTransporter();

        Truck truck1 = new Truck(transporterId1, "CURTAIN_SIDE", new BigDecimal("3000"),
                new BigDecimal("15"), "Durban", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(48));
        Truck truck2 = new Truck(transporterId1, "CONTAINER", new BigDecimal("5000"),
                new BigDecimal("20"), "Johannesburg", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(48));
        Truck truck3 = new Truck(transporterId2, "TANKER", new BigDecimal("10000"), null,
                "Pretoria", OffsetDateTime.now(), OffsetDateTime.now().plusHours(48));

        truckRepository.saveAll(List.of(truck1, truck2, truck3));

        String response = mockMvc.perform(get("/trucks?transporterId=" + transporterId1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<TruckResponse> trucks = objectMapper.readValue(response,
                objectMapper.getTypeFactory().constructCollectionType(List.class, TruckResponse.class));

        assertEquals(2, trucks.size());
        assertTrue(trucks.stream().allMatch(t -> t.transporterId().equals(transporterId1)));
    }

    @Test
    void patchTruck_updatesStatus() throws Exception {
        UUID transporterId = persistTransporter();
        Truck truck = new Truck(
                transporterId,
                "TIPPER",
                new BigDecimal("2000"),
                new BigDecimal("8"),
                "Soweto",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(24));
        Truck saved = truckRepository.save(truck);

        UpdateTruckRequest updateRequest = new UpdateTruckRequest("MATCHED");
        mockMvc.perform(patch("/trucks/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCHED"));

        Truck updated = truckRepository.findById(saved.getId()).orElseThrow();
        assertEquals("MATCHED", updated.getStatus());
    }

    @Test
    void postTruck_invalidRequest_returns400() throws Exception {
        // Missing required fields
        String invalidRequest = "{}";
        mockMvc.perform(post("/trucks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
}
