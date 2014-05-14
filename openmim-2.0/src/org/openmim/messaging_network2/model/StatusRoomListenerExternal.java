package org.openmim.mn2.model;

import org.openmim.mn2.controller.IMNetwork;

public interface StatusRoomListenerExternal {
    void onGetCreateStatusRoom(StatusRoom room);

    void notice(StatusRoom room, String nick, String text);

    void welcome(StatusRoom room, String newNick, String text);

    void registering(StatusRoom room);

    void connecting(StatusRoom statusRoom);

    void connected(IMNetwork imNetwork);
}
