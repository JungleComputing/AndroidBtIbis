package ibis.ipl.impl.androidbt.registry.central;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ibis.ipl.IbisProperties;
import ibis.util.TypedProperties;

public class Client {

    private static final Map<String, Client> clients = new HashMap<String, Client>();
    
    private final VirtualSocketFactory factory;
    
    private final VirtualSocketAddress serverAddress;
    
    private  Client(TypedProperties properties, int port, String clientID) {
        UUID uuid = UUID.nameUUIDFromBytes(clientID.getBytes());
        factory = new VirtualSocketFactory(properties, uuid);

        String serverAddressString = properties.getProperty(IbisProperties.SERVER_ADDRESS);
        byte[] bytes = serverAddressString.getBytes();
        serverAddress = VirtualSocketAddress.fromBytes(bytes);
    }

    public static synchronized Client getOrCreateClient(String clientID,
            TypedProperties properties, int port) {
        Client result = clients.get(clientID);

        if (result == null) {
            result = new Client(properties, port, clientID);
            clients.put(clientID, result);
        }
        return result;
    }

    public VirtualSocketFactory getFactory() {
        return factory;
    }

    public VirtualSocketAddress getServiceAddress(int virtualPort) {
        return serverAddress;
    }

}
