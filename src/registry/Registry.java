package registry;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Registry {
    private static final int REGISTRY_PORT = 5001;
    private static final Map<String, Set<InetAddress>> topicSubscribers = new HashMap<>();

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(REGISTRY_PORT)) {
            System.out.println("✅ Registry started on port: " + REGISTRY_PORT);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                processMessage(received, packet.getAddress(), socket);
            }
        } catch (Exception e) {
            System.err.println("❌ Registry Error: " + e.getMessage());
        }
    }

    private static void processMessage(String message, InetAddress senderAddress, DatagramSocket socket) {
        String[] parts = message.split(":");

        if (parts[0].equals("SUBSCRIBE") && parts.length == 2) {
            String topic = parts[1];
            topicSubscribers.computeIfAbsent(topic, k -> new HashSet<>()).add(senderAddress);
            System.out.println("✅ Subscriber registered for topic: " + topic);
        } else if (parts[0].equals("PUBLISH") && parts.length >= 4) {
            String topic = parts[1];
            String content = parts[2];
            int messageId = Integer.parseInt(parts[3]);

            System.out.println("📥 Registry received message: " + content);

            if (!topicSubscribers.containsKey(topic)) {
                System.out.println("⚠️ No subscribers for topic: " + topic);
                return;
            }

            for (InetAddress subscriber : topicSubscribers.get(topic)) {
                forwardMessage(socket, topic, content, messageId, subscriber);
            }
        }
    }

    private static void forwardMessage(DatagramSocket socket, String topic, String content, int messageId, InetAddress subscriberAddress) {
        try {
            String forwardedMessage = "PUBLISH:" + topic + ":" + content + ":" + messageId;
            byte[] buffer = forwardedMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, subscriberAddress, 5003);
            socket.send(packet);
            System.out.println("📤 Forwarded to Subscriber: " + content);
        } catch (Exception e) {
            System.err.println("❌ Error forwarding message: " + e.getMessage());
        }
    }
}
