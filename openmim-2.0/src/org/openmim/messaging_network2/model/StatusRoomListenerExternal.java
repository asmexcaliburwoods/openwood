package org.openmim.messaging_network2.model;

import org.openmim.messaging_network2.controller.IMNetwork;

public interface StatusRoomListenerExternal {
    void onGetCreateStatusRoom(StatusRoom room);

    void notice(StatusRoom room, String nick, String text);

    void welcome(StatusRoom room, String newNick, String text);

    void registering(StatusRoom room);

    void connecting(StatusRoom statusRoom);

    void connected(IMNetwork imNetwork);
}
