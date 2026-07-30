package za.co.ice.tamp.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.entity.Load;
import za.co.ice.tamp.backend.persistence.entity.Match;
import za.co.ice.tamp.backend.persistence.entity.Rating;
import za.co.ice.tamp.backend.persistence.entity.Truck;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.LoadRepository;
import za.co.ice.tamp.backend.persistence.repository.MatchRepository;
import za.co.ice.tamp.backend.persistence.repository.RatingRepository;
import za.co.ice.tamp.backend.persistence.repository.TruckRepository;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;
import za.co.ice.tamp.backend.web.dto.RatingResponse;

@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RatingControllerTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoadRepository loadRepository;

    @Autowired
    private TruckRepository truckRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private RatingRepository ratingRepository;

    private UUID seedUsers() {
        String ownerEmail = "owner." + UUID.randomUUID() + "@test.com";
        String transporterEmail = "transporter." + UUID.randomUUID() + "@test.com";
        User owner = userRepository.save(
                new User("Owner", ownerEmail, "hash1", "FREIGHT_OWNER"));
        userRepository.save(
                new User("Transporter", transporterEmail, "hash2", "TRANSPORTER"));
        return owner.getId();
    }

    private UUID seedMatch() {
        UUID ownerId = seedUsers();
        User transporter = userRepository.findAll().stream()
                .filter(u -> "TRANSPORTER".equals(u.getRole()))
                .findFirst()
                .orElseThrow();

        Load load = loadRepository.save(new Load(
                ownerId, "Johannesburg", "Cape Town", "GENERAL",
                java.math.BigDecimal.valueOf(500),
                java.math.BigDecimal.valueOf(2.5),
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(6)));

        Truck truck = truckRepository.save(new Truck(
                transporter.getId(), "FLATBED",
                java.math.BigDecimal.valueOf(1000),
                java.math.BigDecimal.valueOf(5),
                "Cape Town",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(7)));

        Match match = matchRepository.save(new Match(
                load.getId(), truck.getId(), java.math.BigDecimal.valueOf(85),
                java.util.List.of("capacity sufficient")));

        return match.getId();
    }

    private String ratingBody(Short score, String comment) {
        return """
                {
                  "score": %d,
                  "comment": "%s"
                }
                """.formatted(score, comment != null ? comment : "");
    }

    @Test
    void submitsRatingForCompletedMatch() throws Exception {
        UUID matchId = seedMatch();
        User transporter = userRepository.findAll().stream()
                .filter(u -> "TRANSPORTER".equals(u.getRole()))
                .findFirst()
                .orElseThrow();
        User owner = userRepository.findAll().stream()
                .filter(u -> "FREIGHT_OWNER".equals(u.getRole()))
                .findFirst()
                .orElseThrow();

        MvcResult result = mockMvc.perform(
                        post("/matches/{matchId}/ratings", matchId)
                                .param("raterId", transporter.getId().toString())
                                .param("rateeId", owner.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ratingBody((short) 5, "Excellent service")))
                .andExpect(status().isCreated())
                .andReturn();

        RatingResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(), RatingResponse.class);

        List<Rating> persisted = ratingRepository.findByMatchId(matchId);
        assertThat(persisted).hasSize(1);
        Rating rating = persisted.get(0);
        assertThat(rating.getScore()).isEqualTo((short) 5);
        assertThat(rating.getComment()).isEqualTo("Excellent service");
        assertThat(rating.getRaterId()).isEqualTo(transporter.getId());
        assertThat(rating.getRateeId()).isEqualTo(owner.getId());
    }

    @Test
    void listRatingsForMatch() throws Exception {
        UUID matchId = seedMatch();
        User transporter = userRepository.findAll().stream()
                .filter(u -> "TRANSPORTER".equals(u.getRole()))
                .findFirst()
                .orElseThrow();
        User owner = userRepository.findAll().stream()
                .filter(u -> "FREIGHT_OWNER".equals(u.getRole()))
                .findFirst()
                .orElseThrow();

        // Submit rating from transporter to owner
        mockMvc.perform(
                        post("/matches/{matchId}/ratings", matchId)
                                .param("raterId", transporter.getId().toString())
                                .param("rateeId", owner.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ratingBody((short) 5, "Great")))
                .andExpect(status().isCreated());

        // Fetch all ratings for this match
        mockMvc.perform(get("/matches/{matchId}/ratings", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].score").value(5))
                .andExpect(jsonPath("$[0].comment").value("Great"));
    }

    @Test
    void listRatingsForUser() throws Exception {
        UUID matchId = seedMatch();
        User transporter = userRepository.findAll().stream()
                .filter(u -> "TRANSPORTER".equals(u.getRole()))
                .findFirst()
                .orElseThrow();
        User owner = userRepository.findAll().stream()
                .filter(u -> "FREIGHT_OWNER".equals(u.getRole()))
                .findFirst()
                .orElseThrow();

        // Submit rating from transporter to owner
        mockMvc.perform(
                        post("/matches/{matchId}/ratings", matchId)
                                .param("raterId", transporter.getId().toString())
                                .param("rateeId", owner.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ratingBody((short) 4, "Good")))
                .andExpect(status().isCreated());

        // Fetch all ratings for the owner (as ratee)
        mockMvc.perform(get("/users/{userId}/ratings", owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].score").value(4))
                .andExpect(jsonPath("$[0].rateeId").value(owner.getId().toString()));
    }
}
