package ibis.ipl.impl.androidbt.registry.central;

import ibis.ipl.impl.androidbt.util.AndroidBtSocketAddress;

import java.io.IOException;
import java.util.UUID;

public class VirtualSocketAddress extends AndroidBtSocketAddress {

    public VirtualSocketAddress(String addr, UUID uuid, int port) {
        super(addr, uuid, port);
    }
    
    public VirtualSocketAddress(byte[] s) throws IOException {
        super(s);
    }
    
    static public VirtualSocketAddress fromBytes(byte[] source) {
        try {
            return new VirtualSocketAddress(source);
        } catch(Exception e) {
            // Should not happen.
            return null;
        }
    }
        
    public VirtualSocketAddress(String s) {
        super(s);
    }
}
