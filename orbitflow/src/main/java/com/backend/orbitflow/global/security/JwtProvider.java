package com.backend.orbitflow.global.security;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import com.backend.orbitflow.domain.user.enums.UserRole;;

@Component 
public class JwtProvider {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String REFRESH_HEADER= "RefreshToken";
    public static final String AUTHORIZATION_KEY = "auth";
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.access-token-expiration}")
    private long accessTokenTime;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenTime;

    @Value("${jwt.secret.key}")
    private String secretKey;
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(String uuid, String email, UserRole role) {

        Date date = new Date();
        
        return Jwts.builder()
                .subject(uuid)
                .claim("email", email)
                .claim(AUTHORIZATION_KEY, role)
                .expiration(new Date(date.getTime()+accessTokenTime))
                .issuedAt(date)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String uuid) {
        Date date = new Date();
        return Jwts.builder()
                .subject(uuid)
                .expiration(new Date(date.getTime() + refreshTokenTime))
                .issuedAt(date)
                .signWith(key)
                .compact();
    }

    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    public Claims getUserInfoFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
