package openwood.chat.xmpp;

import java.util.Properties;

import openwood.chat.ChatConnector;
import openwood.chat.ChatConnectorFactory;

public class XMPPChatConnectorFactory implements ChatConnectorFactory {

	@Override
	public ChatConnector createInstance(String instanceId, Properties properties)
			throws Throwable {
		return new XMPPChatConnector(instanceId,properties);
	}

}
