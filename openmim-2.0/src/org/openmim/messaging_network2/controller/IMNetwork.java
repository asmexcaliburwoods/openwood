package org.openmim.messaging_network2.controller;

import org.openmim.messaging_network2.MessagingNetwork2;
import org.openmim.messaging_network2.model.AbstractContactBean;
import org.openmim.messaging_network2.model.ConcreteContactBean;
import org.openmim.messaging_network2.model.ContactListLeaf;
import org.openmim.messaging_network2.model.IMNetworkBean;
import org.openmim.messaging_network2.model.Server;
import org.openmim.messaging_network2.model.StatusRoom;

import java.util.List;
import java.io.IOException;

public interface IMNetwork {
    boolean isChannelNameValid(String channel);

    void addContact(String loginId, boolean chhannel);

    void joinRoom(ContactListLeaf leaf) throws Exception;
    void sendMessage(String recipLoginId,String plainText)throws Exception;
    void disconnectAndClose();

    public enum Type {irc,icq,msn}//todo more network types, more stuff!
    Type getType();
    String getKey();
    String getCurrentLoginId();
    List<Server> getServersStartedConnectingOrConnected();
    IMNetworkBean toBean();
    void startReconnecting();
    MessagingNetwork2 getMN2();
    StatusRoom getStatusRoom();
	void setMode(AbstractContactBean bot, String mode) throws IOException;
}
