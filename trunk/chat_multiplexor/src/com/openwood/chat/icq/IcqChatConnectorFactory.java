package com.openwood.chat.icq;

import java.util.Properties;

import com.openwood.chat.ChatConnector;
import com.openwood.chat.ChatConnectorFactory;

public class IcqChatConnectorFactory implements ChatConnectorFactory {

	@Override
	public ChatConnector createInstance(String instanceId, Properties properties)
			throws Throwable {
		return new IcqChatConnector(instanceId,properties);
	}

}
