package org.apache.asterix.external.feed.ml.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class ResourceHelper {

    static private final Logger LOGGER = Logger.getLogger(ResourceHelper.class.getName());

    public static String configLookup(String key) {
        final String propertyFile = "config.properties";
        try (InputStream inputStream = ResourceHelper.class.getClassLoader().getResourceAsStream(propertyFile)) {
            if (inputStream == null) {
                throw new FileNotFoundException("property file '" + propertyFile + "' not found in the classpath");
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            String property = properties.getProperty(key);
            if (property == null) {
                LOGGER.warning("unable to locate key '" + key + "' in " + propertyFile);
            }
            return property;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getResourcePath(String relativePath) {
        try {
            return ResourceHelper.class.getClassLoader().getResource(relativePath).getFile();
        } catch (NullPointerException e) {
            LOGGER.warning("Unable to load resource: " + relativePath);
        }
        return null;
    }

}
