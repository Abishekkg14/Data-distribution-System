package subscriber;

import node.Node;
import common.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Subscriber implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Subscriber.class.getName());
    private final Node node;
    private final Set<String> topics;
    private final DatagramSocket socket;
    private volatile boolean running;
    private final int port;
    private final String filterKeyword;
    private final boolean highPriorityOnly;
    private final Duration timeLimit;
    private Thread receiveThread;
    private final Map<Integer, RetryAck> pendingAcks = new ConcurrentHashMap<>();
    private Thread ackRetryThread;
    private static final int MAX_ACK_RETRIES = 5;
    private static final long ACK_RETRY_DELAY = 1000; // 1 second

    private static class RetryAck {
        final DatagramPacket packet;
        final int attempts;
        final LocalDateTime firstAttempt;

        RetryAck(DatagramPacket packet) {
            this.packet = packet;
            this.attempts = 0;
            this.firstAttempt = LocalDateTime.now();
        }

        RetryAck withIncrementedAttempts() {
            return new RetryAck(this.packet, this.attempts + 1, this.firstAttempt);
        }

        private RetryAck(DatagramPacket packet, int attempts, LocalDateTime firstAttempt) {
            this.packet = packet;
            this.attempts = attempts;
            this.firstAttempt = firstAttempt;
        }
    }

    public Subscriber(Node node, Set<String> topics, String filterKeyword, int port, 
            boolean highPriorityOnly, Duration timeLimit) {
        this.node = node;
        this.topics = topics;
        this.filterKeyword = filterKeyword;
        this.port = port;
        this.highPriorityOnly = highPriorityOnly;
        this.timeLimit = timeLimit;
        this.socket = node.getSocket();
        this.running = true;
        LOGGER.info("👂 Subscriber created on node " + node.getName() + 
                "\n📋 Configuration:" +
                "\n   Topics: " + topics + 
                "\n   Filter: '" + filterKeyword + "'" +
                "\n   Port: " + port + 
                "\n   High Priority Only: " + highPriorityOnly + 
                "\n   Time Limit: " + timeLimit);
        registerWithRegistry();
        startReceiving();
        startAckRetryThread();
    }
    
    private void registerWithRegistry() {
        try {
            for (String topic : topics) {
                String message = "SUBSCRIBE:" + topic;
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), port);
                socket.send(packet);
                LOGGER.info("✅ " + node.getName() + " registered for topic: " + topic);
            }
        } catch (IOException e) {
            LOGGER.severe("❌ Error registering with registry: " + e.getMessage());
        }
    }
    
    private void startReceiving() {
        receiveThread = new Thread(this);
        receiveThread.start();
    }
    
    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    
        while (running) {
            try {
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                LOGGER.info("📥 [Subscriber] Raw incoming message: \"" + received + "\"");
                String[] parts = received.split(":");
    
                if (parts.length == 5 && parts[0].equals("PUBLISH") && topics.contains(parts[1])) {
                    int messageId = Integer.parseInt(parts[3]);
                    String content = parts[2];
                    Message.PriorityLevel priority = Message.PriorityLevel.valueOf(parts[4]);
    
                    if (timeLimit != null && Duration.between(LocalDateTime.now(), LocalDateTime.now()).compareTo(timeLimit) > 0) {
                        LOGGER.info("⏳ [Subscriber-" + node.getName() + "] Skipping ancient message: \"" + content + "\"");
                        continue;
                    }
    
                    if (highPriorityOnly && priority != Message.PriorityLevel.HIGH) {
                        LOGGER.warning("⚠️ [Subscriber-" + node.getName() + "] Ditching low-power message: \"" + content + "\"");
                        continue;
                    }
    
                    if (filterKeyword == null || content.contains(filterKeyword)) {
                        LOGGER.info("🎉 [Subscriber-" + node.getName() + "] Cheers! Accepted message: \"" + content + 
                                "\" | Priority: " + priority + " (ID: " + messageId + ")");
                        sendAck(messageId, packet);
                    } else {
                        LOGGER.warning("🚫 [Subscriber-" + node.getName() + "] Rejected message (filter mismatch): \"" + content + "\"");
                    }
                }
            } catch (IOException e) {
                if (running)
                    LOGGER.severe("🚨 [Subscriber-" + node.getName() + "] ERROR receiving message: " + e.getMessage());
            }
        }
    }
    
    private void sendAck(int messageId, DatagramPacket packet) {
        try {
            String ack = "ACK:" + messageId;
            byte[] ackData = ack.getBytes(StandardCharsets.UTF_8);
            // Use the sender address and port from the received packet instead of hardcoding port 5002.
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), packet.getPort());
            pendingAcks.put(messageId, new RetryAck(ackPacket));
            socket.send(ackPacket);
            LOGGER.info("✅ [Subscriber-" + node.getName() + "] Sent ACK for message ID: " + messageId);
        } catch (IOException e) {
            LOGGER.severe("🚨 [Subscriber-" + node.getName() + "] ERROR sending ACK for message ID " + messageId + ": " + e.getMessage());
        }
    }
    
    private void startAckRetryThread() {
        ackRetryThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(ACK_RETRY_DELAY);
                    retryPendingAcks();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        ackRetryThread.setDaemon(true);
        ackRetryThread.start();
    }
    
    private void retryPendingAcks() {
        pendingAcks.forEach((messageId, retryAck) -> {
            if (retryAck.attempts < MAX_ACK_RETRIES) {
                try {
                    socket.send(retryAck.packet);
                    pendingAcks.put(messageId, retryAck.withIncrementedAttempts());
                    LOGGER.info("🔄 [Subscriber-" + node.getName() + "] Retrying ACK for message ID " + messageId +
                                " (Attempt " + (retryAck.attempts + 1) + " of " + MAX_ACK_RETRIES + ")");
                } catch (IOException e) {
                    LOGGER.severe("🚨 [Subscriber-" + node.getName() + "] ERROR retrying ACK for message ID " + messageId + ": " + e.getMessage());
                }
            } else {
                LOGGER.severe("🛑 [Subscriber-" + node.getName() + "] Gave up on message ID " + messageId + " after max retries.");
                pendingAcks.remove(messageId);
            }
        });
    }
    
    public void stop() {
        LOGGER.info("🛑 Stopping subscriber..." );
        running = false;
        if (ackRetryThread != null) {
            ackRetryThread.interrupt();
        }
    }
}
