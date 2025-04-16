package com.example.smarthome.discovery;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;

public class ServiceDiscoveryClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
        String serviceType = "_grpc._tcp.local.";

        System.out.println("Looking for services of type: " + serviceType);

        jmdns.addServiceListener(serviceType, new ServiceListener() {

            @Override
            public void serviceAdded(ServiceEvent event) {
                // Trigger serviceResolved
                jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
            }

            @Override
            public void serviceRemoved(ServiceEvent event) {
                System.out.println("Service removed: " + event.getName());
            }

            @Override
            public void serviceResolved(ServiceEvent event) {
                ServiceInfo info = event.getInfo();
                System.out.println("Service resolved: " + info.getName());
                System.out.println("Host: " + info.getHostAddresses()[0]);
                System.out.println("Port: " + info.getPort());
                System.out.println("Type: " + info.getType());
                System.out.println("Desc: " + info.getNiceTextString());
            }
        });

        // Keep running to allow discovery
        Thread.sleep(5000); // Wait a few seconds to discover the service
        jmdns.close();
    }
}
