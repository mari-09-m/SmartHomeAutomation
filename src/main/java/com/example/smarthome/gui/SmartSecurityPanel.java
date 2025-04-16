package com.example.smarthome.gui;

import com.example.smarthome.security.SmartSecurityServiceGrpc;
import com.example.smarthome.security.AccessRequest;
import com.example.smarthome.security.AccessResponse;
import com.example.smarthome.security.CameraRequest;
import com.example.smarthome.security.CameraResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.Key;
import java.util.Iterator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;

public class SmartSecurityPanel extends JPanel {

    private final JTextField userIdField;
    private final JTextArea outputArea;

    private static final String JWT_SECRET = "thisisaverystrongkeyusedforhs256auth!";
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());

    public SmartSecurityPanel() {
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        inputPanel.add(new JLabel("User ID:"));
        userIdField = new JTextField("user123");
        inputPanel.add(userIdField);

        JButton accessButton = new JButton("Request Access");
        accessButton.addActionListener(new AccessHandler());

        JButton cameraButton = new JButton("View Camera Feed");
        cameraButton.addActionListener(new CameraHandler());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(accessButton);
        buttonPanel.add(cameraButton);

        outputArea = new JTextArea(10, 40);
        outputArea.setEditable(false);

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(new JScrollPane(outputArea), BorderLayout.SOUTH);
    }

    private class AccessHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                    .usePlaintext()
                    .build();

            SmartSecurityServiceGrpc.SmartSecurityServiceBlockingStub stub =
                    SmartSecurityServiceGrpc.newBlockingStub(channel);

            AccessRequest request = AccessRequest.newBuilder()
                    .setUserId(userIdField.getText())
                    .build();

            // Generate the JWT Token
            String jwt = generateJwt("user123");

            // Attach JWT to metadata
            Metadata metadata = new Metadata();
            Metadata.Key<String> jwtHeader = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(jwtHeader, "Bearer " + jwt);

            // Attach metadata with JWT to the stub
            SmartSecurityServiceGrpc.SmartSecurityServiceBlockingStub authStub =
                    MetadataUtils.attachHeaders(stub, metadata);

            try {
                AccessResponse response = authStub.manageAccess(request);
                outputArea.setText("Access granted: " + response.getGranted());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Oops! We couldn't connect to the Smart Security service.\n" +
                        "Please make sure it's running and try again.",
                        "Connection Issue",
                        JOptionPane.WARNING_MESSAGE);
            }

            channel.shutdown();
        }
    }

    private class CameraHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                    .usePlaintext()
                    .build();

            SmartSecurityServiceGrpc.SmartSecurityServiceBlockingStub stub =
                    SmartSecurityServiceGrpc.newBlockingStub(channel);

            CameraRequest request = CameraRequest.newBuilder()
                    .setCameraId("cam001")
                    .build();

            // Generate the JWT Token
            String jwt = generateJwt("user123");

            // Attach JWT to metadata
            Metadata metadata = new Metadata();
            Metadata.Key<String> jwtHeader = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(jwtHeader, "Bearer " + jwt);

            // Attach metadata with JWT to the stub
            SmartSecurityServiceGrpc.SmartSecurityServiceBlockingStub authStub =
                    MetadataUtils.attachHeaders(stub, metadata);

            try {
                Iterator<CameraResponse> frames = authStub.getCameraFeed(request);
                StringBuilder sb = new StringBuilder("Camera Feed:\n");
                while (frames.hasNext()) {
                    CameraResponse frame = frames.next();
                    sb.append(frame.getImage()).append("\n");
                }
                outputArea.setText(sb.toString());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Unable to fetch the camera feed at the moment.\n" +
                        "Please check if the Smart Security service is online.",
                        "Camera Feed Unavailable",
                        JOptionPane.WARNING_MESSAGE);
            }

            channel.shutdown();
        }
    }

    // Generate the JWT token
    private String generateJwt(String userId) {
        long nowMillis = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(nowMillis + 3600000)) // 1 hour
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
}
