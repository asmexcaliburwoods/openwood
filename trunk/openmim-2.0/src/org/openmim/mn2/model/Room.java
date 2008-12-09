package org.openmim.mn2.model;

import java.util.Set;

public interface Room {

    Set<RoomParticipant> getRoomParticipants();
    void addRoomRole(RoomParticipant roomParticipant);
    void deleteRoomRole(RoomParticipant roomParticipant);

    void addRoomListener(RoomListener roomListenerInternal);
    void removeRoomListener(RoomListener roomListenerInternal);

    public static interface RoomListener {
        void roomParticipantAdded(Room room, RoomParticipant roomParticipant);
        void roomParticipantRemoved(Room room, RoomParticipant roomParticipant);
    }
}
