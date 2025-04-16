package com.example.smarthome.climate;

import com.example.smarthome.auth.AuthInterceptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;

public class ClimateControlService {

    private static final Logger logger = LoggerFactory.getLogger(ClimateControlService.class);

    public static void main(String[] args) throws Exception {
        int port = 50052;

        // Register the service with JmDNS
        registerService("_grpc._tcp.local.", "ClimateControlService", port);

        Server server = ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(new ClimateControlServiceImpl(), new AuthInterceptor()))
                .build();

        logger.info("ClimateControlService gRPC server is starting on port {}", port);
        server.start();
        server.awaitTermination();
    }

    // Register service with mDNS (local service discovery)
    private static void registerService(String type, String name, int port) {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            ServiceInfo info = ServiceInfo.create(type, name, port, "Climate control service");
            jmdns.registerService(info);
            logger.info("📡 Service registered via jmDNS: {}", name);
        } catch (IOException e) {
            logger.error("Failed to register service with jmDNS", e);
        }
    }

    public static class ClimateControlServiceImpl extends ClimateControlServiceGrpc.ClimateControlServiceImplBase {

        // Method for setting the temperature
        @Override
        public void setTemperature(TemperatureRequest request, StreamObserver<TemperatureResponse> responseObserver) {
            String room = request.getRoom();
            float temp = request.getTargetTemperature();

            logger.info("🔧 SetTemperature -> Room: {}, Target: {}°C", room, temp);

            TemperatureResponse response = TemperatureResponse.newBuilder()
                    .setStatus("Temperature set to " + temp + "°C in room " + room)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        // Method for getting the current temperature (server streaming)
        @Override
        public void getCurrentTemperature(TemperatureQuery request, StreamObserver<TemperatureData> responseObserver) {
            logger.info("📡 Streaming current temperature for room: {}", request.getRoom());

            TemperatureData data = TemperatureData.newBuilder()
                    .setRoom(request.getRoom())
                    .setCurrentTemperature(22.5f)
                    .setTimestamp(LocalDateTime.now().toString())
                    .build();

            responseObserver.onNext(data);
            responseObserver.onCompleted();
        }

        // Method for reporting sensor readings (client streaming)
        @Override
        public StreamObserver<SensorReading> reportSensorReadings(StreamObserver<SensorSummary> responseObserver) {
            return new StreamObserver<>() {
                StringBuilder summary = new StringBuilder();

                @Override
                public void onNext(SensorReading value) {
                    logger.info("SensorReading -> {}: {}", value.getType(), value.getValue());
                    summary.append(value.getType()).append(": ").append(value.getValue()).append("\n");
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("Error in reportSensorReadings stream", t);
                }

                @Override
                public void onCompleted() {
                    SensorSummary result = SensorSummary.newBuilder()
                            .setSummary(summary.toString())
                            .build();
                    responseObserver.onNext(result);
                    responseObserver.onCompleted();
                }
            };
        }

        // Method for temperature alerts (bidirectional streaming)
        @Override
        public StreamObserver<TemperatureData> temperatureAlerts(StreamObserver<AlertMessage> responseObserver) {
            return new StreamObserver<>() {
                @Override
                public void onNext(TemperatureData value) {
                    logger.info("Received temperature data: {}°C in {}", value.getCurrentTemperature(), value.getRoom());

                    if (value.getCurrentTemperature() > 30.0) {
                        AlertMessage alert = AlertMessage.newBuilder()
                                .setMessage("High temperature in " + value.getRoom())
                                .build();
                        logger.warn("Alert triggered for room: {}", value.getRoom());
                        responseObserver.onNext(alert);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("Error in temperatureAlerts stream", t);
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
