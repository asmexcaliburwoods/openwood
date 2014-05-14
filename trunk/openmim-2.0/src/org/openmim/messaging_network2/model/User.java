package org.openmim.messaging_network2.model;

import java.beans.PropertyChangeListener;

public interface User
{
    final String PROPERTY_DISPLAY_NAME="displayName";
    String getDisplayName();
    boolean canRename();
    void setDisplayName(String displayName);
    void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);
    void removePropertyChangeListener(PropertyChangeListener propertyChangeListener);
}
