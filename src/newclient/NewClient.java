package newclient;

import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NewClient {
    private static final Logger LOGGER = Logger.getLogger(NewClient.class.getName());
    private static final int REGISTRY_PORT = 5003;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            LOGGER.setLevel(Level.INFO);
            LOGGER.info("Client started, sending messages...");
            sendMessage(socket, "Sports", "Messi scored a goal!", 101);
            sendMessage(socket, "Sports", "Penalty awarded to Neymar!", 102);
            sendMessage(socket, "Sports", "Goalkeeper saved the ball!", 103);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error in client", e);
        }
    }

    private static void sendMessage(DatagramSocket socket, String topic, String message, int messageId) throws Exception {
        String msg = "PUBLISH:" + topic + ":" + message + ":" + messageId;
        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), REGISTRY_PORT);
        socket.send(packet);
        LOGGER.info("📤 Sent: " + message + " to topic: " + topic);
    }
}
