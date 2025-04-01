package common;

import java.time.LocalDateTime;

public class Message {
    public enum PriorityLevel {
        LOW, MEDIUM, HIGH
    }

    private final String content;
    private final PriorityLevel priority;
    private final LocalDateTime timestamp;

    public Message(String content, PriorityLevel priority) {
        this.content = content;
        this.priority = priority;
        this.timestamp = LocalDateTime.now();
    }

    public String getContent() {
        return content;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
