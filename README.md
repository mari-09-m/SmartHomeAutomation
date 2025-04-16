Smart Home Automation System
Overview
The Smart Home Automation System is designed to automate key functions in a home environment. It provides intelligent control over climate (temperature), lighting, and security systems. The system uses gRPC for communication between services, enabling real-time data exchange and control.
This project integrates three core services:
Climate Control: Manage temperature settings and monitor real-time updates.
Lighting Control: Toggle lights, monitor energy usage, and control lights in real-time.
Security Monitoring: Manage access control, view camera feeds, and monitor door status.

Additionally, this system features service discovery, secure communication using JWT and API keys, and a graphical user interface (GUI) for easy interaction with the services.

Features
#Real-Time Communication: Uses gRPC to implement four types of communication styles (Unary, Server Streaming, Client Streaming, and Bi-directional Streaming).
#Automatic Service Discovery: Uses jmDNS for automatically discovering services in the local network.
#Secure Authentication: Authentication using JWT and API keys for secure communication.
#Graphical User Interface (GUI): A user-friendly interface for controlling and monitoring the system's services.

Requirements
Java: JDK 17 or later
Maven: For building and managing dependencies
NetBeans (optional): IDE for managing the project
gRPC and Protobuf dependencies for communication

Setup Instructions
Clone the Repository:

bash:
git clone https://github.com/mar-09-m/SmartHomeAutomation.git
Install Dependencies: Ensure that you have Maven and JDK 17 installed. To build the project, navigate to the project directory and run:

nginx:
mvn clean install
Run the Services:

Navigate to the appropriate service directory (e.g., Climate Control, Lighting, Security).

Run the main class of each service. Example:

bash:
mvn exec:java -Dexec.mainClass="com.example.smarthome.climate.ClimateControlService"
mvn exec:java -Dexec.mainClass="com.example.smarthome.lighting.SmartLightingService"
mvn exec:java -Dexec.mainClass="com.example.smarthome.security.SmartSecurityServer"
Run the Client (GUI):

After running the services, run the GUI using the following:

bash:
mvn exec:java -Dexec.mainClass="com.example.smarthome.gui.SmartHomeGUI"
Service Communication
The system communicates using gRPC with the following services and RPC methods:

1. Climate Control Service
SetTemperature (Unary RPC): Set the desired temperature for a room.
GetCurrentTemperature (Server Streaming RPC): Receive real-time temperature updates.
ReportSensorReadings (Client Streaming RPC): Send multiple sensor readings and get a summary.
TemperatureAlerts (Bi-directional Streaming RPC): Stream temperature data and receive alerts when a threshold is crossed.

2. Lighting Control Service
ToggleLight (Unary RPC): Turn the light on or off.
StreamLightStatus (Server Streaming RPC): Continuously stream light status updates.
SendEnergyUsage (Client Streaming RPC): Stream energy usage data.
RealTimeLightControl (Bi-directional Streaming RPC): Control light status and receive updates in real-time.

3. Security Monitoring Service
ManageAccess (Unary RPC): Check access permissions for a user.
GetCameraFeed (Server Streaming RPC): Receive continuous camera feed frames.
SendSensorData (Client Streaming RPC): Stream sensor data to the server.
LiveDoorMonitor (Bi-directional Streaming RPC): Continuously stream door status and receive security alerts.

Error Handling:
The system is designed with error handling for remote invocations. Common errors include:

Authentication errors: Invalid JWT or missing API key results in UNAUTHENTICATED status.
Service errors: If a service is unavailable or returns an error, the client will be notified accordingly.

Security:
The system uses JWT (JSON Web Token) and API Keys for authentication, ensuring that only authorized clients can interact with the services.

Service Discovery:
The services are discoverable automatically using jmDNS (Java Multicast DNS), allowing for seamless registration and discovery of services within the local network.

GUI
The Graphical User Interface (GUI) allows users to interact with the services through a simple, intuitive interface:

Set the temperature for Climate Control.

Toggle lights and view energy usage in the Lighting section.

Request access and view camera feeds in the Security section.

GitHub Repository
The source code for this project is hosted on GitHub. You can find the project repository https://github.com/mari-09-m/SmartHomeAutomation/tree/master.

Conclusion
The Smart Home Automation System offers a seamless and secure solution for controlling and monitoring home environments. With real-time communication, automatic service discovery, and secure access control, it is an efficient and scalable system for home automation.
