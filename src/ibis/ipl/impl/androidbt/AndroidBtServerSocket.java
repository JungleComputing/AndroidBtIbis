package ibis.ipl.impl.androidbt;

import ibis.util.ThreadPool;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

/**
 * Bluetooth accept server socket.
 */
class AndroidBtServerSocket implements Runnable {
    // Should we have a fixed UUID??? Or generate it and make it part of the
    // AndroidBtSocketAddress???
    static final UUID MYSERVICEUUID = new UUID(0x2d26618601fb47c2L, 0x8d9f10b8ec891363L);
    
    private final BluetoothAdapter localDevice; // local Bluetooth Manager
    private final BluetoothServerSocket serverSocket;
    
    private AndroidBtSocket socket = null;
    
    private boolean closed = false;
    
    private IOException ex = null;
    
    private final AndroidBtSocketAddress myAddress;

    AndroidBtServerSocket(BluetoothAdapter local) throws IOException {
        myAddress = new AndroidBtSocketAddress(local.getAddress());
        localDevice = local;
        serverSocket = localDevice.listenUsingRfcommWithServiceRecord("AndroidBtIbis", MYSERVICEUUID);

        // Create a new accept thread
        ThreadPool.createNew(this, "Bluetooth Accept Thread");
    }

    synchronized AndroidBtSocket accept() throws IOException {
        while (socket == null && ! closed) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignored
            }
        }
        if (ex != null) {
            IOException e = ex;
            ex = null;
            throw e;
        }
        if (socket == null && closed) {
            throw new IOException("Server socket is closed");
        }
        AndroidBtSocket sock = socket;
        socket = null;
        notifyAll();
        return sock;   
    }
    
    AndroidBtSocketAddress getLocalSocketAddress() {
    	return myAddress;
    }

    synchronized void close() throws java.io.IOException {
        closed = true;
        serverSocket.close();
        notifyAll();
    }
    
    synchronized void addLocalConnection(AndroidBtSocket sckt) {
        while (socket == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignored
            }
        }
        socket = sckt;
        notifyAll();
    }

    public void run() {
        for (;;) {
            BluetoothSocket sockt;
            try {
                sockt = serverSocket.accept();
            } catch (IOException e) {
                synchronized(this) {
                    if (! closed) {
                        ex = e;
                        notifyAll();
                    }
                    return;
                }
            }
            synchronized(this) {
                while (socket == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // ignored
                    }
                }
                socket = new AndroidBtSocket(sockt);
                notifyAll();
            }
        }
    }
}
