package za.co.ice.tamp.backend.security;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.ice.tamp.backend.persistence.entity.User;
import za.co.ice.tamp.backend.persistence.repository.UserRepository;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final JwtService jwtService;

    /**
     * A valid-format hash that matches no real password, computed once via the same
     * {@link PasswordEncoder} bean the rest of the class uses, rather than a second,
     * independently configured encoder. Compared against on an unknown email so that path costs
     * the same BCrypt work as a known email with a wrong password, closing the timing side
     * channel that would otherwise let an attacker infer whether an email is registered by
     * measuring response latency alone.
     */
    private final String dummyHashForTimingSafety;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuditService auditService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.jwtService = jwtService;
        this.dummyHashForTimingSafety = passwordEncoder.encode("no-account-uses-this-password");
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        User user = new User(
                request.fullName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role());
        user = userRepository.save(user);

        auditService.record(user.getId(), "REGISTERED", "User", user.getId(),
                Map.of("email", user.getEmail(), "role", user.getRole()));

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        String hashToCheck = user != null ? user.getPasswordHash() : dummyHashForTimingSafety;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !passwordMatches) {
            throw new BadCredentialsException("invalid email or password");
        }

        String token = jwtService.issueToken(user.getId(), user.getRole());
        auditService.record(user.getId(), "LOGGED_IN", "User", user.getId(), Map.of());

        return ResponseEntity.ok(new LoginResponse(token, UserResponse.from(user)));
    }
}
