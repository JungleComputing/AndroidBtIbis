package ibis.ipl.impl.androidbt.util;

import ibis.util.ThreadPool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

/**
 * Bluetooth accept server socket.
 */
public class AndroidBtServerSocket implements Runnable {

    private final BluetoothAdapter localDevice; // local Bluetooth Manager
    private final BluetoothServerSocket btServerSocket;
    private final ServerSocket serverSocket;
    private final int port;
    
    private AndroidBtSocket socket = null;
    
    private boolean closed = false;
    
    private IOException ex = null;
    
    private final AndroidBtSocketAddress myAddress;
    private boolean btAccepting;

    public AndroidBtServerSocket(BluetoothAdapter local) throws IOException {
             // new UUID(0x2d26618601fb47c2L, 0x8d9f10b8ec891363L);
        this(local, UUID.randomUUID());
    }
    
    public AndroidBtServerSocket(BluetoothAdapter local, UUID myUUID) throws IOException {
        localDevice = local;
        btServerSocket = localDevice.listenUsingRfcommWithServiceRecord("AndroidBtIbis", myUUID);
        serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        myAddress = new AndroidBtSocketAddress(local.getAddress(), myUUID, port);

        // Create a new accept thread, one for bluetooth, one for localhost.
        ThreadPool.createNew(this, "Accept Thread");
        ThreadPool.createNew(this, "Accept Thread");
    }

    public synchronized AndroidBtSocket accept() throws IOException {
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
    
    public AndroidBtSocketAddress getLocalSocketAddress() {
    	return myAddress;
    }

    public synchronized void close() throws java.io.IOException {
        closed = true;
        btServerSocket.close();
        notifyAll();
    }
    
    public synchronized void addLocalConnection(AndroidBtSocket sckt) {
        while (socket != null) {
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
        boolean iAmBtAccepter = false;
        synchronized(this) {
            if (! btAccepting) {
                btAccepting = true;
                iAmBtAccepter = true;
            }
        }
        for (;;) {
            if (iAmBtAccepter) {
                BluetoothSocket btSockt;
                try {
                    btSockt = btServerSocket.accept();
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
                    while (socket != null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            // ignored
                        }
                    }
                    socket = new AndroidBtSocket(btSockt);
                    notifyAll();
                }
            } else {
                Socket sockt;
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
                    while (socket != null) {
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
}
