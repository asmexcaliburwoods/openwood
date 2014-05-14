package org.openmim.infrastructure;

import org.openmim.messaging_network2.model.ConfigurationBean;
import org.openmim.messaging_network2.model.StatusRoomListenerExternal;
import org.openmim.messaging_network3.MetaNetwork;
import org.openmim.messaging_network_central.MessagingNetworkCentral_Listener;

public class Context {
    private MetaNetwork metaNetwork;
    private ConfigurationBean configurationBean;
    private StatusRoomListenerExternal statusRoomListenerExternal;
    private MessagingNetworkCentral_Listener messagingNetworkCentral_Listener;

    public Context(MetaNetwork metaNetwork, ConfigurationBean configurationBean, StatusRoomListenerExternal statusRoomListenerExternal) {
        this.metaNetwork = metaNetwork;
        this.configurationBean = configurationBean;
        this.statusRoomListenerExternal=statusRoomListenerExternal;
    }

    public MetaNetwork getMetaNetwork() {
        return metaNetwork;
    }

    public ConfigurationBean getConfigurationBean() {
        return configurationBean;
    }

    public StatusRoomListenerExternal getStatusRoomListenerExternal() {
        return statusRoomListenerExternal;
    }

    public void setKernelListener(MessagingNetworkCentral_Listener messagingNetworkCentral_Listener) {
        this.messagingNetworkCentral_Listener = messagingNetworkCentral_Listener;
    }

    public MessagingNetworkCentral_Listener getKernelListener() {
        return messagingNetworkCentral_Listener;
    }
}

