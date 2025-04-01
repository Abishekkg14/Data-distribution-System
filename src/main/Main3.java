package main;

import node.Node;
import common.QoSLevel;
import publisher.Publisher;
import common.Message.PriorityLevel;

public class Main3 {
    public static void main(String[] args) throws Exception {
        Node node = new Node("NewsNode");
        String topic = "News";
        QoSLevel qos = QoSLevel.AT_LEAST_ONCE;

        Publisher publisher = node.createPublisher(topic, qos);

        System.out.println("🚀 Publishing news messages...");
        publisher.publish("Breaking: Stock market crashes!", 201, PriorityLevel.HIGH);
        Thread.sleep(1000);
        publisher.publish("Weather update: Heavy rains expected!", 202, PriorityLevel.MEDIUM);
        Thread.sleep(1000);
        publisher.publish("Sports: Local football team wins!", 203, PriorityLevel.LOW);
        System.out.println("✅ News messages published.");
    }
}
