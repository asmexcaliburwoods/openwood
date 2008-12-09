package org.openmim.kernel;

import org.openmim.mn2.model.StatusRoom;
import org.openmim.mn2.model.StatusRoomListenerExternal;
import org.openmim.mn2.controller.IMNetwork;

class StatusRoomListenerExternalGUIImpl implements StatusRoomListenerExternal {
    private KernelListener gui;

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

    public void setGUI(KernelListener gui) {
        this.gui=gui;
    }
}
