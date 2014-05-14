package org.openmim.messaging_network2.model;

public class RoleToDisplayBean {
    /** not canonical */
    private String loginId;
    private IMNetworkBean imNetwork;

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public IMNetworkBean getImNetwork() {
        return imNetwork;
    }

    public void setImNetwork(IMNetworkBean imNetwork) {
        this.imNetwork = imNetwork;
    }

}
