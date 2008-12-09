package org.openmim.infrastructure;

import org.openmim.metanetwork.MetaNetwork;
import org.openmim.mn2.model.StatusRoomListenerExternal;
import org.openmim.kernel.KernelListener;
import org.openmim.mn2.model.ConfigurationBean;

public class Context {
    private MetaNetwork metaNetwork;
    private ConfigurationBean configurationBean;
    private StatusRoomListenerExternal statusRoomListenerExternal;
    private KernelListener kernelListener;

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

    public void setKernelListener(KernelListener kernelListener) {
        this.kernelListener = kernelListener;
    }

    public KernelListener getKernelListener() {
        return kernelListener;
    }
}

