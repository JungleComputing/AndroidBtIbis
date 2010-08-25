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
    private final String btAddress;
    private final UUID uuid;
    private final int port;

    public AndroidBtSocketAddress(String fulladdress, UUID uuid, int port) {
    	btAddress = fulladdress;
    	this.uuid = uuid;
    	this.port = port;
    }

    public AndroidBtSocketAddress(byte[] buf) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        DataInputStream is = new DataInputStream(in);
        btAddress = is.readUTF();
        uuid = UUID.fromString(is.readUTF());
        port = is.readInt();
        is.close();
    }
    
    public AndroidBtSocketAddress(String s) {
        String[] splits = s.split("/");
        if (splits.length != 3) {
            throw new RuntimeException("Wrong string in AndroidBtSocketAddress constructor: " + s);
        }
        btAddress = splits[0];
        uuid = UUID.fromString(splits[1]);
        port = Integer.parseInt(splits[2]);
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(out);
        os.writeUTF(btAddress);
        os.writeUTF(uuid.toString());
        os.writeInt(port);
        os.close();
        return out.toByteArray();        
    }

    public boolean equals(Object other) {
        if (other.getClass() == this.getClass())
            return btAddress.equals(((AndroidBtSocketAddress) other).btAddress)
                    && uuid.equals(((AndroidBtSocketAddress) other).uuid)
                    && port == ((AndroidBtSocketAddress) other).port;
        else
            return false;
    }
    
    public String getBtAddress() {
        return btAddress;
    }
    
    public UUID getUUID() {
        return uuid;
    }
    
    public int getPort() {
        return port;
    }
    
    public String toString() {
        return btAddress + "/" + uuid.toString() + "/" + port;
    }
}
