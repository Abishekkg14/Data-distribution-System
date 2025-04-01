package server;

import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class NewServer {
    private static final int SERVER_PORT = 5007;
    private static final int REGISTRY_PORT = 5001;
    private static final Map<Integer, Boolean> pendingAcks = new HashMap<>();

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("✅ Server started on port: " + SERVER_PORT);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("📥 Received: " + received);

                String[] parts = received.split(":");
                if (parts.length >= 4 && parts[0].equals("PUBLISH")) {
                    int messageId = Integer.parseInt(parts[3]);
                    forwardMessage(socket, received);
                    pendingAcks.put(messageId, false);
                } else if (parts.length == 2 && parts[0].equals("ACK")) {
                    handleAck(Integer.parseInt(parts[1]));
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Server Error: " + e.getMessage());
        }
    }

    private static void forwardMessage(DatagramSocket socket, String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket forwardPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), REGISTRY_PORT);
            socket.send(forwardPacket);
            System.out.println("📤 Forwarded message to Registry: " + message);
        } catch (Exception e) {
            System.err.println("❌ Error forwarding message to registry: " + e.getMessage());
        }
    }

    private static void handleAck(int messageId) {
        if (pendingAcks.containsKey(messageId)) {
            pendingAcks.put(messageId, true);
            System.out.println("✅ ACK received for message ID: " + messageId);
        } else {
            System.err.println("⚠️ Unexpected ACK received for message ID: " + messageId);
        }
    }

}
