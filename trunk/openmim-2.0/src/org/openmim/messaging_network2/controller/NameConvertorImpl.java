package org.openmim.mn2.controller;

import org.openmim.mn2.controller.NameConvertor;

public class NameConvertorImpl implements NameConvertor {
    public String toCanonicalIRCNick(String nick) {
        return nick.toLowerCase();
    }

    public String toCanonicalIRCChannelName(String channelName) {
        return channelName.toLowerCase();
    }

    public String toCanonicalIMNetworkKey(String ircNetworkKey) {
        return ircNetworkKey.toLowerCase();
    }
}
