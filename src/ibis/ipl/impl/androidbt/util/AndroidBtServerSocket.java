package ibis.ipl.impl.androidbt.util;

import ibis.util.ThreadPool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bluetooth accept server socket.
 */
public class AndroidBtServerSocket implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AndroidBtServerSocket.class);
    
    private final BluetoothAdapter localDevice; // local Bluetooth Manager
    private BluetoothServerSocket btServerSocket;
    private final ServerSocket serverSocket;
    private final int port;
    private final UUID uuid;
    
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
        this(local, myUUID, 0);
    }
    
    public AndroidBtServerSocket(BluetoothAdapter local, UUID myUUID, int port) throws IOException {
        this.port = port;
        this.uuid = myUUID;
        localDevice = local;
        if (local != null) {
            btServerSocket = localDevice.listenUsingRfcommWithServiceRecord("Ibis", myUUID);
        } else {
            btServerSocket = null;
        }
        if (log.isDebugEnabled()) {
            log.debug("LocalBlueToothAdaptor: " + (local == null ? "null" : local.getAddress()));
        }
        // serverSocket = new ServerSocket(port);
        serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        myAddress = new AndroidBtSocketAddress(local == null ? null : local.getAddress(), myUUID, port);
        
        if (log.isDebugEnabled()) {
            log.debug("myAddress: " + myAddress);
        }

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
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (btServerSocket != null) {
                btServerSocket.close();
            }
            serverSocket.close();
        } finally {
            notifyAll();
        }
    }
    
    public void run() {
        boolean iAmBtAccepter = false;
        synchronized(this) {
            if (! btAccepting) {
                btAccepting = true;
                iAmBtAccepter = true;
            }
        }
        if (iAmBtAccepter && btServerSocket == null) {
            return;
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
                // Unlike TCP sockets, only one connection is accepted per channel. So,
                // immediately create a new one. TODO: is this reasonable? Or should we
                // multiplex all traffic over a single connection?
                try {
                    btServerSocket.close();
                    btServerSocket = localDevice.listenUsingRfcommWithServiceRecord("Ibis", uuid);
                } catch(IOException e) {
                    System.err.println("Oops: " + e);
                    e.printStackTrace(System.err);
                    // TODO: what now?
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
                    System.err.println("accept gave exception");
                    e.printStackTrace(System.err);
                    synchronized(this) {
                        if (! closed) {
                            ex = e;
                            notifyAll();
                        }
                        return;
                    }
                }
                System.err.println("We got a succesful accept!");
                synchronized(this) {
                    while (socket != null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            // ignored
                        }
                    }
                    System.err.println("Creating AndroisBtSocket ...");
                    socket = new AndroidBtSocket(sockt);
                    System.err.println("Notifying ...");
                    notifyAll();
                }
            }
        }
    }
}
