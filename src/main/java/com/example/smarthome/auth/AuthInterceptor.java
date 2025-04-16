package com.example.smarthome.auth;

import io.grpc.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;

public class AuthInterceptor implements ServerInterceptor {

    private static final String JWT_SECRET = "thisisaverystrongkeyusedforhs256auth!";
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        Metadata.Key<String> jwtHeader = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        String jwtToken = headers.get(jwtHeader);

        if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
            jwtToken = jwtToken.substring(7);  

            try {
                // Validate the JWT token
                Jwts.parserBuilder()
                        .setSigningKey(SECRET_KEY)
                        .build()
                        .parseClaimsJws(jwtToken); 

                return next.startCall(call, headers); 

            } catch (JwtException e) {
                call.close(Status.UNAUTHENTICATED.withDescription("Invalid JWT: " + e.getMessage()), headers);
                return new ServerCall.Listener<>() {};
            }
        }

        call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid JWT"), headers);
        return new ServerCall.Listener<>() {}; // No JWT or invalid JWT, block the call
    }
}
