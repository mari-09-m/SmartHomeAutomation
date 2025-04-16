package com.example.smarthome.gui;

import com.example.smarthome.climate.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ClimateControlPanel extends JPanel {

    private final JTextField roomField;
    private final JTextField tempField;
    private final JTextArea outputArea;

    // JWT secret key for signing
    private static final String JWT_SECRET = "thisisaverystrongkeyusedforhs256auth!";
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());

    public ClimateControlPanel() {
        setLayout(new BorderLayout());

        // Top input panel
        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        inputPanel.add(new JLabel("Room:"));
        roomField = new JTextField("Living Room");
        inputPanel.add(roomField);

        inputPanel.add(new JLabel("Temperature (°C):"));
        tempField = new JTextField("22.5");
        inputPanel.add(tempField);

        add(inputPanel, BorderLayout.NORTH);

        // Output log
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // Button
        JButton setButton = new JButton("Set Temperature");
        setButton.addActionListener(this::handleSetTemperature);  
        add(setButton, BorderLayout.SOUTH);
    }

    private void handleSetTemperature(ActionEvent e) { 
        String room = roomField.getText();
        float temperature;

        try {
            temperature = Float.parseFloat(tempField.getText());
        } catch (NumberFormatException ex) {
            outputArea.append("❌ Invalid temperature format.\n");
            return;
        }

        // 1. Generate JWT Token
        String jwt = Jwts.builder()
                .setSubject("climate-client")
                .setIssuer("smarthome")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000)) // valid 1 hour
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();

        // 2. Set up channel
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50052)
                .usePlaintext()
                .build();

        // 3. Attach JWT token to metadata
        Metadata metadata = new Metadata();
        Metadata.Key<String> jwtHeader = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(jwtHeader, "Bearer " + jwt);  

        // 4. Wrap the stub
        ClimateControlServiceGrpc.ClimateControlServiceBlockingStub stub =
                MetadataUtils.attachHeaders(ClimateControlServiceGrpc.newBlockingStub(channel), metadata);

        // 5. Make the RPC call
        TemperatureRequest request = TemperatureRequest.newBuilder()
                .setRoom(room)
                .setTargetTemperature(temperature)
                .build();

        try {
            TemperatureResponse response = stub.setTemperature(request);
            outputArea.append(" * " + response.getStatus() + "\n");
        } catch (Exception ex) {
            outputArea.append("Error: " + ex.getMessage() + "\n");
        } finally {
            try {
                channel.shutdown().awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
        }
    }
}
