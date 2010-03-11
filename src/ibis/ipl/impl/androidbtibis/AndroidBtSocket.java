package ibis.ipl.impl.androidbtibis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;

class AndroidBtSocket {
    private BluetoothSocket socket;
    OutputStream ostream;
    InputStream istream;
    
    /**
     * This version of the IbisSocket constructor gets called from the Ibis connect
     * method, and creates the connection.
     * @param bt The bluetooth adapter.
     * @param addr address of ibis to connect to.
     * @throws IOException
     */
    AndroidBtSocket(BluetoothAdapter bt, AndroidBtSocketAddress addr) throws IOException {
        socket = bt.getRemoteDevice(addr.toString()).createRfcommSocketToServiceRecord(
                AndroidBtServerSocket.MYSERVICEUUID);
        bt.cancelDiscovery();   // Should, according to docs, always be called before
                                // attempting to connect.
        socket.connect();
        ostream = socket.getOutputStream();
        istream = socket.getInputStream();
    }
    
    /**
     * This version of the IbisSocket constructor gets called from an accept of
     * the server socket, so it represents an established connection.
     * 
     * @param sckt the bluetooth socket resulting from the accept.
     */
    AndroidBtSocket(BluetoothSocket sckt) {
        socket = sckt;
        try {
            istream = socket.getInputStream(); 
            ostream = socket.getOutputStream();
        } catch (IOException e) {
            System.err.println("Unable to open io streams");
        }
    } 
    
    /**
     * This version of the IbisSocket constructor gets called in case a connection is
     * set up to the same host. There is no loopback support in bluetooth, so this is
     * a special case, that is dealt with through Piped streams.
     * @param in        the input stream of the socket
     * @param out       the output stream of the socket
     */
    AndroidBtSocket(InputStream in, OutputStream out) {
        istream = in;
        ostream = out;
        socket = null;
    }

    OutputStream getOutputStream() {
        return ostream;
    }

    InputStream getInputStream() {
        return istream;
    }

    void close() {
        try {
            ostream.flush();
        } catch(Throwable e) {
            // ignored   
        }
        
        try {
            ostream.close();
        } catch(Throwable e) {
            // ignored   
        }
        
        try {
            istream.close();
        } catch(Throwable e) {
            // ignored   
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch(Throwable e) {
            // ignored
        } finally {
            socket = null;
            ostream = null;
            istream = null;
        }
    }
}
