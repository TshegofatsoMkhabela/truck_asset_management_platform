package za.co.ice.tamp.backend.acceptance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.co.ice.tamp.backend.MatchFixture;
import za.co.ice.tamp.backend.persistence.JpaTestBase;
import za.co.ice.tamp.backend.persistence.entity.Match;
import za.co.ice.tamp.backend.persistence.entity.User;

/**
 * Exercises the decision endpoint over real HTTP against the migrated schema, covering the
 * success path plus the three failure modes a caller can actually trigger: an unknown match,
 * a match already decided, and a decision value the schema would refuse.
 */
@AutoConfigureMockMvc
class AcceptanceControllerTest extends JpaTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MatchFixture fixture;

    @Test
    void acceptsAMatchAndReportsTheDecision() throws Exception {
        User owner = fixture.owner("ad-owner1@example.com");
        Match match = fixture.proposedMatch(owner, "ad-transporter1@example.com");

        mockMvc.perform(post("/matches/{id}/decision", match.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ACCEPTED", owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(match.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.decidedBy").value(owner.getId().toString()))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty());
    }

    @Test
    void returns404ForAMatchThatDoesNotExist() throws Exception {
        mockMvc.perform(post("/matches/{id}/decision", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ACCEPTED", UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns409WhenTheMatchWasAlreadyDecided() throws Exception {
        User owner = fixture.owner("ad-owner2@example.com");
        Match match = fixture.proposedMatch(owner, "ad-transporter2@example.com");

        mockMvc.perform(post("/matches/{id}/decision", match.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ACCEPTED", owner.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/matches/{id}/decision", match.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("REJECTED", owner.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void returns400ForADecisionValueTheSchemaWouldRefuse() throws Exception {
        // Without this validation the string reaches the database and fails on the
        // matches status CHECK, turning a caller typo into a 500 rather than a 400.
        User owner = fixture.owner("ad-owner3@example.com");
        Match match = fixture.proposedMatch(owner, "ad-transporter3@example.com");

        mockMvc.perform(post("/matches/{id}/decision", match.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("MAYBE", owner.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns400WhenTheActorDoesNotExist() throws Exception {
        // matches.decided_by and receipts.actor_id both reference users(id), so an actorId
        // naming nobody fails on a foreign key at flush time. Without a handler that is a
        // 500 carrying a Postgres constraint name, when the caller's actual mistake was
        // naming a user who does not exist. The match itself is valid here, which is what
        // makes this distinct from returns404ForAMatchThatDoesNotExist: that test passes a
        // random UUID for both arguments and so never reaches the actor at all.
        User owner = fixture.owner("ad-owner4@example.com");
        Match match = fixture.proposedMatch(owner, "ad-transporter4@example.com");

        mockMvc.perform(post("/matches/{id}/decision", match.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ACCEPTED", UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    private String body(String decision, UUID actorId) {
        return "{\"decision\": \"" + decision + "\", \"actorId\": \"" + actorId + "\"}";
    }
}
