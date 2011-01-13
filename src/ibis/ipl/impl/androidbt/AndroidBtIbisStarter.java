/* $Id: AndroidBtIbisStarter.java 11529 2009-11-18 15:53:11Z ceriel $ */

package ibis.ipl.impl.androidbt;

import ibis.ipl.CapabilitySet;
import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisProperties;
import ibis.ipl.PortType;
import ibis.ipl.RegistryEventHandler;

import java.util.ArrayList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AndroidBtIbisStarter extends ibis.ipl.IbisStarter {

    static final Logger logger = LoggerFactory
            .getLogger("ibis.ipl.impl.androidbtibis.AndroidBtIbisStarter");

    static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.CLOSED_WORLD,
            IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED,
            IbisCapabilities.MEMBERSHIP_UNRELIABLE, IbisCapabilities.SIGNALS,
            IbisCapabilities.ELECTIONS_UNRELIABLE,
            IbisCapabilities.ELECTIONS_STRICT, IbisCapabilities.MALLEABLE,
            IbisCapabilities.TERMINATION);

    static final PortType portCapabilities = new PortType(
            PortType.SERIALIZATION_OBJECT_SUN,
            PortType.SERIALIZATION_OBJECT_IBIS, PortType.SERIALIZATION_OBJECT,
            PortType.SERIALIZATION_DATA, PortType.SERIALIZATION_BYTE,
            PortType.COMMUNICATION_FIFO, PortType.COMMUNICATION_NUMBERED,
            PortType.COMMUNICATION_RELIABLE, PortType.CONNECTION_DOWNCALLS,
            PortType.CONNECTION_UPCALLS, PortType.CONNECTION_TIMEOUT,
            PortType.CONNECTION_MANY_TO_MANY, PortType.CONNECTION_MANY_TO_ONE,
            PortType.CONNECTION_ONE_TO_MANY, PortType.CONNECTION_ONE_TO_ONE,
            PortType.RECEIVE_POLL, PortType.RECEIVE_AUTO_UPCALLS,
            PortType.RECEIVE_EXPLICIT, PortType.RECEIVE_POLL_UPCALLS,
            PortType.RECEIVE_TIMEOUT);

    public AndroidBtIbisStarter(String nickName, String iplVersion,
            String implementationVersion) {
        super(nickName, iplVersion, implementationVersion);
    }

    @Override
    public boolean matches(IbisCapabilities capabilities, PortType[] types) {
        if (!capabilities.matchCapabilities(ibisCapabilities)) {
            return false;
        }
        for (PortType portType : types) {
            if (!portType.matchCapabilities(portCapabilities)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CapabilitySet unmatchedIbisCapabilities(
            IbisCapabilities capabilities, PortType[] types) {
        return capabilities.unmatchedCapabilities(ibisCapabilities);
    }

    @Override
    public PortType[] unmatchedPortTypes(IbisCapabilities capabilities,
            PortType[] types) {
        ArrayList<PortType> result = new ArrayList<PortType>();

        for (PortType portType : types) {
            if (!portType.matchCapabilities(portCapabilities)) {
                result.add(portType);
            }
        }
        return result.toArray(new PortType[0]);
    }

    @Override
    public Ibis startIbis(IbisFactory factory,
            RegistryEventHandler registryEventHandler,
            Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag,
            PortType[] portTypes, String specifiedSubImplementation)
            throws IbisCreationFailedException {
        Properties p = new Properties(userProperties);
        p.setProperty(IbisProperties.REGISTRY_IMPLEMENTATION, "ibis.ipl.impl.androidbt.registry.central.client.Registry");
        
        // The SERVER_ADDRESS system property may have been set too late in case a server is started
        // as well. Prevent the use of an earlier one.
        String registry = p.getProperty(IbisProperties.SERVER_ADDRESS);
        String registry1 = System.getProperty(IbisProperties.SERVER_ADDRESS);
        if (registry == null || ! registry.equals(registry1)) {
            p.setProperty(IbisProperties.SERVER_ADDRESS, registry1);
        }
        return new AndroidBtIbis(registryEventHandler, capabilities, credentials,
                applicationTag, portTypes, p, this);
    }
}
