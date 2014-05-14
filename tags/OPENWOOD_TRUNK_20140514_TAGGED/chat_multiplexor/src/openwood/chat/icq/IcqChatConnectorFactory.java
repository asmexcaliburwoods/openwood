package openwood.chat.icq;

import java.util.Properties;

import openwood.chat.ChatConnector;
import openwood.chat.ChatConnectorFactory;

public class IcqChatConnectorFactory implements ChatConnectorFactory {

	@Override
	public ChatConnector createInstance(String instanceId, Properties properties)
			throws Throwable {
		return new IcqChatConnector(instanceId,properties);
	}

}
