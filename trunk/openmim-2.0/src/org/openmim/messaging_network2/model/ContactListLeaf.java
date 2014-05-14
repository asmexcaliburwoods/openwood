package org.openmim.mn2.model;

public interface ContactListLeaf extends ContactListItem{
    String getLoginId();
    void setLoginId(String loginId);
    boolean isChannel();

    void setNetworkKey(String key);
    String getNetworkKey();
}
