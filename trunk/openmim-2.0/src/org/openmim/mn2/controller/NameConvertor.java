package org.openmim.mn2.controller;

public interface NameConvertor {
    String toCanonicalIRCNick(String nick);
    String toCanonicalIRCChannelName(String channelName);
    String toCanonicalIMNetworkKey(String ircNetworkKey);
}
