package ibis.ipl.impl.androidbt.registry.central;

import ibis.ipl.impl.androidbt.util.AndroidBtSocketAddress;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

public class VirtualSocketAddress extends AndroidBtSocketAddress {

    public VirtualSocketAddress(String addr, UUID uuid, int port) {
        super(addr, uuid, port);
    }
 
    static public VirtualSocketAddress fromBytes(byte[] source) {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(source));
        try {
            String addr = dis.readUTF();
            UUID uuid = UUID.fromString(dis.readUTF());
            int port = dis.readInt();
            return new VirtualSocketAddress(addr, uuid, port);
        } catch(Exception e) {
            // Should not happen.
            return null;
        }
    }
}
