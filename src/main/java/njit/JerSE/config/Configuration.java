package njit.JerSE.config;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
    private final Properties properties;

    public Configuration() {
        properties = new Properties();

        Class<?> cls = getClass();
        ClassLoader classLoader = (cls == null) ? null : cls.getClassLoader();

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
            ex.printStackTrace();
        }
    }


    public @Nullable String getPropertyValue(String key) {
        return properties.getProperty(key);
    }
}