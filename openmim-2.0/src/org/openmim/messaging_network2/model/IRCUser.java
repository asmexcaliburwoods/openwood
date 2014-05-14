package org.openmim.messaging_network2.model;

import java.beans.PropertyChangeListener;

public interface IRCUser extends User{
    final String PROPERTY_ACTIVE_NICK="activeNick";
    String getActiveNick();
    void setActiveNick(String newNick);
    void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);
    void removePropertyChangeListener(PropertyChangeListener propertyChangeListener);

    String getUserName();

    void setUserName(String userName);

    String getHostName();

    void setHostName(String hostName);
}
