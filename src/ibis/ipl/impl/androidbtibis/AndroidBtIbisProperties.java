package ibis.ipl.impl.androidbtibis;

import ibis.ipl.IbisProperties;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Properties management for Bluetooth Ibis. The
 * {@link #getDefaultProperties()} method obtains the properties in the
 * following order: first, some hardcoded properties are set. Next, a file
 * <code>ibis.properties</code> is searched for in the current directory,
 * the classpath, or the user home directory, in that order.
 * If found, it is read as a properties file, and the properties contained in
 * it are set, possibly overriding the hardcoded properties.
 * Finally, the system properties are obtained. These, too, may override
 * the properties set so far.
 */
public final class AndroidBtIbisProperties {

    private static final String PREFIX = IbisProperties.PREFIX;
    
    /** Property name for disabling bluetooth scanning. */
    public static final String BT_NOSCAN = PREFIX + "registry.bt.noscan";
    
    /** Property name for the UUID of the central registry server. */
    public static final String BT_CENTRAL_SERVER_UUID = PREFIX + "registry.bt.serveruuid";
    
    /** Property name for the UUID of the central registry client. */
    public static final String BT_CENTRAL_CLIENT_UUID = PREFIX + "registry.bt.clientuuid";
    
    /** Property name for the UUID of the BTIbis instances. */
    public static final String BT_IBIS_UUID = PREFIX + "ipl.impl.bt.uuid";
    
    /** List of {NAME, DESCRIPTION, DEFAULT_VALUE} for properties. */
    private static final String[][] propertiesList = new String[][] {
        { BT_CENTRAL_SERVER_UUID, "c34b1158823f4824bbc0fb33dd9dba06", "String: UUID of the central registry server" },
        { BT_CENTRAL_CLIENT_UUID, "c76212695c8a4e768d17739c555a4be0", "String: UUID of the central registry client" },
        { BT_IBIS_UUID, "abb93f68cacc4a6db43c7f9d4c3ca62d", "String: UUID of the Ibis instances" },
    };

    private static Properties defaultProperties;

    /**
     * Private constructor, to prevent construction of a BTIbisProperties object.
     */
    private AndroidBtIbisProperties() {
        // nothing
    }

    /**
     * Returns the hard-coded properties of BTIbis.
     * 
     * @return
     *          the resulting properties.
     */
    public static Properties getHardcodedProperties() {
        Properties properties = new Properties();

        for (String[] element : propertiesList) {
            if (element[1] != null) {
                properties.setProperty(element[0], element[1]);
            }
        }

        return properties;
    }

    /**
     * Returns a map mapping hard-coded property names to their descriptions.
     * 
     * @return
     *          the name/description map.
     */
    public static Map<String, String> getDescriptions() {
        Map<String, String> result = new LinkedHashMap<String, String>();

        for (String[] element : propertiesList) {
            result.put(element[0], element[2]);
        }

        return result;
    }

    /**
     * Adds the properties as loaded from the specified stream to the specified
     * properties.
     * 
     * @param inputStream
     *            the input stream.
     * @param properties
     *            the properties.
     */
    private static void load(InputStream inputStream, Properties properties) {
        if (inputStream != null) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                // ignored
            } finally {
                try {
                    inputStream.close();
                } catch (Throwable e1) {
                    // ignored
                }
            }
        }
    }

    /**
     * Loads properties from the standard configuration file locations.
     * @return properties loaded from the standard configuration file locations.
     * 
     */
    @SuppressWarnings("unchecked")
	public static synchronized Properties getDefaultProperties() {
        if (defaultProperties == null) {
            defaultProperties = getHardcodedProperties();

            // Load properties from the classpath
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            InputStream inputStream =
                classLoader.getResourceAsStream(IbisProperties.PROPERTIES_FILENAME);
            load(inputStream, defaultProperties);

            // See if there is an ibis.properties file in the current
            // directory.
            try {
                inputStream =
                    new FileInputStream(IbisProperties.PROPERTIES_FILENAME);
                load(inputStream, defaultProperties);
            } catch (FileNotFoundException e) {
                // ignored
            }

            Properties systemProperties = System.getProperties();

            // Then see if the user specified an properties file.
            String file =
                systemProperties.getProperty(IbisProperties.PROPERTIES_FILE);
            if (file != null) {
                try {
                    inputStream = new FileInputStream(file);
                    load(inputStream, defaultProperties);
                } catch (FileNotFoundException e) {
                    System.err.println("User specified preferences \"" + file
                            + "\" not found!");
                }
            }

            // Finally, add the properties from the command line to the result,
            // possibly overriding entries from file or the defaults.
            for (Enumeration<String> e = (Enumeration<String>)systemProperties.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement();
                String value = systemProperties.getProperty(key);
                defaultProperties.setProperty(key, value);
            }
        }

        return new Properties(defaultProperties);
    }

}
