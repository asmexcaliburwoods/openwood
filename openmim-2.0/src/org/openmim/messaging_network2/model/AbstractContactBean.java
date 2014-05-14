package org.openmim.messaging_network2.model;

public abstract class AbstractContactBean implements ContactListLeaf {
    private String loginId;
    private String displayName;
    private String networkKey;

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getNetworkKey() {
        return networkKey;
    }

    public void setNetworkKey(String networkKey) {
        this.networkKey = networkKey;
    }
}
