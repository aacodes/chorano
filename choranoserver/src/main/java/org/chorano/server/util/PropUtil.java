package org.chorano.server.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Uses initialization on demand holder pattern to initialize a single properties object.
 * All property values are retrieved from this object.
 *
 * Reads properties from "properties" file from the classpath
 */
public final class PropUtil {

    private static final class PropertyServiceHolder {

        private static final Properties properties = new Properties();
        static {
            try (InputStreamReader reader =
                         new InputStreamReader(ClassLoader.getSystemResourceAsStream("properties"))) {
                properties.load(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getPropVal(String propertyName) {
        return PropertyServiceHolder.properties.getProperty(propertyName);
    }

    public static int getIntPropVal(String propertyName) {
        return Integer.valueOf(PropertyServiceHolder.properties.getProperty(propertyName));
    }
}
