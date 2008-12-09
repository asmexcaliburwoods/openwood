package org.openmim.mn2.model;

public interface Query extends Room {

    RoomParticipant getMe();

    RoomParticipant getMyParty();
}
