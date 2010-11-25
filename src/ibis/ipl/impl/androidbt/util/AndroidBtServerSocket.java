package ibis.ipl.impl.androidbt.util;

import ibis.util.ThreadPool;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
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
            log.debug("btServerSocket UUID = " + myUUID);
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
            if (log.isDebugEnabled()) {
        	log.debug("accept(): waiting for socket");
            }
            try {
                wait();
            } catch (InterruptedException e) {
                // ignored
            }
        }
        if (ex != null) {
            if (log.isDebugEnabled()) {
                log.debug("accept(): got exception, socket = " + socket, ex);
            }
            IOException e = ex;
            ex = null;
            throw e;
        }
        if (socket == null && closed) {
            throw new IOException("Server socket is closed");
        }
        if (log.isDebugEnabled()) {
            log.debug("accept(): got socket");
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
        } catch(Throwable e) {
            // ignore
        } finally {
            notifyAll();
        }
    }
    
    private void bluetoothAcceptor() {
	while (! closed) {
	    // Bluetooth can only handle a single connection on a particular rfcomm channel
	    // at a time. So, as soon as we have a connection, we close the server socket
	    // and create a new one (that should then pick a different channel ...).
	    
	    // O well, it does not, which means that a second connection setup will fail.
	    // So now connection setup becomes a two-stage process: first connect with the
	    // advertised server socket, which then sets up a new server socket with a new
	    // uuid, and sends this new uuid over the connection. Then, close the connection,
	    // on both sides. Client will then connect to the new server socket.
            BluetoothSocket btSockt;
            if (log.isDebugEnabled()) {
                log.debug("Bluetooth acceptor accepting ...");
            }
            try {
                btSockt = btServerSocket.accept();
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
            	log.debug("Bluetooth acceptor got exception", e);
                }
                synchronized(this) {
                    if (! closed) {
                        ex = e;
                        notifyAll();
                    }
                    break;
                }
            } /* finally {
        	try {
        	    btServerSocket.close();
        	} catch (IOException e) {
        	    // ignore
        	}

            } */

            // Now, generate a new UUID and listener, because a particular server socket
            // can only deal with a single connection at a time.
            UUID newUUID = UUID.randomUUID();
            BluetoothServerSocket newServer;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Listen to new UUID " + newUUID);
                }
                newServer = localDevice.listenUsingRfcommWithServiceRecord("Ibis", newUUID);
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(btSockt.getOutputStream()));
                o.writeUTF(newUUID.toString());
                o.close();
            } catch (IOException e) {
        	if (log.isDebugEnabled()) {
        	    log.debug("Bluetooth acceptor got exception", e);
        	}
        	synchronized(this) {
        	    if (! closed) {
        		ex = e;
        		notifyAll();
        	    }
        	}
        	break;
            } finally {
        	try {
        	    btSockt.close();
        	} catch (IOException e) {
        	    // ignore
        	}
            }
            // TODO: separate thread
            try {
        	btSockt = newServer.accept();
            } catch (IOException e) {
        	if (log.isDebugEnabled()) {
        	    log.debug("Bluetooth acceptor got exception", e);
        	}
        	synchronized(this) {
        	    if (! closed) {
        		ex = e;
        		notifyAll();
        	    }
        	}
        	break;
            } finally {
        	try {
        	    newServer.close();
        	} catch (IOException e) {
        	    // ignore
        	}
            }

            if (log.isDebugEnabled()) {
        	log.debug("Bluetooth acceptor accept returns!");
            }

            // Unlike TCP sockets, only one connection is accepted per channel. So,
            // immediately create a new one. TODO: is this reasonable? Or should we
            // multiplex all traffic over a single connection?
/*
            try {
        	btServerSocket = localDevice.listenUsingRfcommWithServiceRecord("Ibis", uuid);
            } catch (IOException e) {
        	if (log.isDebugEnabled()) {
        	    log.debug("Bluetooth acceptor got exception", e);
        	}
        	// TODO: what now?
            }
*/
            synchronized(this) {
        	while (socket != null) {
        	    if (log.isDebugEnabled()) {
        		log.debug("Bluetooth acceptor waiting for free socket");
        	    }
        	    try {
        		wait();
        	    } catch (InterruptedException e) {
        		// ignored
        	    }
        	}
        	if (log.isDebugEnabled()) {
        	    log.debug("Bluetooth acceptor notifying ...");
        	}
        	socket = new AndroidBtSocket(btSockt);
        	notifyAll();
            }
	}
	try {
	    btServerSocket.close();
	} catch(Throwable e) {
	    // ignore
	}
    }
    
    private void socketAcceptor() {
	while (! closed) {
            Socket sockt;
            if (log.isDebugEnabled()) {
                log.debug("Local acceptor accepting ...");
            }
            try {
                sockt = serverSocket.accept();
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Local acceptor got exception", e);
                }
                synchronized(this) {
                    if (! closed) {
                        ex = e;
                        notifyAll();
                    }
                    break;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Local acceptor accept returns!");
            }
            synchronized(this) {
                while (socket != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Local acceptor waiting for free socket");
                    }
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // ignored
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Local acceptor notifying ...");
                }
                socket = new AndroidBtSocket(sockt);
                notifyAll();
            }
	}
        try {
            serverSocket.close();
        } catch(Throwable e) {
            // ignore
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
        if (iAmBtAccepter) {
            if (btServerSocket == null) {
                return;
            }
            bluetoothAcceptor();
        } else {
            socketAcceptor();
        }
    }
}
