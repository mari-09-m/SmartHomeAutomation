package com.example.smarthome.climate;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ClimateControlClient {

    public static void main(String[] args) throws InterruptedException {
        
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50052)
                .usePlaintext()
                .build();

        
        Key secretKey = Keys.hmacShaKeyFor(
                "thisisaverystrongkeyusedforhs256auth!".getBytes(StandardCharsets.UTF_8)
        );

        
        String jwt = Jwts.builder()
                .setSubject("climate-client")
                .setIssuer("smarthome")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000)) 
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        System.out.println(" Generated JWT: " + jwt);

        
        Metadata metadata = new Metadata();
        Metadata.Key<String> jwtHeader = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(jwtHeader, "Bearer " + jwt);

        
        ClimateControlServiceGrpc.ClimateControlServiceBlockingStub blockingStub =
                MetadataUtils.attachHeaders(
                        ClimateControlServiceGrpc.newBlockingStub(channel), metadata);

        // Unary RPC: SetTemperature
        TemperatureRequest request = TemperatureRequest.newBuilder()
                .setRoom("Living Room")
                .setTargetTemperature(22.5f)
                .build();

        try {
            TemperatureResponse response = blockingStub.setTemperature(request);
            System.out.println("[Unary] SetTemperature response: " + response.getStatus());
        } catch (Exception e) {
            System.err.println("Error during setTemperature RPC: " + e.getMessage());
        }

        // Server Streaming RPC: GetCurrentTemperature
        TemperatureQuery query = TemperatureQuery.newBuilder()
                .setRoom("Living Room")
                .build();

        try {
            System.out.println("[Server Streaming] Temperature updates:");
            blockingStub.getCurrentTemperature(query).forEachRemaining(temp -> {
                System.out.println("  - " + temp.getCurrentTemperature() + "°C at " + temp.getTimestamp());
            });
        } catch (Exception e) {
            System.err.println("Error during getCurrentTemperature RPC: " + e.getMessage());
        }

        
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
