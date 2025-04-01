package common;

public enum QoSLevel {
    AT_MOST_ONCE,   // Fire and forget
    AT_LEAST_ONCE,  // Ensure delivery with ACKs
    EXACTLY_ONCE    // Future improvement (deduplication)
}
