package ibis.ipl.impl.androidbt.registry.central;

import java.util.Properties;

/**
 * A dummy VirtualSocketFactory class that allows for less
 * changes in central registry code.
 */
public class VirtualSocketFactory {
    private Properties properties;

    public VirtualSocketFactory(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public VirtualServerSocket createServerSocket(int virtualPort,
            int connectionBacklog, Object object) {
        // TODO Auto-generated method stub
        return null;
    }

    public void end() {
        // TODO Auto-generated method stub
        
    }
}
