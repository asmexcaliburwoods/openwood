package org.openmim.messaging_network2.model;

import org.openmim.messaging_network2.controller.IMNetwork;

import java.util.Vector;

public interface StatusRoom extends Room {
    void setStatusRoomListener(StatusRoomListenerExternal listenerInternal);
    StatusRoomListenerExternal getStatusRoomListener();
    void modeChange(String senderSpecification, String modeFor, Vector vector);

    void notice(String nick, String text);

    void welcome(String newNick, String text);

    void registering();

    IMNetwork getIMNetwork();

    void connecting();
}