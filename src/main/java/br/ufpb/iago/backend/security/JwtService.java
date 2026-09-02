package br.ufpb.iago.backend.security;

import br.ufpb.iago.backend.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long accessTokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, Role role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmailFromToken(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRoleFromToken(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrai a data de expiração do token para armazenar na blacklist.
     */
    public LocalDateTime extractExpirationFromToken(String token) {
        Date expiration = extractClaims(token).getExpiration();
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}