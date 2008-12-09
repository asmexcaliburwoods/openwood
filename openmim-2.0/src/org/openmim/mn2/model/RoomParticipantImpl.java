package org.openmim.mn2.model;

import squirrel_util.Lang;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

public class RoomParticipantImpl implements RoomParticipant {
    private Room room;
    private User user;
    PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public User getUser() {
        return user;
    }

    public Room getRoom() {
        return room;
    }

    public void setUser(User user) {
        Lang.ASSERT_NOT_NULL(user, "user");
        User oldValue = this.user;
        this.user = user;
        propertyChangeSupport.firePropertyChange(PROPERTY_USER, oldValue, user);
    }

    public void setRoom(Room room) {
        Lang.ASSERT_NOT_NULL(room, "room");
        Room oldValue = this.room;
        this.room = room;
        propertyChangeSupport.firePropertyChange(PROPERTY_ROOM, oldValue, room);
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
    }

    public String toString() {
        return getUser() + "@" + getRoom();
    }
}
