package client;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;
import common.Message.PriorityLevel;

public class Client {
    private static final int REGISTRY_PORT = 5001; // ✅ Updated to match the correct Registry port
    private final DatagramSocket socket;

    public Client() throws IOException {
        socket = new DatagramSocket();
    }

    public void sendMessage(String topic, String content, PriorityLevel priority) {
        try {
            int messageId = (int) (Math.random() * 1000); // ✅ Random message ID for testing
            String message = "PUBLISH:" + topic + ":" + content + ":" + messageId + ":" + priority + ":" + java.time.LocalDateTime.now();
            byte[] buffer = message.getBytes();

            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length,
                    InetAddress.getLocalHost(),
                    REGISTRY_PORT
            );

            socket.send(packet);
            System.out.println("📤 Sent: " + content + " (Priority: " + priority + ")");
        } catch (IOException e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
        }
    }

    public void close() {
        socket.close();
        System.out.println("🚫 Client closed.");
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.sendMessage("Sports", "Messi scored a goal!", PriorityLevel.HIGH);
        client.sendMessage("Sports", "Penalty awarded to Neymar!", PriorityLevel.MEDIUM);
        client.sendMessage("Sports", "Goalkeeper saved the ball!", PriorityLevel.LOW);
        client.close();
    }
}