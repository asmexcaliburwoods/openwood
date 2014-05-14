package org.openmim.messaging_network2.model;

import org.openmim.messaging_network2.controller.IMNetwork;

public abstract class IMNetworkBean {
    /** not canonical */
    private String key;
    private IMNetwork.Type type;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public IMNetwork.Type getType() {
        return type;
    }

    public void setType(IMNetwork.Type type) {
        this.type = type;
    }
}
