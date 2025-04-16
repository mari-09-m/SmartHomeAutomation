package com.example.smarthome.lighting;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;

import com.example.smarthome.auth.AuthInterceptor;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartLightingService {

    private static final Logger logger = LoggerFactory.getLogger(SmartLightingService.class);

    public static void main(String[] args) throws Exception {
        int port = 50053;

        registerService("_grpc._tcp.local.", "SmartLightingService", port);

        Server server = ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(new SmartLightingServiceImpl(), new AuthInterceptor()))
                .build();

        logger.info("SmartLightingService gRPC server is starting on port {}", port);
        server.start();
        server.awaitTermination();
    }

    private static void registerService(String type, String name, int port) {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            ServiceInfo info = ServiceInfo.create(type, name, port, "Lighting control service");
            jmdns.registerService(info);
            logger.info("Service registered via jmDNS: {}", name);
        } catch (IOException e) {
            logger.error("Failed to register service via jmDNS", e);
        }
    }

    public static class SmartLightingServiceImpl extends SmartLightingServiceGrpc.SmartLightingServiceImplBase {

        @Override
        public void toggleLight(LightCommand request, StreamObserver<LightStatus> responseObserver) {
            String room = request.getRoom();
            boolean turnOn = request.getTurnOn();

            logger.info("ToggleLight: Room = {}, Status = {}", room, turnOn ? "ON" : "OFF");

            LightStatus status = LightStatus.newBuilder()
                    .setRoom(room)
                    .setIsOn(turnOn)
                    .setTimestamp(LocalDateTime.now().toString())
                    .build();

            responseObserver.onNext(status);
            responseObserver.onCompleted();
        }

        @Override
        public void streamLightStatus(LightStatusRequest request, StreamObserver<LightStatus> responseObserver) {
            logger.info("Streaming status for room: {}", request.getRoom());

            for (int i = 1; i <= 3; i++) {
                LightStatus status = LightStatus.newBuilder()
                        .setRoom(request.getRoom())
                        .setIsOn(i % 2 == 0)
                        .setTimestamp(LocalDateTime.now().toString())
                        .build();
                responseObserver.onNext(status);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("Streaming interrupted", e);
                }
            }

            responseObserver.onCompleted();
        }

        @Override
        public StreamObserver<EnergyUsageData> sendEnergyUsage(StreamObserver<EnergyUsageSummary> responseObserver) {
            return new StreamObserver<>() {
                StringBuilder summary = new StringBuilder();

                @Override
                public void onNext(EnergyUsageData data) {
                    logger.info("Energy usage: {} -> {} kWh", data.getDeviceId(), data.getEnergyUsed());
                    summary.append("Device: ").append(data.getDeviceId())
                           .append(", Energy Used: ").append(data.getEnergyUsed())
                           .append("\n");
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("Error receiving energy usage", t);
                }

                @Override
                public void onCompleted() {
                    EnergyUsageSummary usageSummary = EnergyUsageSummary.newBuilder()
                            .setSummary(summary.toString())
                            .build();
                    responseObserver.onNext(usageSummary);
                    responseObserver.onCompleted();
                }
            };
        }

        @Override
        public StreamObserver<LightCommand> realTimeLightControl(StreamObserver<LightStatus> responseObserver) {
            return new StreamObserver<>() {
                @Override
                public void onNext(LightCommand request) {
                    logger.info("RealTimeControl: {} -> {}", request.getRoom(), request.getTurnOn() ? "ON" : "OFF");

                    LightStatus status = LightStatus.newBuilder()
                            .setRoom(request.getRoom())
                            .setIsOn(request.getTurnOn())
                            .setTimestamp(LocalDateTime.now().toString())
                            .build();

                    responseObserver.onNext(status);
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("Real-time stream error", t);
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
