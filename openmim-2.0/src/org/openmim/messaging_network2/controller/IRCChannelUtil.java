package org.openmim.messaging_network2.controller;

public class IRCChannelUtil {
    public static boolean isChannelName(String channelName){
        return channelName != null && channelName.length() >= 1 && "#&".indexOf(channelName.charAt(0)) != -1;//todo get from IRC server data and make it relative to IRCServer
    }
}
