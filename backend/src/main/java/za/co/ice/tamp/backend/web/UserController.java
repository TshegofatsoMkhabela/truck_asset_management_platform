package za.co.ice.tamp.backend.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;
import za.co.ice.tamp.backend.web.dto.CreateUserRequest;
import za.co.ice.tamp.backend.web.dto.UpdateUserRequest;
import za.co.ice.tamp.backend.web.dto.UserResponse;

/**
 * CRUD (create, read, update, delete, though this issue builds only the first three) for
 * user profile and compliance data (FR-02, basic identity/compliance information).
 *
 * <p>Talks directly to {@link UserRepository} with no intervening service class: there is no
 * business rule here beyond validation and a not-found check, matching this codebase's
 * existing precedent ({@code IntegrationController} has no service layer either).
 *
 * <p>Deliberately unauthenticated: #9 (RBAC, role-based access control, and auth) owns role
 * enforcement and has not merged yet. The role each operation will eventually require is
 * documented in Step 5's OpenAPI annotations rather than enforced here.
 */
@RestController
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(
            summary = "Register a new user profile. Requires no role yet: RBAC (role-based "
                    + "access control) is added by #9; once merged this becomes an unauthenticated "
                    + "public endpoint, since registration itself has no caller to authenticate.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "fullName": "Jane Owner",
                              "email": "jane.owner@example.com",
                              "password": "s3cret-pass",
                              "role": "FREIGHT_OWNER"
                            }"""))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation failed, e.g. blank name or malformed email"),
            @ApiResponse(responseCode = "409", description = "A user with this email already exists")
    })
    @PostMapping("/users")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = new User(
                request.fullName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role());
        User saved = userRepository.save(user);
        // save() returns the entity as Hibernate holds it in the persistence context, which
        // never re-reads database-generated defaults (compliance_status, created_at,
        // updated_at) after an insert; only a fresh SELECT sees the values Postgres actually
        // wrote. findById forces exactly that read.
        User persisted = userRepository.findById(saved.getId()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(persisted));
    }

    @Operation(summary = "Fetch a user's profile and compliance status. Requires the caller's "
            + "own user id or ADMIN once #9's RBAC lands; unenforced today.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "No user with this id")
    })
    @GetMapping("/users/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Operation(
            summary = "Partially update a profile or set the compliance/verification status. "
                    + "Requires ADMIN for complianceStatus, or the profile's own owner for fullName, "
                    + "once #9's RBAC lands; unenforced today.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            { "complianceStatus": "APPROVED" }"""))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "404", description = "No user with this id")
    })
    @PatchMapping("/users/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        User user = findOrThrow(id);
        user.applyUpdate(request.fullName(), request.complianceStatus());
        return UserResponse.from(userRepository.save(user));
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Temporary, controller-scoped: #9 (RBAC and auth) owns the single global error shape
     * (timestamp, status, error code, message, field errors) that every endpoint should
     * eventually share. This handler exists only so #10 can be demoed and Swagger-tested
     * today; it is deleted, not merged around, the moment #9's {@code @ControllerAdvice}
     * lands, since that handler will cover this same exception type globally.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDuplicateEmail(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("A user with this email already exists");
    }
}
