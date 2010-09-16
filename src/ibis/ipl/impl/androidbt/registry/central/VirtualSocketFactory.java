package ibis.ipl.impl.androidbt.registry.central;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;

/**
 * A dummy VirtualSocketFactory class that allows for less
 * changes in central registry code.
 */
public class VirtualSocketFactory {
    
    private Properties properties;
    private final VirtualSocketAddress address;
    
    private final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

    public VirtualSocketFactory(Properties properties, UUID base) {
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
