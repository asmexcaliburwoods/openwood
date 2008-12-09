package org.openmim.kernel;

import org.openmim.mn2.controller.IMNetwork;
import org.openmim.mn2.model.Room;
import org.openmim.mn2.model.StatusRoom;

public interface KernelListener {
    void onGetCreateStatusRoom(StatusRoom room);

    void onNotice(StatusRoom room, String nick, String text);

    void onWelcome(StatusRoom room, String newNick, String text);

    void onRegistering(StatusRoom room);

    void onConnecting(StatusRoom statusRoom);

    void onActionMessage(IMNetwork net, Room room, String nickName, String text);

    void onMessage(IMNetwork net, Room room, String nickFrom, String text);

    void onAwayMessage(IMNetwork mn, String nick, String msg);

    void onMeJoined(IMNetwork net, String s);

    void onNickChange(IMNetwork net, String s, String s1);

    void onQuit(IMNetwork net, String s, String s1);

    void onUserQuit(IMNetwork net, boolean me, String nick, String note);

    void onUserLoggedOn(IMNetwork net, boolean me, String nick, String note);

    void onNoSuchNickChannel(IMNetwork net, String nickOrChannel, String comment);

    void onConnected(IMNetwork imNetwork);
}
