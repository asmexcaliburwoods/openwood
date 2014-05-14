package openwood.chat.irc;

import java.util.Properties;

import openwood.chat.ChatConnector;
import openwood.chat.ChatConnectorFactory;

public class IrcChatConnectorFactory implements ChatConnectorFactory {

	@Override
	public ChatConnector createInstance(String instanceId, Properties properties)
			throws Throwable {
		return new IrcChatConnector(instanceId,properties);
	}

}
