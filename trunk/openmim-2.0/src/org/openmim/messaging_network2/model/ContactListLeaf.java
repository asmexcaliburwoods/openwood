package org.openmim.messaging_network2.model;

public interface ContactListLeaf extends ContactListItem{
    String getLoginId();
    void setLoginId(String loginId);
    boolean isChannel();

    void setNetworkKey(String key);
    String getNetworkKey();
}
