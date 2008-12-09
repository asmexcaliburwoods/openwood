package org.openmim.mn2.model;

import java.beans.PropertyChangeListener;

public interface RoomParticipant
{
    final String PROPERTY_USER="user";
    final String PROPERTY_ROOM="room";
    User getUser();
    void setUser(User user);
    Room getRoom();
    void setRoom(Room room);
    void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);
    void removePropertyChangeListener(PropertyChangeListener propertyChangeListener);
}
