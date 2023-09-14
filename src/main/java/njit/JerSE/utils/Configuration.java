package njit.JerSE.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to manage application-specific configurations.
 * <p>
 * This class reads properties from a {@code config.properties} file and
 * provides a mechanism to retrieve property values based on given keys.
 * It assumes that the {@code config.properties} file is located at the root
 * of the classpath.
 */
public class Configuration {
    private final Properties properties;

    /**
     * Initializes the Configuration by loading the properties from the {@code config.properties} file.
     */
    public Configuration() {
        properties = new Properties();

        Class<?> cls = getClass();
        ClassLoader classLoader = cls.getClassLoader();

        if (classLoader == null) {
            System.out.println("Class loader is null");
            return;
        }

        try (InputStream input = classLoader.getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Unable to find config.properties");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            System.out.println("Exception occurred while loading config.properties");
        }
    }

    /**
     * Retrieves the property value for a given key.
     *
     * @param key The key of the property to be retrieved.
     * @return The value associated with the provided key.
     * @throws IllegalArgumentException If the key is {@code null} or does not exist in the properties.
     */
    public String getPropertyValue(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Property key must not be null");
        }

        String props = properties.getProperty(key);
        if (props == null) {
            throw new IllegalArgumentException("Property key does not exist");
        }

        return props;
    }
}