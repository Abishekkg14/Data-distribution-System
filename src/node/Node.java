package node;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Set;
import publisher.Publisher;
import subscriber.Subscriber;
import common.QoSLevel;
import java.time.Duration;
import java.util.logging.Logger;

public class Node {
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());
    private final String nodeName;
    private final DatagramSocket socket;

    public Node(String nodeName) {
        try {
            this.nodeName = nodeName;
            this.socket = new DatagramSocket();
            LOGGER.info("✅ Node " + nodeName + " started on port " + socket.getLocalPort());
        } catch (SocketException e) {
            LOGGER.severe("❌ Error creating node: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Node(String nodeName, int port) {
        try {
            this.nodeName = nodeName;
            this.socket = new DatagramSocket(port);
            LOGGER.info("✅ Node " + nodeName + " started on port " + port);
        } catch (SocketException e) {
            LOGGER.severe("❌ Error creating node: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Publisher createPublisher(String topic, QoSLevel qos) {
        return new Publisher(this, topic, qos);
    }

    public Subscriber createSubscriber(Set<String> topics, String filterKeyword, int port, 
            boolean highPriorityOnly, Duration timeLimit) {
        return new Subscriber(this, topics, filterKeyword, port, highPriorityOnly, timeLimit);
    }

    public Subscriber createSubscriber(String topic, String filterKeyword) {
        return new Subscriber(this, Set.of(topic), filterKeyword, getPort(), false, Duration.ZERO);
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public String getName() {
        return nodeName;
    }
}
