package com.cs.ge.services.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public String generateToken(final Authentication profile) {
        final Map<String, Object> claims = new HashMap<>();
        final ObjectMapper oMapper = new ObjectMapper();
        final Map<String, String> infos = oMapper.convertValue(profile.getPrincipal(), Map.class);
        claims.put("lastName", infos.get("lastName"));
        claims.put("firstName", infos.get("firstName"));
        claims.put("name", infos.get("firstName") + " " + infos.get("lastName"));
        claims.put("email", infos.get("email"));
        final Instant now = Instant.now();
        final String scope = profile.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        final JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(6, ChronoUnit.HOURS))
                .subject(profile.getName())
                .claim("scope", scope)
                .build();
        return this.jwtEncoder
                .encode(JwtEncoderParameters.from(jwtClaimsSet)).getTokenValue();
    }

    public String getUsernameFromToken(final String token) {
        return this.jwtDecoder.decode(token).getClaim("email");
    }

    private Instant getExpirationDateFromToken(final String token) {
        return this.jwtDecoder.decode(token).getExpiresAt();
    }

    public Boolean validateToken(final String token, final UserDetails userDetails) {
        final String username = this.getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !this.isTokenExpired(token));
    }

    private Boolean isTokenExpired(final String token) {
        final Instant expiration = this.getExpirationDateFromToken(token);
        return expiration.isBefore(Instant.now());
    }
}
