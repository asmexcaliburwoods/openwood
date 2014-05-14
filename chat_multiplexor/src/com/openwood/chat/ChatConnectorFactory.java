package com.openwood.chat;

import java.util.Properties;

public interface ChatConnectorFactory {
	ChatConnector createInstance(String instanceId, Properties properties)throws Throwable;
}
