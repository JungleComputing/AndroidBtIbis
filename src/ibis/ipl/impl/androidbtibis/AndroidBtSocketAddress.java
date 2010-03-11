package ibis.ipl.impl.androidbtibis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Immutable representation of a socket address.
 */
public class AndroidBtSocketAddress {
    private final String address;

    public AndroidBtSocketAddress(String fulladdress) {
    	address = fulladdress;
    }

    AndroidBtSocketAddress(byte[] buf) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        ObjectInputStream is = new ObjectInputStream(in);
        try {
            address = (String) is.readObject();
        } catch(ClassNotFoundException e) {
            throw new IOException("Could not read address" + e);
        }
        is.close();
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(address);
        os.close();
        return out.toByteArray();        
    }

    public String toString() {
        return address;
    }
}
