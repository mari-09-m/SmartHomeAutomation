package com.example.smarthome.gui;

import javax.swing.*;

public class SmartHomeGUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SmartHomeGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Smart Home Automation System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // 🧩 Tabs for each service
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Climate Control", new ClimateControlPanel());
        tabbedPane.addTab("Lighting", new SmartLightingPanel()); // Will be replaced later
        tabbedPane.addTab("Security", new SmartSecurityPanel()); // Will be replaced later

        frame.add(tabbedPane);
        frame.setVisible(true);
    }
}
