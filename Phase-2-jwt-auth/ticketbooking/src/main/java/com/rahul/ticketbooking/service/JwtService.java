package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.entity.Person;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Person person) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())          // jti, used by the blacklist
                .subject(person.getUsername())
                .claim("role", person.getRole().name())
                .claim("userId", person.getId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    // Throws JwtException if the token is tampered with or expired
    public Claims parse(String token) {
        return Jwts.parser()
                    .verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
    }
}