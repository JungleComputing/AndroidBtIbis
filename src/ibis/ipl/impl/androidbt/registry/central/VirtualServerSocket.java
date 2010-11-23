package ibis.ipl.impl.androidbt.registry.central;

import ibis.ipl.impl.androidbt.util.AdaptorFinder;
import ibis.ipl.impl.androidbt.util.AndroidBtServerSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualServerSocket {
    
    private static final Logger log = LoggerFactory.getLogger(VirtualServerSocket.class);
        
    private final AndroidBtServerSocket serverSocket;
    private final VirtualSocketAddress address;
    private static final Map<VirtualSocketAddress, VirtualServerSocket> map
            = new HashMap<VirtualSocketAddress, VirtualServerSocket>();
    
    public VirtualServerSocket(VirtualSocketAddress addr, int port) throws IOException {
        serverSocket = new AndroidBtServerSocket(AdaptorFinder.getBluetoothAdaptor(),
                addr.getUUID(), port);
        this.address = new VirtualSocketAddress(addr.getBtAddress(), addr.getUUID(),
                serverSocket.getLocalSocketAddress().getPort());
        synchronized(this.getClass()) {
            map.put(address, this);
        }
    }
    
    AndroidBtServerSocket getServerSocket() {
        return serverSocket;
    }

    public void close() throws IOException {
        serverSocket.close();
    }

    public VirtualSocketAddress getLocalSocketAddress() {
        return address;
    }

    public static synchronized VirtualServerSocket findServer(VirtualSocketAddress address2) {
        return map.get(address2);
    }
}
