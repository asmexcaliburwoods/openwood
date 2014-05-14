package org.openmim.mn2.model;

import org.openmim.irc.regexp.IRCMask;
import org.openmim.mn2.controller.IMNetwork;

import java.util.Vector;

public interface IMListener {
    void actionMessage(IMNetwork net,Room room, String nickName, String text);

    void message(IMNetwork net,Room room, String nickFrom, String text);

    void awayMessage(IMNetwork net,String nick, String msg);



    void serverReplyBanListEnd(IMNetwork net,String s);

    void serverReplyBanListItem(IMNetwork net,String s, String s1);

    void serverReplyBanListStart(IMNetwork net);

    void isonReply(IMNetwork net,String serverAddr, String[] nicksOnline);

    void listEnd(IMNetwork net);

    void listItem(IMNetwork net,String s, int i, String s1);

    void listStart(IMNetwork net);

    void modeChangeRaw(IMNetwork net,String senderSpecification, String s, String s1, Vector vector, String myCurrentNick);







    void meParts(IMNetwork net,String s, String s2);

    void whoisEnd(IMNetwork net,String s);

    void meJoined(IMNetwork net,String s);

    void invalidNick(IMNetwork net,String s, String s1);

    void nickChange(IMNetwork net,String s, String s1);

    void meQueried(IMNetwork net,String roomName);

    void quit(IMNetwork net,String s, String s1);

    void cannotJoinChannel(IMNetwork net,String s1, String s2);

    void onVoice(IMNetwork net,String channel);

    void onDeVoice(IMNetwork net,String channel);

    void unhandledCommand(String text);

    void kickedTryingToRejoin(IMNetwork net,String channel);

    void userQuit(IMNetwork net,boolean me, String nick, String note);

    void userLoggedOn(IMNetwork net,boolean me, String nick, String note);

    void willBeIgnored(IMNetwork net,IRCMask mask, boolean ignored);

    void channelClicked(IMNetwork net,String channelName);

    void urlClicked(String s);

    void noSuchNickChannel(IMNetwork net,String nickOrChannel, String comment);
}
