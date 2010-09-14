package ibis.ipl.impl.androidbt.util;

import ibis.ipl.IbisFactory;
import ibis.ipl.impl.androidbt.registry.central.VirtualSocketFactory;
import ibis.ipl.impl.androidbt.registry.central.client.Registry;
import ibis.ipl.impl.androidbt.registry.central.server.CentralRegistryService;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IbisRegistry {

    private static final Logger logger = LoggerFactory.getLogger(IbisRegistry.class);
    
    private static final class RegistryProperties {

        public static final String PREFIX = "ibis.android.registry.";

        public static final String PORT = PREFIX + "port";
        
        public static final String MYUUID = PREFIX + "uuid";

        public static final String PRINT_EVENTS = PREFIX + "print.events";

        public static final String PRINT_STATS = PREFIX + "print.stats";

        public static final String PRINT_ERRORS = PREFIX + "print.errors";

        public static final String implementationVersion;

        public static final int DEFAULT_PORT = 8888;
        
        public static final String DEFAULT_UUID = "2d26618601fb47c28d9f10b8ec891364";

        static {
            String version = Registry.class.getPackage().getImplementationVersion();

            if (version == null || version.equals("0.0")) {
                // try to get version from IPL_MANIFEST file
                version = IbisFactory.getManifestProperty("support.version");
            }

            if (version == null) {
                throw new Error("Cannot get version for server");
            }

            implementationVersion = version;
        }

        private static final String[][] propertiesList = new String[][] {
                { PORT, Integer.toString(DEFAULT_PORT),
                        "Port which the registry binds to" },
                { MYUUID, DEFAULT_UUID, "UUID of the Android Ibis Registry" },                        
                { PRINT_EVENTS, "false",
                        "Boolean: if true, events of services are printed to standard out." },
                { PRINT_ERRORS, "false",
                        "Boolean: if true, details of errors (like stacktraces) are printed" },
                { PRINT_STATS, "false",
                        "Boolean: if true, statistics are printed to standard out regularly." },

        };

        public static TypedProperties getHardcodedProperties() {
            TypedProperties properties = new TypedProperties();

            for (String[] element : propertiesList) {
                if (element[1] != null) {
                    properties.setProperty(element[0], element[1]);
                }
            }

            return properties;
        }

        public static Map<String, String> getDescriptions() {
            Map<String, String> result = new LinkedHashMap<String, String>();

            for (String[] element : propertiesList) {
                result.put(element[0], element[2]);
            }

            return result;
        }
    }

    public IbisRegistry(Properties properties) throws IOException {
        // get default properties.
        TypedProperties typedProperties = RegistryProperties
                .getHardcodedProperties();

        // add specified properties
        typedProperties.addProperties(properties);

        if (logger.isDebugEnabled()) {
            TypedProperties serverProperties = typedProperties
                    .filter("ibis.android.registry");
            logger.debug("Settings for server:\n" + serverProperties);
        }
                
        UUID uuid = UUID.fromString(typedProperties.getProperty(RegistryProperties.MYUUID));
        VirtualSocketFactory factory = new VirtualSocketFactory(typedProperties, uuid);
        new CentralRegistryService(typedProperties, factory, null);
        if (logger.isDebugEnabled()) {
            logger.debug("Registry started");
        }
    }
}
