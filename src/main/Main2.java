package main;

import node.Node;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

public class Main2 {
    public static void main(String[] args) {
        try {
            // Try to create subscriber node with incrementing ports if default is busy
            Node node = null;
            int port = 5005;
            while (node == null && port < 5010) {
                try {
                    node = new Node("SportsSubscriber", port);
                    break;
                } catch (RuntimeException e) {
                    System.out.println("⚠️ Port " + port + " is busy, trying next port...");
                    port++;
                }
            }

            if (node == null) {
                throw new RuntimeException("Could not find available port between 5002-5010");
            }

            Set<String> topics = Set.of("Sports");
            String filterKeyword = "goal";
            Duration timeLimit = Duration.ofMinutes(10);

            System.out.println("✅ Subscriber started and listening on port " + port + " for topics: " + topics);

            // Active message receiving loop
            DatagramSocket socket = node.getSocket();
            byte[] buffer = new byte[1024];

            while (true) {
                try {
                    // Prepare to receive message
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    // Process received message
                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    String[] parts = message.split(":");
                    
                    if (parts.length >= 5) {
                        String content = parts[2];
                        String messageId = parts[3];
                        
                        System.out.println("📨 Received message: " + content);
                        
                        // Send acknowledgment
                        sendAck(socket, Integer.parseInt(messageId), packet);
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error processing message: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendAck(DatagramSocket socket, int messageId, DatagramPacket originalPacket) {
        try {
            String ack = "ACK:" + messageId;
            byte[] ackData = ack.getBytes(StandardCharsets.UTF_8);
            DatagramPacket ackPacket = new DatagramPacket(
                ackData, 
                ackData.length, 
                originalPacket.getAddress(), 
                originalPacket.getPort()
            );
            socket.send(ackPacket);
            System.out.println("✅ Sent ACK for message ID: " + messageId);
        } catch (Exception e) {
            System.err.println("❌ Error sending ACK: " + e.getMessage());
        }
    }
}
