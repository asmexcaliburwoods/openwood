package org.openmim.messaging_network2.model;

public interface Query extends Room {

    RoomParticipant getMe();

    RoomParticipant getMyParty();
}
