package org.openmim.messaging_network2.model;

import java.util.Set;

import org.openmim.messaging_network2.controller.IMNetwork;

public interface Room {
	IMNetwork getNetwork();
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
