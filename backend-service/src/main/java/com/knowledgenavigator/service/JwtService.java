package com.knowledgenavigator.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long expiration = 86400000; // 24 hours

    public String generateToken(String username, String role, List<String> groupIds) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("groups", groupIds)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            return !extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}