package com.example.smarthome.security;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.Iterator;

public class SmartSecurityClient {

    private static final String API_KEY = "my-secure-key-123"; // API Key for authentication
    private static final String JWT_SECRET = "thisisaverystrongkeyusedforhs256auth!"; // Secret key for JWT validation
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());

    public static void main(String[] args) throws InterruptedException {
        String target = "localhost:50051";

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();

        
        Metadata headers = new Metadata();

        
        Metadata.Key<String> apiKeyHeader = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(apiKeyHeader, API_KEY);

        // Create JWT token and set it in the headers
        String jwt = generateJwt("user123");  
        Metadata.Key<String> jwtHeader = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(jwtHeader, "Bearer " + jwt);  

        // Attach the headers to both blocking and async stubs
        SmartSecurityServiceGrpc.SmartSecurityServiceBlockingStub blockingStub =
                MetadataUtils.attachHeaders(
                        SmartSecurityServiceGrpc.newBlockingStub(channel), headers);

        SmartSecurityServiceGrpc.SmartSecurityServiceStub asyncStub =
                MetadataUtils.attachHeaders(
                        SmartSecurityServiceGrpc.newStub(channel), headers);

    
        testManageAccess(blockingStub);
        testGetCameraFeed(blockingStub);
        testSendSensorData(asyncStub);
        testLiveDoorMonitor(asyncStub);

        // Wait for async responses (client and bidirectional streaming) to complete
        Thread.sleep(3000);  

        // Clean up by shutting down the channel gracefully
        channel.shutdownNow().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    // Test Manage Access (Unary RPC)
    private static void testManageAccess(SmartSecurityServiceGrpc.SmartSecurityServiceBlockingStub stub) {
        AccessRequest request = AccessRequest.newBuilder()
                .setUserId("user123")
                .build();
        AccessResponse response = stub.manageAccess(request);
        System.out.println("Access granted: " + response.getGranted());
    }

    // Test Get Camera Feed (Server Streaming RPC)
    private static void testGetCameraFeed(SmartSecurityServiceGrpc.SmartSecurityServiceBlockingStub stub) {
        CameraRequest request = CameraRequest.newBuilder()
                .setCameraId("cam001")
                .build();

        Iterator<CameraResponse> responses = stub.getCameraFeed(request);
        while (responses.hasNext()) {
            System.out.println("Camera frame: " + responses.next().getImage());
        }
    }

    // Test Send Sensor Data (Client Streaming RPC)
    private static void testSendSensorData(SmartSecurityServiceGrpc.SmartSecurityServiceStub stub) {
        StreamObserver<SensorData> requestObserver = stub.sendSensorData(new StreamObserver<>() {
            @Override
            public void onNext(SummaryResponse value) {
                System.out.println("Sensor summary: " + value.getSummary());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Sensor stream error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Sensor data completed.");
            }
        });

        // Simulate sending multiple sensor data
        for (int i = 1; i <= 3; i++) {
            SensorData data = SensorData.newBuilder()
                    .setType("Motion")
                    .setValue(0.5f + i)
                    .setTimestamp("2025-04-11T19:5" + i + ":00")
                    .build();
            requestObserver.onNext(data);
        }
        requestObserver.onCompleted();
    }

    // Test Live Door Monitor (Bidirectional Streaming RPC)
    private static void testLiveDoorMonitor(SmartSecurityServiceGrpc.SmartSecurityServiceStub stub) {
        StreamObserver<DoorStatus> requestObserver = stub.liveDoorMonitor(new StreamObserver<>() {
            @Override
            public void onNext(SecurityAlert value) {
                System.out.println("Alert: " + value.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Door monitor error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Door monitor completed.");
            }
        });

        // Simulate sending multiple door statuses
        for (int i = 1; i <= 3; i++) {
            DoorStatus status = DoorStatus.newBuilder()
                    .setDoorId("door" + i)
                    .setIsOpen(i % 2 == 0)
                    .build();
            requestObserver.onNext(status);
        }

        requestObserver.onCompleted();
    }

    
    public static String generateJwt(String userId) {
        long nowMillis = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(nowMillis + 3600000))  // 1 hour
                .signWith(SignatureAlgorithm.HS256, JWT_SECRET.getBytes())
                .compact();
    }
}
