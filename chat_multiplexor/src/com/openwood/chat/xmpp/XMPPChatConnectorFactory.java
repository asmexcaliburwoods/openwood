package com.openwood.chat.xmpp;

import java.util.Properties;

import com.openwood.chat.ChatConnector;
import com.openwood.chat.ChatConnectorFactory;

public class XMPPChatConnectorFactory implements ChatConnectorFactory {

	@Override
	public ChatConnector createInstance(String instanceId, Properties properties)
			throws Throwable {
		return new XMPPChatConnector(instanceId,properties);
	}

}
