package ibis.ipl.impl.androidbt.registry.central;

import ibis.ipl.impl.androidbt.util.AndroidBtServerSocket;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;

public class VirtualServerSocket {
    
    private final AndroidBtServerSocket serverSocket;
    private final VirtualSocketAddress address;
    
    public VirtualServerSocket(VirtualSocketAddress addr) throws IOException {
        this.address = addr;
        serverSocket = new AndroidBtServerSocket(BluetoothAdapter.getDefaultAdapter(), addr.getUUID());
    }
    
    AndroidBtServerSocket getServerSocket() {
        return serverSocket;
    }

    public void addLocalConnection(Connection sckt) {
        serverSocket.addLocalConnection(sckt.getSocket());
    }

    public void close() throws IOException {
        serverSocket.close();
    }

    public VirtualSocketAddress getLocalSocketAddress() {
        return address;
    }
}
