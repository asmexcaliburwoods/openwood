package org.openmim.messaging_network2.controller;

import org.openmim.messaging_network2.model.*;
import org.openmim.infrastructure.Context;

import java.util.List;

public interface MN2Factory {
    NameConvertor getNameConvertor();

    /** @param key not canonical
     * @param imListener*/
    IMNetwork createIMNetwork(IMNetwork.Type type, String key, IMListener imListener,
                              StatusRoomListenerInternal statusRoomListenerInternal,
                              List<Server> listOfServersToKeepConnectionWith, Context ctx);





    List<Server> createListOfServersToKeepConnectionWith(
            List<ServerBean> listFromStorage, ConfigurationBean config);
}