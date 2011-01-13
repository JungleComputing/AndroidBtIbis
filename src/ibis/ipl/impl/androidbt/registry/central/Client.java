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
    
    private final String clientID;
    
    private  Client(String serverAddressString, TypedProperties properties, int port, String clientID) {
        UUID uuid = UUID.nameUUIDFromBytes(clientID.getBytes());
        factory = new VirtualSocketFactory(properties, uuid);

        serverAddress = new VirtualSocketAddress(serverAddressString);
        this.clientID = clientID;
    }

    public static synchronized Client getOrCreateClient(String clientID,
            TypedProperties properties, int port) {
	String serverAddressString = properties.getProperty(IbisProperties.SERVER_ADDRESS);
	clientID = serverAddressString + "--" + clientID;
        Client result = clients.get(clientID);

        if (result == null) {
            result = new Client(serverAddressString, properties, port, clientID);
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
    
    public void end() {
	try {
	    factory.end();
	} catch(Throwable e) {
	    // ignored
	}
	clients.remove(clientID);
    }

}
