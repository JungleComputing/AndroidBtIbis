package ibis.ipl.impl.androidbt.registry.central;

import ibis.ipl.impl.androidbt.util.AdaptorFinder;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dummy VirtualSocketFactory class that allows for less
 * changes in central registry code.
 */
public class VirtualSocketFactory {
    
    private static final Logger log = LoggerFactory.getLogger(VirtualSocketFactory.class);
    
    private Properties properties;
    private final VirtualSocketAddress address;
    
    private static final BluetoothAdapter adapter = AdaptorFinder.getBluetoothAdaptor();

    public VirtualSocketFactory(Properties properties, UUID base) {
	if (log.isDebugEnabled()) {
	    log.debug("adapter = " + (adapter == null ? "null" : adapter.getAddress()));
	}
        if (adapter == null) {
            this.address = new VirtualSocketAddress(null, base, 0);
            // throw new Error("Bluetooth device not supported");
        } else {
            this.address = new VirtualSocketAddress(adapter.getAddress(),
                base, 0);
        }
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public VirtualServerSocket createServerSocket(int virtualPort,
            int connectionBacklog, Object object) throws IOException {
        return new VirtualServerSocket(address, virtualPort);
    }

    public void end() {
        // TODO Auto-generated method stub
    }
}
