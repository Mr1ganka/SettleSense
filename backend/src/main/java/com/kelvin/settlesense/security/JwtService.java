package com.kelvin.settlesense.security;

import com.kelvin.settlesense.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String key;

    @Value("${jwt.expiration}")
    private long expiration;



    public String fetchToken(HashMap<String, String> claims, String subject) {
        return generateToken(claims, subject);
    }

    public String fetchToken(String subject) {
        return generateToken(new HashMap<String, String>(), subject);
    }

    public String generateTokenWithDisplayName(Map<String, String> claims, User user) {
        HashMap<String, String> claimsWithDisplayName = new HashMap<>(claims);
        claimsWithDisplayName.put("email", user.getEmail());
        claimsWithDisplayName.put("displayName", user.getDisplayName());
        return generateToken(claimsWithDisplayName, user.getEmail());
    }

    public String extractDisplayName(String token) {
        return extractClaim(token, claims -> claims.get("displayName", String.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String generateToken(Map<String, String> claims, String subject) {

       return Jwts.builder()
               .claims(claims)
               .subject(subject)
               .issuedAt(new Date())
               .expiration(new Date(System.currentTimeMillis() + expiration))
               .signWith(getKey())
               .compact();

    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));
    }

    public boolean isTokenValid(String token, String subject) {
        try {
            String tokenSubject = extractClaim(token, Claims::getSubject);
            return subject.equals(tokenSubject) && !tokenHasExpired(token);
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    public boolean tokenHasExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
