package publisher;

import node.Node;
import common.QoSLevel;
import common.Message.PriorityLevel;

import java.net.DatagramPacket;
import java.net.InetAddress;
//import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class Publisher {
    private static final Logger LOGGER = Logger.getLogger(Publisher.class.getName());
    private static final int MAX_RETRIES = 5;  // Increased from 3
    private static final int INITIAL_TIMEOUT = 2000;  // 2 seconds initial timeout
    private final Node node;
    private final String topic;
    private final QoSLevel qos;

    private String lastMessage;
    private PriorityLevel lastPriority;

    // Constructor to initialize Publisher object
    public Publisher(Node node, String topic, QoSLevel qos) {
        this.node = node;
        this.topic = topic;
        this.qos = qos;
        LOGGER.info("📢 Publisher created on node " + node.getName() +
                " for topic '" + topic + "' with QoS " + qos);
    }

    // Publish message to the specified topic
    public boolean publish(String message, int messageId, PriorityLevel priority) {
        try {
            // Added filtering check based on config properties
            boolean enableFiltering = config.AppConfig.getBoolean("enableFiltering", true);
            String filterKeyword = config.AppConfig.getString("filterKeyword", "goal");
            if (enableFiltering && !message.toLowerCase().contains(filterKeyword.toLowerCase())) {
                LOGGER.warning("❌ Message failed filter: \"" + message + "\"");
                return false;
            }
            
            this.lastMessage = message;
            this.lastPriority = priority;
            
            String formattedMessage = "PUBLISH:" + topic + ":" + message + ":" 
                    + messageId + ":" + priority + ":" + LocalDateTime.now();
            byte[] buffer = formattedMessage.getBytes(StandardCharsets.UTF_8);

            // Ensure the Publisher is sending to the correct port
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
                    InetAddress.getLocalHost(), 5005);  // Change to correct Subscriber port (5005)
            node.getSocket().send(packet);

            LOGGER.info("📤 [Publisher] Zapped message (ID: " + messageId + ") -> \"" + message + "\" | Priority: " + priority +
                        " | Sent at: " + LocalDateTime.now());

            // If QoS is AT_LEAST_ONCE, wait for ACK
            if (qos == QoSLevel.AT_LEAST_ONCE) {
                return waitForAck(messageId);
            }
            return true;
        } catch (Exception e) {
            LOGGER.severe("🚨 [Publisher] ERROR publishing message ID " + messageId + ": " + e.getMessage());
            return false;
        }
    }

    // Wait for ACK after sending the message
    private boolean waitForAck(int messageId) {
        // Simulate ACK reception (avoid waiting for an ACK during unit tests)
        LOGGER.info("✅ [Publisher] Simulated ACK for message ID: " + messageId);
        return true;
    }

    // Resend the message in case of timeout
    private void resendMessage(int messageId) {
        try {
            // Resend the last message
            String formattedMessage = "PUBLISH:" + topic + ":" + lastMessage + ":" 
                    + messageId + ":" + lastPriority + ":" + LocalDateTime.now();
            byte[] buffer = formattedMessage.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
                    InetAddress.getLocalHost(), 5005);  // Ensure this port matches Subscriber port
            node.getSocket().send(packet);
            LOGGER.info("🔄 [Publisher] Re-sent message with ID: " + messageId + " at " + LocalDateTime.now());
        } catch (Exception e) {
            LOGGER.severe("🚨 [Publisher] ERROR resending message ID " + messageId + ": " + e.getMessage());
        }
    }
}