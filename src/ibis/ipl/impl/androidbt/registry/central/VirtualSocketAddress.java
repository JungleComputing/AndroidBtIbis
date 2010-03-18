package ibis.ipl.impl.androidbt.registry.central;

import java.util.UUID;

public class VirtualSocketAddress {

    String address;
    UUID uuid;

    public VirtualSocketAddress(String addr, UUID uuid) {
        address = addr;
        this.uuid = uuid;
    }

    public String toString() {
        return address;
    }

    public byte[] toBytes() {
        return address.getBytes();
    }

    public boolean equals(Object other) {
        if (other.getClass() == this.getClass())
            return address.equals(((VirtualSocketAddress) other).toString());
        else
            return false;
    }

    static public VirtualSocketAddress fromBytes(byte[] source, int offset) {
        // TODO: not sure...
        return new VirtualSocketAddress(new String(source));
    }

    public UUID getUUID() {
        return uuid;
    }
}
