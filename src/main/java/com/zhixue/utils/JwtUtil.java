package com.zhixue.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET = "zhixueshuliSuperSecretKey2025!!!";
    
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private static final long EXPIRE =
            7 * 24 * 60 * 60 * 1000;

    public static String createToken(Long id,String username){

        return Jwts.builder()

                .claim("id", id)

                .claim("username", username)

                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + EXPIRE
                        )
                )

                .signWith(KEY)

                .compact();
    }

    public static Claims parseToken(String token){

        return Jwts.parserBuilder()

                .setSigningKey(KEY)

                .build()

                .parseClaimsJws(token)

                .getBody();
    }

    public static Long getUserId(String token){

        Claims claims = parseToken(token);

        Object id = claims.get("id");

        if(id instanceof Integer){
            return ((Integer) id).longValue();
        }

        if(id instanceof Long){
            return (Long) id;
        }

        return Long.parseLong(id.toString());
    }

    public static String getUsername(String token){

        Claims claims = parseToken(token);

        return claims.get("username").toString();
    }
}