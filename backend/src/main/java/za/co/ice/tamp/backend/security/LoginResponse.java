package za.co.ice.tamp.backend.security;

public record LoginResponse(String token, AuthUserResponse user) {
}
