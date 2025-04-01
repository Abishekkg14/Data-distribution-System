package main;

import node.Node;
import common.QoSLevel;
import publisher.Publisher;
import common.Message.PriorityLevel;
import java.util.logging.Logger;
import config.AppConfig; // new configuration helper

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int MAX_RETRIES = AppConfig.getInt("maxRetries", 3);
    private static final long RETRY_DELAY = AppConfig.getLong("retryDelay", 1000);
    private static final String FILTER_KEYWORD = AppConfig.getString("filterKeyword", "goal");
    private static final boolean ENABLE_FILTERING = AppConfig.getBoolean("enableFiltering", true);

    private static boolean publishWithRetry(Publisher publisher, String message, int messageId, PriorityLevel priority) {
        if (ENABLE_FILTERING) {
            if (message.toLowerCase().contains(FILTER_KEYWORD.toLowerCase())) {
                LOGGER.info("✅ The message has successfully passed the filter");
            } else {
                LOGGER.warning("❌ The message has failed to pass the filter");
                return false;
            }
        }

        LOGGER.info("✅ Message approved" + (ENABLE_FILTERING ? " (contains keyword: \"" + FILTER_KEYWORD + "\")" : ""));

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                LOGGER.info("Attempt " + attempt + " of " + MAX_RETRIES);
                publisher.publish(message, messageId, priority); // Call publish without expecting a return value
                LOGGER.info("✅ Message ID " + messageId + " sent successfully on attempt " + attempt);
                return true; // Assume success if no exception is thrown
            } catch (Exception e) {
                LOGGER.severe("❌ Attempt " + attempt + " failed: " + e.getMessage());
            }

            // Wait before retrying
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOGGER.severe("❌ Retry interrupted");
                    return false;
                }
            }
        }

        LOGGER.severe("❌ No ACK received for message ID " + messageId + " after " + MAX_RETRIES + " attempts");
        return false;
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node("SportsNode");
        String topic = "Sports";
        QoSLevel qos = QoSLevel.AT_LEAST_ONCE;

        LOGGER.info("🌅 [Main] Rise & shine! Waiting 5000ms for subscribers to awaken...");
        Thread.sleep(5000);

        Publisher publisher = node.createPublisher(topic, qos);
        LOGGER.info("🚀 [Main] Time to unleash messages on the '" + topic + "' channel!");

        LOGGER.info("🌟 Emoji Test: 🎉 🚀 ✅ 🔄 💥");

        try {
            LOGGER.info("🎯 [Main] Launching HIGH priority missile: \"Messi scored a goal!\" (ID:101)");
            boolean ack1 = publishWithRetry(publisher, "Messi scored a goal!", 101, PriorityLevel.HIGH);
            LOGGER.info(ack1 ? "🥇 [Main] SUCCESS: Message 101 rocketed out!" 
                               : "💥 [Main] FAILURE: Message 101 fizzled after " + MAX_RETRIES + " tries.");
            Thread.sleep(2000);

            LOGGER.info("🎯 [Main] Launching HIGH priority missile: \"Ronaldo scored a goal !\" (ID:104)");
            boolean ack4 = publishWithRetry(publisher, "Ronaldo scored a goal !", 104, PriorityLevel.HIGH);
            LOGGER.info(ack4 ? "🥇 [Main] SUCCESS: Message 104 blasted off!" 
                               : "💥 [Main] FAILURE: Message 104 failed after " + MAX_RETRIES + " attempts.");
            Thread.sleep(2000);

            LOGGER.info("🧨 [Main] Firing MEDIUM priority rocket: \"Neymar has scored a goal!\" (ID:102)");
            boolean ack2 = publishWithRetry(publisher, "Neymar has scored a goal!", 102, PriorityLevel.MEDIUM);
            LOGGER.info(ack2 ? "🥇 [Main] SUCCESS: Message 102 soared high!" 
                               : "💥 [Main] FAILURE: Message 102 did not reach orbit after " + MAX_RETRIES + " ticks.");
            Thread.sleep(2000);

            LOGGER.info("🪂 [Main] Releasing LOW priority message: \"Maldini saved the ball so it is not a goal!\" (ID:103)");
            boolean ack3 = publishWithRetry(publisher, "Maldini saved the ball so it is not a goal!", 103, PriorityLevel.LOW);
            LOGGER.info(ack3 ? "🥇 [Main] SUCCESS: Message 103 glided out gracefully." 
                               : "💥 [Main] FAILURE: Message 103 stalled after " + MAX_RETRIES + " attempts.");

            LOGGER.info("🎯 [Main] Launching HIGH priority missile: \"Totti did not score !\" (ID:107)");
            boolean ack7 = publishWithRetry(publisher, "Totti did not score !", 107, PriorityLevel.HIGH);
            LOGGER.info(ack7 ? "🥇 [Main] SUCCESS: Message 107 flew true!"
                               : "💥 [Main] FAILURE: Message 107 faltered after " + MAX_RETRIES + " attempts.");
            Thread.sleep(2000);

            LOGGER.info("📊 [Main] ✨ Publishing Summary ✨:\n   - Message 101 (HIGH): " + (ack1 ? "Sent Successfully" : "Failed") +
                   "\n   - Message 102 (MEDIUM): " + (ack2 ? "Sent Successfully" : "Failed") +
                   "\n   - Message 103 (LOW): " + (ack3 ? "Sent Successfully" : "Failed") +
                   "\n   - Message 104 (HIGH): " + (ack4 ? "Sent Successfully" : "Failed") +
                   "\n   - Message 107 (HIGH): " + (ack7 ? "Sent Successfully" : "Failed"));
        } catch (Exception e) {
            LOGGER.severe("🔥 [Main] Oops, catastrophic error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

