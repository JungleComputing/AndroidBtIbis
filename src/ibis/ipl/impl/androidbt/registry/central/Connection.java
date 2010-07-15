package ibis.ipl.impl.androidbt.registry.central;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.androidbt.util.AndroidBtSocket;
import ibis.ipl.support.CountInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.bluetooth.BluetoothAdapter;

public class Connection {

    private static final Logger logger = LoggerFactory
            .getLogger(Connection.class);

    private BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();  
    private VirtualSocketAddress addr;
    private final DataOutputStream out;
    private final DataInputStream in;
    private final CountInputStream counter;
    
    private final AndroidBtSocket socket;
    
    public Connection(IbisIdentifier ibis, int timeout, boolean fillTimeout,
            VirtualSocketFactory factory, int port) throws IOException {
        this(VirtualSocketAddress.fromBytes(ibis.getRegistryData()),
                timeout, fillTimeout);
    }
    
    public Connection(VirtualSocketAddress serverAddress, int timeout,
            boolean b, VirtualSocketFactory virtualSocketFactory) throws IOException {
        this(serverAddress, timeout, b);
    }

    private Connection(VirtualSocketAddress address, int timeout,
            boolean fillTimeout) throws IOException {
        
        addr = address;
        if (logger.isDebugEnabled()) {
            logger.debug("connecting to " + address + ", timeout = " + timeout
                    + " , filltimeout = " + fillTimeout);
        }
        
        if (address.getAddress().equals(bt.getAddress())) {
            VirtualServerSocket srvr = VirtualServerSocket.findServer(address);
            // Note: this assumes that the server socket is running inside the same JVM.
            // So, if the Ibis registry server is running on a phone that runs a client
            // as well, it must run in a separate thread, on the same JVM.
            // Also, it is not possible to have two Ibis instances running in separate
            // JVM instances connect to each other. TODO: fix!
            // Maybe use socket for that? loopback?
            if (srvr == null) {
                throw new IOException("Local server socket not found");
            }
            // Set up piped streams:
            PipedInputStream in1 = new PipedInputStream();
            PipedInputStream in2 = new PipedInputStream();
            PipedOutputStream out1 = new PipedOutputStream(in1);
            PipedOutputStream out2 = new PipedOutputStream(in2);
            // create the client socket
            socket = new AndroidBtSocket(in1, out2);
            // Find the server socket corresponding to this connection ...
            srvr.addLocalConnection(new AndroidBtSocket(in2, out1));
        } else {
            socket = new AndroidBtSocket(bt, address); 
        }

        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        counter = new CountInputStream(new BufferedInputStream(socket.getInputStream()));
        in = new DataInputStream(counter);

        logger.debug("connection to " + address + " established");
    }

    public Connection(VirtualServerSocket serverSocket) throws IOException {        
        socket = serverSocket.getServerSocket().accept();
        counter = new CountInputStream(new BufferedInputStream(socket.getInputStream()));
        in = new DataInputStream(counter);
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

    }

    static final byte REPLY_ERROR = 2;

    static final byte REPLY_OK = 1;

    AndroidBtSocket getSocket() {
        return socket;
    }

    public DataOutputStream out() {
        return out;
    }

    public DataInputStream in() {
        return in;
    }

    public int written() {
        return out.size();
    }

    public int read() {
        return counter.getCount();
    }

    public void getAndCheckReply() throws IOException {
        // flush output, just in case...
        out.flush();

        // get reply
        byte reply = in.readByte();
        if (reply == Connection.REPLY_ERROR) {
            String message = in.readUTF();
            close();
            throw new IOException("Remote side: " + message);
        } else if (reply != Connection.REPLY_OK) {
            close();
            throw new IOException("Unknown reply (" + reply + ")");
        }
    }

    public void sendOKReply() throws IOException {
        out.writeByte(Connection.REPLY_OK);
        out.flush();
    }

    public void closeWithError(String message) {
        if (message == null) {
            message = "";
        }
        try {
            out.writeByte(Connection.REPLY_ERROR);
            out.writeUTF(message);

            close();
        } catch (IOException e) {
            // IGNORE
        }
    }

    public void close() {

        // Thread.dumpStack();
        try {
            out.flush();
        } catch (IOException e) {
            // IGNORE
        }

        try {
            out.close();
        } catch (IOException e) {
            // IGNORE
        }

        try {
            in.close();
        } catch (IOException e) {
            // IGNORE
        }

        socket.close();
    }

}
