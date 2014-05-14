package org.openmim.messaging_network2.controller;

public interface NameConvertor {
    String toCanonicalIRCNick(String nick);
    String toCanonicalIRCChannelName(String channelName);
    String toCanonicalIMNetworkKey(String ircNetworkKey);
}
