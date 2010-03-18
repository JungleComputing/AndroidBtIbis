package ibis.ipl.impl.androidbt.registry.central;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.support.CountInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.bluetooth.BluetoothAdapter;

public final class Connection {

    private static final Logger logger = LoggerFactory
            .getLogger(Connection.class);
    // private final VirtualSocket socket;

    private BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
    
    private String addr;
    private final DataOutputStream out;

    private final DataInputStream in;
    private final CountInputStream counter;

    static final byte REPLY_ERROR = 2;

    static final byte REPLY_OK = 1;

    public Connection(IbisIdentifier ibis, int timeout, boolean fillTimeout,
            VirtualSocketFactory factory, int port) throws IOException {
        this(VirtualSocketAddress.fromBytes(ibis.getRegistryData(), 0),
                timeout, fillTimeout);
    }

    private Connection(VirtualSocketAddress address, int timeout,
            boolean fillTimeout) throws IOException {
        addr = address.toString();
        if (logger.isDebugEnabled()) {
            logger.debug("connecting to " + address + ", timeout = " + timeout
                    + " , filltimeout = " + fillTimeout);
        }
        socket = bt.getRemoteDevice(addr.toString()).createRfcommSocketToServiceRecord(
                address.getUUID());
        bt.cancelDiscovery();   // Should, according to docs, always be called before
                                // attempting to connect.
        socket.connect();
        ostream = socket.getOutputStream();
        istream = socket.getInputStream();
        /*
         * if(activesOutbound == null) activesOutbound = new
         * HashMap<VirtualSocketAddress, Connection>();
         * 
         * if(activesOutbound.containsKey(address)){ Connection c =
         * activesOutbound.get(address); out = c.out; in = c.in; counter =
         * c.counter; System.out.println("Reusing connection to " + address);
         * return; }
         */
        boolean ok = false;
        // System.out.println("Connecting connection to " + address);
        int i = 0;
        streamConnection = null;
        while (!ok || (streamConnection == null)) {
            // System.out.println("Registry connecting " + address.toString());
            try {
                streamConnection = (StreamConnection) Connector.open(address
                        .toString());
                ok = true;
            } catch (Exception e) {
                // System.out.print(".");
                try {
                    Thread.sleep((int) (250 + (i * 500) * Math.random()));
                } catch (Exception e2) {
                }
                ++i;
            }
        }
        // socket = factory.createClientSocket(address, timeout, fillTimeout,
        // lightConnection);
        // socket.setTcpNoDelay(true);

        out = new DataOutputStream(new BufferedOutputStream(streamConnection
                .openOutputStream()));
        counter = new CountInputStream(new BufferedInputStream(streamConnection
                .openInputStream()));
        in = new DataInputStream(counter);

        logger.debug("connection to " + address + " established");
        // System.out.println("Caching connection to " + address);
        // activesOutbound.put(address, this);

    }

    /**
     * Accept incoming connection on given serverSocket.
     */
    public Connection(VirtualServerSocket serverSocket) throws IOException {
        logger.debug("waiting for incomming connection...");
        /*
         * if(activesInbound==null) activesInbound = new
         * HashMap<VirtualServerSocket, LinkedList<Connection>>();
         * 
         * 
         * if(activesInbound.containsKey(serverSocket)){ LinkedList<Connection>
         * ll = activesInbound.get(serverSocket); Iterator<Connection> it =
         * ll.iterator(); while(it.hasNext()){ Connection c = it.next();
         * if(c.in.available()>0){ out = c.out; in = c.in; counter = c.counter;
         * return; } }
         * 
         * }
         */
        streamConnection = serverSocket.accept();

        counter = new CountInputStream(new BufferedInputStream(streamConnection
                .openInputStream()));
        in = new DataInputStream(counter);
        out = new DataOutputStream(new BufferedOutputStream(streamConnection
                .openOutputStream()));

        /*
         * if(activesInbound.containsKey(serverSocket))
         * activesInbound.get(serverSocket).add(this); else{
         * LinkedList<Connection> ll = new LinkedList<Connection>();
         * ll.add(this); activesInbound.put(serverSocket, ll); }
         */
        logger.debug("new connection accepted");
    }

    public Connection(VirtualSocketAddress serverAddress, int timeout,
            boolean b, VirtualSocketFactory virtualSocketFactory) throws IOException {
        this(serverAddress, timeout, b);
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
            throw new RemoteException(message);
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

        try {
            streamConnection.close();
        } catch (IOException e) {
            // IGNORE
        }

    }

}
