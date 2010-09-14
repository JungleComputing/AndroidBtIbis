package ibis.ipl.impl.androidbt.registry.central;

import ibis.ipl.impl.androidbt.util.AndroidBtServerSocket;
import ibis.ipl.impl.androidbt.util.AndroidBtSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.bluetooth.BluetoothAdapter;

public class VirtualServerSocket {
    
    private final AndroidBtServerSocket serverSocket;
    private final VirtualSocketAddress address;
    private static final Map<VirtualSocketAddress, VirtualServerSocket> map
            = new HashMap<VirtualSocketAddress, VirtualServerSocket>();
    
    public VirtualServerSocket(VirtualSocketAddress addr, int port) throws IOException {
        serverSocket = new AndroidBtServerSocket("AndroidBTRegistry",
                BluetoothAdapter.getDefaultAdapter(), addr.getUUID(), port);
        this.address = new VirtualSocketAddress(addr.getBtAddress(), addr.getUUID(),
                serverSocket.getLocalSocketAddress().getPort());
        synchronized(this.getClass()) {
            map.put(address, this);
        }
    }
    
    AndroidBtServerSocket getServerSocket() {
        return serverSocket;
    }

    public void addLocalConnection(AndroidBtSocket sckt) {
        serverSocket.addLocalConnection(sckt);
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
