package org.openmim.messaging_network2.model;

public interface StatusRoomListenerInternal {
    void notice(StatusRoom statusRoom, String nick, String text);
    void welcome(StatusRoom statusRoom, String newNick, String text);
    void registering(StatusRoom statusRoom);
}
