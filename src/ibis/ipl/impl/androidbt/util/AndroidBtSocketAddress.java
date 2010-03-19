package ibis.ipl.impl.androidbt.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Immutable representation of a socket address.
 */
public class AndroidBtSocketAddress {
    private final String address;
    private final UUID uuid;

    public AndroidBtSocketAddress(String fulladdress, UUID uuid) {
    	address = fulladdress;
    	this.uuid = uuid;
    }

    public AndroidBtSocketAddress(byte[] buf) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        DataInputStream is = new DataInputStream(in);
        address = is.readUTF();
        uuid = UUID.fromString(is.readUTF());
        is.close();
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(out);
        os.writeUTF(address);
        os.writeUTF(uuid.toString());
        os.close();
        return out.toByteArray();        
    }


    public boolean equals(Object other) {
        if (other.getClass() == this.getClass())
            return address.equals(((AndroidBtSocketAddress) other).address)
                    && uuid.equals(((AndroidBtSocketAddress) other).uuid);
        else
            return false;
    }
    
    public String getAddress() {
        return address;
    }
    
    public UUID getUUID() {
        return uuid;
    }
}
