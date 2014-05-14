package org.openmim.mn2.controller;

import org.openmim.mn2.model.*;
import org.openmim.infrastructure.Context;
import org.openmim.mn2.model.ServerBean;
import org.openmim.mn2.model.ConfigurationBean;

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