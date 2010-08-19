package ibis.ipl.impl.androidbt.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;

public class AndroidBtSocket {
    private BluetoothSocket btSocket = null;
    private Socket socket = null;
    OutputStream ostream;
    InputStream istream;
    
    /**
     * This version of the IbisSocket constructor gets called from the Ibis connect
     * method, and creates the connection.
     * @param bt The bluetooth adapter.
     * @param addr address of ibis to connect to.
     * @throws IOException
     */
    public AndroidBtSocket(BluetoothAdapter bt, AndroidBtSocketAddress addr) throws IOException {
        if (bt.getAddress().equals(addr.getBtAddress())) {
            // Same host, so we have to set up a local connection. We use the
            // loopback device for that.
            socket = new Socket(InetAddress.getLocalHost(), addr.getPort());
            ostream = socket.getOutputStream();
            istream = socket.getInputStream();
        } else {
            btSocket = bt.getRemoteDevice(addr.getBtAddress()).createRfcommSocketToServiceRecord(
                    addr.getUUID());
            bt.cancelDiscovery();   // Should, according to docs, always be called before
            // attempting to connect.
            btSocket.connect();
            ostream = btSocket.getOutputStream();
            istream = btSocket.getInputStream();
        }
    }
    
    /**
     * This version of the IbisSocket constructor gets called from an accept of
     * the server socket, so it represents an established connection.
     * 
     * @param sckt the bluetooth socket resulting from the accept.
     */
    public AndroidBtSocket(BluetoothSocket sckt) {
        btSocket = sckt;
        try {
            istream = btSocket.getInputStream(); 
            ostream = btSocket.getOutputStream();
        } catch (IOException e) {
            System.err.println("Unable to open io streams");
        }
    } 
    
    /**
     * This version of the IbisSocket constructor gets called from an accept of
     * the server socket, so it represents an established connection.
     * 
     * @param sckt the socket resulting from the accept.
     */
    public AndroidBtSocket(Socket sckt) {
        socket = sckt;
        try {
            istream = socket.getInputStream(); 
            ostream = socket.getOutputStream();
        } catch (IOException e) {
            System.err.println("Unable to open io streams");
        }
    } 
    
    public OutputStream getOutputStream() {
        return ostream;
    }

    public InputStream getInputStream() {
        return istream;
    }

    public void close() {
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
            if (btSocket != null) {
                btSocket.close();
            }
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
            btSocket = null;
            socket = null;
            ostream = null;
            istream = null;
        }
    }
}
