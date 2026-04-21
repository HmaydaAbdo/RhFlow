package com.hrflow.security.services;

import com.hrflow.security.dtos.LoginResponse;
import com.hrflow.roles.entities.Role;
import com.hrflow.users.entities.User;
import com.hrflow.users.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;

    @Value("${app.jwt.access-expiration:4}")
    private long accessExpiration;

    @Value("${app.jwt.refresh-expiration:7}")
    private long refreshExpiration;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 JwtEncoder jwtEncoder,
                                 JwtDecoder jwtDecoder,
                                 UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String password) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        log.info("Tentative de login email={}", normalizedEmail);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, password));

        User user = userRepository.findWithRolesByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .toList();

        Instant now = Instant.now();
        String accessToken = generateToken(now, normalizedEmail, roles,
                accessExpiration, ChronoUnit.HOURS, TOKEN_TYPE_ACCESS);
        String refreshToken = generateToken(now, normalizedEmail, roles,
                refreshExpiration, ChronoUnit.DAYS, TOKEN_TYPE_REFRESH);

        log.info("Login réussi email={} roles={}", normalizedEmail, roles);
        return new LoginResponse(accessToken, refreshToken, normalizedEmail,
                user.getFullName(), roles);
    }

    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String refreshToken) {
        Jwt jwt = jwtDecoder.decode(refreshToken);

        String type = jwt.getClaimAsString(CLAIM_TYPE);
        if (!TOKEN_TYPE_REFRESH.equals(type)) {
            throw new JwtException("Invalid token type: expected refresh token");
        }

        String email = jwt.getSubject();
        log.info("Refresh token demandé email={}", email);

        // On re-charge l'utilisateur depuis la DB pour rafraîchir les rôles
        // (principe : la source de vérité est la base, pas le token précédent).
        User user = userRepository.findWithRolesByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .toList();

        Instant now = Instant.now();
        String newAccessToken = generateToken(now, email, roles,
                accessExpiration, ChronoUnit.HOURS, TOKEN_TYPE_ACCESS);
        String newRefreshToken = generateToken(now, email, roles,
                refreshExpiration, ChronoUnit.DAYS, TOKEN_TYPE_REFRESH);

        return new LoginResponse(newAccessToken, newRefreshToken, email,
                user.getFullName(), roles);
    }

    private String generateToken(Instant issuedAt, String email,
                                 List<String> roles,
                                 long amount, ChronoUnit unit, String tokenType) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(amount, unit))
                .subject(email)
                .claim(CLAIM_ROLES, String.join(" ", roles))
                .claim(CLAIM_TYPE, tokenType)
                .build();

        JwtEncoderParameters params = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims);

        return jwtEncoder.encode(params).getTokenValue();
    }

    /**
     * Utilitaire pour convertir un claim string "a b c" en liste (utilisé par le converter).
     */
    public static List<String> splitClaim(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.trim().split("\\s+")).filter(s -> !s.isBlank()).toList();
    }
}
