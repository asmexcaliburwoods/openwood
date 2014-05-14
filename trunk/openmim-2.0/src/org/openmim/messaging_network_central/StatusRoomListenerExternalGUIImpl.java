package org.openmim.messaging_network_central;

import org.openmim.messaging_network2.controller.IMNetwork;
import org.openmim.messaging_network2.model.StatusRoom;
import org.openmim.messaging_network2.model.StatusRoomListenerExternal;

class StatusRoomListenerExternalGUIImpl implements StatusRoomListenerExternal {
    private MessagingNetworkCentral_Listener gui;

    public void onGetCreateStatusRoom(StatusRoom room) {
        gui.onGetCreateStatusRoom(room);
    }

    public void notice(StatusRoom room, String nick, String text) {
        gui.onNotice(room,nick,text);
    }

    public void welcome(StatusRoom room, String newNick, String text) {
        gui.onWelcome(room,newNick,text);
    }

    public void registering(StatusRoom room) {
        gui.onRegistering(room);
    }

    public void connecting(StatusRoom statusRoom) {
        onGetCreateStatusRoom(statusRoom);
        gui.onConnecting(statusRoom);
    }

    public void connected(IMNetwork imNetwork) {
        gui.onConnected(imNetwork);
    }

    public void setGUI(MessagingNetworkCentral_Listener gui) {
        this.gui=gui;
    }
}
