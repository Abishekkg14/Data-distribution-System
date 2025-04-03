package config;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;

public class AppConfig {
    private static final Properties properties = new Properties();

    static {
        try {
            // Attempt to load config.properties from the classpath
            System.err.println("🔍 Looking for config.properties in classpath...");
            InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("config.properties");

            if (input == null) {
                System.err.println("❌ config.properties not found in classpath; trying file system...");
                input = new FileInputStream("src/main/resources/config.properties");
                if (input == null) {
                    throw new IOException("Configuration file not found: config.properties.");
                }
            }

            properties.load(input);
            System.err.println("✅ Successfully loaded config.properties.");
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to load configuration file", ex);
        }
    }

    // Get integer values from config with a default fallback
    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Invalid format for key: " + key + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    // Get long values from config with a default fallback
    public static long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        try {
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Invalid format for key: " + key + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    // Get string values from config with a default fallback
    public static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    // Get boolean values from config with a default fallback
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
}