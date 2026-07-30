package za.co.ice.tamp.backend.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.persistence.entity.Rating;
import za.co.ice.tamp.backend.persistence.repository.RatingRepository;
import za.co.ice.tamp.backend.security.CurrentUser;
import za.co.ice.tamp.backend.web.dto.RatingRequest;
import za.co.ice.tamp.backend.web.dto.RatingResponse;

@RestController
public class RatingController {

    private final RatingRepository ratingRepository;

    public RatingController(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @PostMapping("/matches/{matchId}/ratings")
    @Operation(summary = "Submit a rating for a completed match",
            description = "Freight owner or transporter rates the other party after a trip. "
                    + "raterId and rateeId are caller-supplied query params rather than read from "
                    + "an authenticated identity; see known-limitations.md.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rating created",
                content = @Content(schema = @Schema(implementation = RatingResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed (score not 1-5)"),
        @ApiResponse(responseCode = "409", description = "Duplicate rating for this match by this rater")
    })
    public ResponseEntity<RatingResponse> submitRating(
            @PathVariable UUID matchId,
            @RequestParam(required = false) UUID raterId,
            @RequestParam UUID rateeId,
            @Valid @RequestBody RatingRequest request,
            Authentication authentication) {
        Rating rating = new Rating(matchId,
                CurrentUser.requireIdOrFallback(authentication, raterId, "raterId"),
                rateeId, request.score(), request.comment());
        Rating saved = ratingRepository.save(rating);
        return ResponseEntity.status(HttpStatus.CREATED).body(RatingResponse.from(saved));
    }

    @GetMapping("/matches/{matchId}/ratings")
    @Operation(summary = "List all ratings for a match",
            description = "Returns all feedback submitted for a completed match.")
    @ApiResponse(responseCode = "200", description = "Ratings retrieved")
    public List<RatingResponse> listRatingsForMatch(@PathVariable UUID matchId) {
        return ratingRepository.findByMatchId(matchId).stream()
                .map(RatingResponse::from)
                .toList();
    }

    @GetMapping("/users/{userId}/ratings")
    @Operation(summary = "List all ratings of a user",
            description = "Returns all feedback the user received from their partners.")
    @ApiResponse(responseCode = "200", description = "Ratings retrieved")
    public List<RatingResponse> listRatingsForUser(@PathVariable UUID userId) {
        return ratingRepository.findByRateeId(userId).stream()
                .map(RatingResponse::from)
                .toList();
    }
}
