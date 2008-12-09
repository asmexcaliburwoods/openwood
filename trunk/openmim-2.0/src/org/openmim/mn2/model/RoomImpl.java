package org.openmim.mn2.model;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class RoomImpl implements Room {
    private Set<RoomParticipant> roomRoles = new HashSet<RoomParticipant>();
    private List<RoomListener> roomListeners =new LinkedList<RoomListener>();

    public void addRoomListener(RoomListener roomListener){
        roomListeners.add(roomListener);
    }

    public void removeRoomListener(RoomListener roomListener) {
        roomListeners.remove(roomListener);
    }

    public Set<RoomParticipant> getRoomParticipants() {
        return roomRoles;
    }

    public void addRoomRole(RoomParticipant roomParticipant) {
        roomRoles.add(roomParticipant);
        for (RoomListener roomListener : roomListeners) {
            roomListener.roomParticipantAdded(this,roomParticipant);
        }
    }

    public void deleteRoomRole(RoomParticipant roomParticipant) {
        roomRoles.remove(roomParticipant);
        for (RoomListener roomListener : roomListeners) {
            roomListener.roomParticipantRemoved(this,roomParticipant);
        }
    }
}
