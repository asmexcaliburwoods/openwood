package org.openmim.mn2.model;

public abstract class ServerBean {
    private IMNetworkBean imNetwork;

    public IMNetworkBean getImNetwork() {
        return imNetwork;
    }

    public void setImNetwork(IMNetworkBean imNetwork) {
        this.imNetwork = imNetwork;
    }
}
