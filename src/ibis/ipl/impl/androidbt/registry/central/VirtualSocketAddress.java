package ibis.ipl.impl.androidbt.registry.central;

import ibis.ipl.impl.androidbt.util.AndroidBtSocketAddress;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

public class VirtualSocketAddress extends AndroidBtSocketAddress {

    public VirtualSocketAddress(String addr, UUID uuid) {
        super(addr, uuid);
    }
 
    static public VirtualSocketAddress fromBytes(byte[] source) {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(source));
        try {
            String addr = dis.readUTF();
            UUID uuid = UUID.fromString(dis.readUTF());
            return new VirtualSocketAddress(addr, uuid);
        } catch(Exception e) {
            // Should not happen.
            return null;
        }
    }
}
