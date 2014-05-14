package org.openmim.messaging_network2;

import org.openmim.infrastructure.Context;
import org.openmim.messaging_network2.controller.IMNetwork;
import org.openmim.messaging_network2.controller.MN2Factory;
import org.openmim.messaging_network2.model.IMListener;
import org.openmim.messaging_network2.model.StatusRoomListenerExternal;

import java.util.List;

public interface MessagingNetwork2 {
    MN2Factory getFactory();
    void setListener(MessagingNetwork2Listener listener);
    void load(Context context, IMListener imListener, StatusRoomListenerExternal statusRoomListenerExternal);
    void startReconnecting();
    public List<IMNetwork> getIMNetworks();
    boolean supportsChannels();
    String getDisplayName();
}
