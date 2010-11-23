/* $Id: BtIbis.java 11529 2009-11-18 15:53:11Z ceriel $ */

package ibis.ipl.impl.androidbt;

import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.CapabilitySet;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.Credentials;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisStarter;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortMismatchException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.ipl.impl.androidbt.util.AdaptorFinder;
import ibis.ipl.impl.androidbt.util.AndroidBtServerSocket;
import ibis.ipl.impl.androidbt.util.AndroidBtSocket;
import ibis.ipl.impl.androidbt.util.AndroidBtSocketAddress;
// import ibis.ipl.impl.androidbt.util.UIHandler;
import ibis.util.ThreadPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.bluetooth.BluetoothAdapter;

public final class AndroidBtIbis extends ibis.ipl.impl.Ibis implements Runnable,
        AndroidBtProtocol {

    static final Logger logger = LoggerFactory
            .getLogger("ibis.ipl.impl.bluetooth.BtIbis");

    private AndroidBtServerSocket systemServer;

    private AndroidBtSocketAddress myAddress;
    
    private static final BluetoothAdapter bt = AdaptorFinder.getBluetoothAdaptor();

    private boolean quiting = false;

    private HashMap<ibis.ipl.IbisIdentifier, AndroidBtSocketAddress> addresses = new HashMap<ibis.ipl.IbisIdentifier, AndroidBtSocketAddress>();
    
    // private final UIHandler uiHandler = new UIHandler(bt);
    
    public AndroidBtIbis(RegistryEventHandler registryEventHandler,
            IbisCapabilities capabilities, Credentials credentials,
            byte[] applicationTag, PortType[] types, Properties userProperties, IbisStarter starter) throws IbisCreationFailedException {
        super(registryEventHandler, capabilities, credentials, applicationTag, types,
                userProperties, starter);

        this.properties.checkProperties("ibis.ipl.impl.androidbt.",
                new String[] { }, null, true);
        
        if (bt != null) {

            if (! bt.isEnabled()) {
                // uiHandler.enableBT();
                // if (! uiHandler.waitForBT()) {
                    throw new IbisCreationFailedException("Bluetooth device was not enabled");
                // }
            }

            if (bt.getScanMode() == BluetoothAdapter.SCAN_MODE_NONE) {
                throw new IbisCreationFailedException("Bluetooth device should at least be connectable");
            }
        }
        // Create a new accept thread
        ThreadPool.createNew(this, "BtIbis Accept Thread");
    }

    protected byte[] getData() throws IOException {
	
	if (logger.isDebugEnabled()) {
	    logger.debug("getData: bt = " + (bt == null ? "null" : bt.getAddress()));
	}

        systemServer = new AndroidBtServerSocket(bt);
        myAddress = systemServer.getLocalSocketAddress();

        if (logger.isInfoEnabled()) {
            logger.info("--> BtIbis: address = " + myAddress);
        }

        return myAddress.toBytes();
    }

    /*
     * // NOTE: this is wrong ? Even though the ibis has left, the
     * IbisIdentifier may still be floating around in the system... We should
     * just have some timeout on the cache entries instead...
     * 
     * public void left(ibis.ipl.IbisIdentifier id) { super.left(id);
     * synchronized(addresses) { addresses.remove(id); } }
     * 
     * public void died(ibis.ipl.IbisIdentifier id) { super.died(id);
     * synchronized(addresses) { addresses.remove(id); } }
     */

    AndroidBtSocket connect(AndroidBtSendPort sp, ibis.ipl.impl.ReceivePortIdentifier rip,
            int timeout, boolean fillTimeout) throws IOException {

        IbisIdentifier id = (IbisIdentifier) rip.ibisIdentifier();
        String name = rip.name();
        AndroidBtSocketAddress idAddr;

        synchronized (addresses) {
            idAddr = addresses.get(id);
            if (idAddr == null) {
                idAddr = new AndroidBtSocketAddress(id.getImplementationData());
                addresses.put(id, idAddr);
            }
        }

        long startTime = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            logger.debug("--> Creating socket for connection to " + name
                    + " at " + idAddr);
        }

        PortType sendPortType = sp.getPortType();

        do {
            DataOutputStream out = null;
            AndroidBtSocket s = null;
            int result = -1;

            try {
                s = new AndroidBtSocket(bt, idAddr);                
                out = new DataOutputStream(
                        new BufferedArrayOutputStream(s.getOutputStream()));

                out.writeUTF(name);
                sp.getIdent().writeTo(out);
                sendPortType.writeTo(out);
                out.flush();

                result = s.getInputStream().read();

                switch (result) {
                case ReceivePort.ACCEPTED:
                    return s;
                case ReceivePort.ALREADY_CONNECTED:
                    throw new AlreadyConnectedException("Already connected",
                            rip);
                case ReceivePort.TYPE_MISMATCH:
                    // Read receiveport type from input, to produce a
                    // better error message.
                    DataInputStream in = new DataInputStream(s.getInputStream());
                    PortType rtp = new PortType(in);
                    CapabilitySet s1 = rtp.unmatchedCapabilities(sendPortType);
                    CapabilitySet s2 = sendPortType.unmatchedCapabilities(rtp);
                    String message = "";
                    if (s1.size() != 0) {
                        message = message
                                + "\nUnmatched receiveport capabilities: "
                                + s1.toString() + ".";
                    }
                    if (s2.size() != 0) {
                        message = message
                                + "\nUnmatched sendport capabilities: "
                                + s2.toString() + ".";
                    }
                    throw new PortMismatchException(
                            "Cannot connect ports of different port types."
                                    + message, rip);
                case ReceivePort.DENIED:
                    throw new ConnectionRefusedException(
                            "Receiver denied connection", rip);
                case ReceivePort.NO_MANY_TO_X:
                    throw new ConnectionRefusedException(
                            "Receiver already has a connection and neither ManyToOne not ManyToMany "
                                    + "is set", rip);
                case ReceivePort.NOT_PRESENT:
                case ReceivePort.DISABLED:
                    // and try again if we did not reach the timeout...
                    if (timeout > 0
                            && System.currentTimeMillis() > startTime + timeout) {
                        throw new ConnectionTimedOutException(
                                "Could not connect", rip);
                    }
                    break;
                case -1:
                    throw new IOException("Encountered EOF in BtIbis.connect");
                default:
                    throw new IOException("Illegal opcode in BtIbis.connect");
                }
            } catch (SocketTimeoutException e) {
                throw new ConnectionTimedOutException("Could not connect", rip);
            } finally {
                if (result != ReceivePort.ACCEPTED) {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (Throwable e) {
                        // ignored
                    }
                    try {
                        s.close();
                    } catch (Throwable e) {
                        // ignored
                    }
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        } while (true);
    }

    protected void quit() {
        try {
            quiting = true;
            cleanup();          // will make accept throw an exception.
        } catch (Throwable e) {
            // Ignore
        }
    }

    private void handleConnectionRequest(AndroidBtSocket s) throws IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("--> BtIbis got connection request from " + s);
        }

        BufferedArrayInputStream bais = 
            new BufferedArrayInputStream(s.getInputStream());

        DataInputStream in = new DataInputStream(bais);
        OutputStream out = s.getOutputStream();

        String name = in.readUTF();
        SendPortIdentifier send = new SendPortIdentifier(in);
        PortType sp = new PortType(in);

        // First, lookup receiveport.
        AndroidBtReceivePort rp = (AndroidBtReceivePort) findReceivePort(name);

        int result;
        if (rp == null) {
            result = ReceivePort.NOT_PRESENT;
        } else {
            result = rp.connectionAllowed(send, sp);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("--> S RP = " + name + ": "
                    + ReceivePort.getString(result));
        }

        out.write(result);
        if (result == ReceivePort.TYPE_MISMATCH) {
            DataOutputStream dout = new DataOutputStream(out);
            rp.getPortType().writeTo(dout);
            dout.flush();
        }
        out.flush();
        if (result == ReceivePort.ACCEPTED) {
            // add the connection to the receiveport.
            rp.connect(send, s, bais);
            if (logger.isDebugEnabled()) {
                logger.debug("--> S connect done ");
            }
        } else {
            out.close();
            in.close();
            s.close();
        }
    }

    public void run() {
        // This thread handles incoming connection request from the
        // connect(BtSendPort) call.

        boolean stop = false;

        while (!stop) {
            AndroidBtSocket s = null;

            if (logger.isDebugEnabled()) {
                logger.debug("--> BtIbis doing new accept()");
            }

            try {
                s = systemServer.accept();
            } catch (Throwable e) {
                if (quiting) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("--> it is a quit: RETURN");
                    }
                    return;
                }
                /* if the accept itself fails, we have a fatal problem. */
                logger.error("BtIbis:run: got fatal exception in accept! ", e);
                cleanup();
                throw new Error("Fatal: BtIbis could not do an accept", e);
                // This error is thrown in the BtIbis thread, not in a user
                // thread. It kills the thread.
            }

            if (logger.isDebugEnabled()) {
                logger.debug("--> BtIbis through new accept()");
            }

            try {
                // This thread will now live on as a connection handler. Start
                // a new accept thread here, and make sure that this thread does
                // not do an accept again, if it ever returns to this loop.
                stop = true;

                try {
                    Thread.currentThread().setName("Connection Handler");
                } catch (Exception e) {
                    // ignore
                }

                ThreadPool.createNew(this, "BtIbis Accept Thread");

                // Try to get the accept thread into an accept call. (Ceriel)
                // Thread.currentThread().yield();
                //
                // Yield is evil. It breaks the whole concept of starting a
                // replacement thread and handling the incoming request
                // ourselves. -- Jason

                handleConnectionRequest(s);
            } catch (Throwable e) {
                try {
                    s.close();
                } catch (Throwable e2) {
                    // ignored
                }
                logger.error("EEK: BtIbis:run: got exception "
                        + "(closing this socket only: ", e);
            }
        }
    }

    private void cleanup() {
        try {
            systemServer.close();
        } catch (Throwable e) {
            // Ignore
        }
    }

    protected SendPort doCreateSendPort(PortType tp, String nm,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        return new AndroidBtSendPort(this, tp, nm, cU, props);
    }

    protected ReceivePort doCreateReceivePort(PortType tp, String nm,
            MessageUpcall u, ReceivePortConnectUpcall cU, Properties props)
            throws IOException {
        return new AndroidBtReceivePort(this, tp, nm, u, cU, props);
    }

}
