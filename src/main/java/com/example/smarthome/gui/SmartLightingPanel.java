package com.example.smarthome.gui;

import com.example.smarthome.lighting.LightCommand;
import com.example.smarthome.lighting.LightStatus;
import com.example.smarthome.lighting.SmartLightingServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class SmartLightingPanel extends JPanel {

    private final JTextField roomField;
    private final JCheckBox toggleCheckBox;
    private final JTextArea outputArea;

    private static final String JWT_SECRET = "thisisaverystrongkeyusedforhs256auth!";
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());

    public SmartLightingPanel() {
        setLayout(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        inputPanel.add(new JLabel("Room:"));
        roomField = new JTextField("Living Room");
        inputPanel.add(roomField);

        inputPanel.add(new JLabel("Turn ON:"));
        toggleCheckBox = new JCheckBox();
        inputPanel.add(toggleCheckBox);

        // Button
        JButton toggleButton = new JButton("Toggle Light");
        toggleButton.addActionListener(new ToggleLightHandler());

        // Output
        outputArea = new JTextArea(5, 40);
        outputArea.setEditable(false);

        add(inputPanel, BorderLayout.NORTH);
        add(toggleButton, BorderLayout.CENTER);
        add(new JScrollPane(outputArea), BorderLayout.SOUTH);
    }

    private class ToggleLightHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String room = roomField.getText();
            boolean turnOn = toggleCheckBox.isSelected();

            // Generate JWT
            String jwt = Jwts.builder()
                    .setSubject("lighting-client")
                    .setIssuer("smarthome")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3600_000)) 
                    .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                    .compact();

            // Create channel
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50053)
                    .usePlaintext() 
                    .build();

            // Attach JWT to metadata
            Metadata metadata = new Metadata();
            Metadata.Key<String> jwtHeader = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(jwtHeader, "Bearer " + jwt);

            // Create stub with attached JWT metadata
            SmartLightingServiceGrpc.SmartLightingServiceBlockingStub stub =
                    MetadataUtils.attachHeaders(SmartLightingServiceGrpc.newBlockingStub(channel), metadata);

            // Prepare the request
            LightCommand command = LightCommand.newBuilder()
                    .setRoom(room)
                    .setTurnOn(turnOn)
                    .build();

            // Make the RPC call
            try {
                LightStatus status = stub.toggleLight(command);
                outputArea.setText("Status: Light in " + status.getRoom() +
                        " is " + (status.getIsOn() ? "ON" : "OFF") +
                        "\nTimestamp: " + status.getTimestamp());
            } catch (Exception ex) {
                ex.printStackTrace();
                outputArea.setText("Error: " + ex.getMessage());
            } finally {
                
                channel.shutdown();
            }
        }
    }
}
