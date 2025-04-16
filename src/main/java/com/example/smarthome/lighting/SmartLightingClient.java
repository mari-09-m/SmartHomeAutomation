package com.example.smarthome.lighting;

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

public class SmartLightingClient {

    public static void main(String[] args) throws InterruptedException {
        String target = "localhost:50053";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();

    
        Key secretKey = Keys.hmacShaKeyFor("thisisaverystrongkeyusedforhs256auth!".getBytes());

        String jwt = Jwts.builder()
                .setSubject("lighting-client")
                .setIssuer("smarthome")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        Metadata metadata = new Metadata();
        Metadata.Key<String> jwtHeader =
                Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(jwtHeader, "Bearer " + jwt);

        
        SmartLightingServiceGrpc.SmartLightingServiceBlockingStub blockingStub =
                MetadataUtils.attachHeaders(
                        SmartLightingServiceGrpc.newBlockingStub(channel), metadata);

        SmartLightingServiceGrpc.SmartLightingServiceStub asyncStub =
                MetadataUtils.attachHeaders(
                        SmartLightingServiceGrpc.newStub(channel), metadata);

        // Tests
        testToggleLight(blockingStub);
        testStreamLightStatus(blockingStub);
        testSendEnergyUsage(asyncStub);
        testRealTimeLightControl(asyncStub);

        
Thread.sleep(3000); // wait 3 seconds


channel.shutdownNow().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

    }

    private static void testToggleLight(SmartLightingServiceGrpc.SmartLightingServiceBlockingStub blockingStub) {
        LightCommand command = LightCommand.newBuilder()
                .setRoom("Living Room")
                .setTurnOn(true)
                .build();

        LightStatus status = blockingStub.toggleLight(command);
        System.out.println("Light status: " + (status.getIsOn() ? "ON" : "OFF") + " in " + status.getRoom());
    }

    private static void testStreamLightStatus(SmartLightingServiceGrpc.SmartLightingServiceBlockingStub blockingStub) {
        LightStatusRequest request = LightStatusRequest.newBuilder()
                .setRoom("Living Room")
                .build();

        blockingStub.streamLightStatus(request).forEachRemaining(status -> {
            System.out.println("Streaming light status: " + (status.getIsOn() ? "ON" : "OFF") + " in " + status.getRoom());
        });
    }

    private static void testSendEnergyUsage(SmartLightingServiceGrpc.SmartLightingServiceStub asyncStub) {
        StreamObserver<EnergyUsageSummary> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(EnergyUsageSummary value) {
                System.out.println("Energy usage summary: " + value.getSummary());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Energy usage data completed.");
            }
        };

        StreamObserver<EnergyUsageData> requestObserver = asyncStub.sendEnergyUsage(responseObserver);

        for (int i = 1; i <= 5; i++) {
            EnergyUsageData data = EnergyUsageData.newBuilder()
                    .setDeviceId("Device" + i)
                    .setEnergyUsed(10 + i)
                    .setTimestamp("2025-04-10T16:50:" + i)
                    .build();
            requestObserver.onNext(data);
        }

        requestObserver.onCompleted();
    }

    private static void testRealTimeLightControl(SmartLightingServiceGrpc.SmartLightingServiceStub asyncStub) {
        StreamObserver<LightStatus> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(LightStatus value) {
                System.out.println("Received light status: " + (value.getIsOn() ? "ON" : "OFF") + " in " + value.getRoom());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Real-time light control completed.");
            }
        };

        StreamObserver<LightCommand> requestObserver = asyncStub.realTimeLightControl(responseObserver);

        for (int i = 1; i <= 3; i++) {
            LightCommand command = LightCommand.newBuilder()
                    .setRoom("Living Room")
                    .setTurnOn(i % 2 == 0)
                    .build();
            requestObserver.onNext(command);
        }

        requestObserver.onCompleted();
    }
}
