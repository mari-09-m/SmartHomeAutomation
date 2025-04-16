package com.example.smarthome.security;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;

import com.example.smarthome.auth.AuthInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;

public class SmartSecurityServer {

    private static final Logger logger = LoggerFactory.getLogger(SmartSecurityServer.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50051;

        registerService("_grpc._tcp.local.", "SmartSecurityService", port);

        Server server = ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(
                        new SmartSecurityServiceImpl(), new AuthInterceptor())) 
                .build();

        logger.info("SmartSecurityService gRPC server is starting on port {}", port);
        server.start();
        server.awaitTermination();
    }

    private static void registerService(String type, String name, int port) {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            ServiceInfo serviceInfo = ServiceInfo.create(type, name, port, "Security monitoring service");
            jmdns.registerService(serviceInfo);
            logger.info("jmDNS registered service: {}", name);
        } catch (IOException e) {
            logger.error("jmDNS registration failed", e);
        }
    }

    public static class SmartSecurityServiceImpl extends SmartSecurityServiceGrpc.SmartSecurityServiceImplBase {

        @Override
        public void manageAccess(AccessRequest request, StreamObserver<AccessResponse> responseObserver) {
            String userId = request.getUserId();
            logger.info("Access requested for userId: {}", userId);

            // Handle the request for access (validation logic, etc.)
            AccessResponse response = AccessResponse.newBuilder()
                    .setGranted(true)  // Simulate granting access
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void getCameraFeed(CameraRequest request, StreamObserver<CameraResponse> responseObserver) {
            logger.info("Streaming camera feed for: {}", request.getCameraId());

            for (int i = 1; i <= 3; i++) {
                CameraResponse frame = CameraResponse.newBuilder()
                        .setImage("Frame " + i + " from camera " + request.getCameraId())
                        .build();
                responseObserver.onNext(frame);

                try {
                    Thread.sleep(1000); // Simulate streaming delay
                } catch (InterruptedException e) {
                    logger.error("Streaming error", e);
                }
            }

            responseObserver.onCompleted();
        }

        @Override
        public StreamObserver<SensorData> sendSensorData(StreamObserver<SummaryResponse> responseObserver) {
            return new StreamObserver<>() {
                StringBuilder summary = new StringBuilder();

                @Override
                public void onNext(SensorData data) {
                    logger.info("SensorData received: {} -> {}", data.getType(), data.getValue());
                    summary.append(data.getType()).append(": ").append(data.getValue()).append("\n");
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("SensorData stream error", t);
                }

                @Override
                public void onCompleted() {
                    SummaryResponse response = SummaryResponse.newBuilder()
                            .setSummary(summary.toString())
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
            };
        }

        @Override
        public StreamObserver<DoorStatus> liveDoorMonitor(StreamObserver<SecurityAlert> responseObserver) {
            return new StreamObserver<>() {
                @Override
                public void onNext(DoorStatus status) {
                    String doorId = status.getDoorId();
                    boolean isOpen = status.getIsOpen();

                    String message = isOpen
                            ? " Door " + doorId + " is OPEN!"
                            : " Door " + doorId + " is CLOSED.";

                    logger.info("🚪 DoorStatus update: {}", message);

                    SecurityAlert alert = SecurityAlert.newBuilder()
                            .setMessage(message)
                            .build();

                    responseObserver.onNext(alert);
                }

                @Override
                public void onError(Throwable t) {
                    logger.error(" LiveDoorMonitor stream error", t);
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
