package org.openmim.messaging_network_central;

import org.openmim.messaging_network2.controller.IMNetwork;
import org.openmim.messaging_network2.model.IMListener;
import org.openmim.messaging_network2.model.Room;
import org.openmim.irc.regexp.IRCMask;

import java.util.Vector;

public class IMListenerImpl implements IMListener {
    private MessagingNetworkCentral_Listener kl;

    public void actionMessage(IMNetwork net,Room room, String nickName, String text) {
        kl.onActionMessage(net,room,nickName,text);
    }

    public void message(IMNetwork net,Room room, String nickFrom, String text) {
        kl.onMessage(net,room,nickFrom,text);
    }

    public void awayMessage(IMNetwork net,String nick, String msg) {
        kl.onAwayMessage(net,nick,msg);
    }

    public void serverReplyBanListEnd(IMNetwork net,String s) {
    }

    public void serverReplyBanListItem(IMNetwork net,String s, String s1) {
        
    }

    public void serverReplyBanListStart(IMNetwork net) {
        
    }

    public void isonReply(IMNetwork net,String serverAddr, String[] nicksOnline) {
        
    }

    public void listEnd(IMNetwork net) {
        
    }

    public void listItem(IMNetwork net,String s, int i, String s1) {
        
    }

    public void listStart(IMNetwork net) {
        
    }

    public void modeChangeRaw(IMNetwork net, String senderSpecification, String s, String s1, Vector vector, String myCurrentNick) {
//    	sendsp: slowbot
//    	s: slowbot
//    	s1: +x
    	if(s!=null&&s.equalsIgnoreCase(myCurrentNick))kl.onModeChangeForMe(s1);

//        System.out.println("sendsp: "+senderSpecification);
//        System.out.println("s: "+s);
//        System.out.println("s1: "+s1);
//        if(vector==null)System.out.println("vector: null");
//        else{
//        	System.out.println("vector: {");
//        	for(Object o:vector){String vs=""+o;System.out.println("  vec[i]:"+vs);}
//        	System.out.println("}");
//        }
    }

    public void meParts(IMNetwork net, String s, String s2) {
        
    }

    public void whoisEnd(IMNetwork net, String s) {
        
    }

    public void meJoined(IMNetwork net,String channel) {
        kl.onMeJoined(net,channel);
    }

    public void invalidNick(IMNetwork net, String s, String s1) {
        
    }

    public void nickChange(IMNetwork net,String s, String s1) {
        kl.onNickChange(net,s,s1);
    }

    public void meQueried(IMNetwork net, String roomName) {
        
    }

    public void quit(IMNetwork net, String s, String s1) {
        kl.onQuit(net,s,s1);
    }

    public void cannotJoinChannel(IMNetwork net, String s1, String s2) {
        
    }

    public void onVoice(IMNetwork net, String channel) {
        
    }

    public void onDeVoice(IMNetwork net, String channel) {
        
    }

    public void unhandledCommand(String text) {
    }

    public void kickedTryingToRejoin(IMNetwork net, String channel) {
    }

    public void userQuit(IMNetwork net, boolean me, String nick, String note) {
        kl.onUserQuit(net,me,nick,note);
    }

    public void userLoggedOn(IMNetwork net, boolean me, String nick, String note) {
        kl.onUserLoggedOn(net,me,nick,note);
    }

    public void willBeIgnored(IMNetwork net, IRCMask mask, boolean ignored) {
        
    }

    public void channelClicked(IMNetwork net, String channelName) {
        
    }

    public void urlClicked(String s) {
        
    }

    public void noSuchNickChannel(IMNetwork net, String nickOrChannel, String comment) {
        kl.onNoSuchNickChannel(net,nickOrChannel,comment);
    }

    public void setKernelListener(MessagingNetworkCentral_Listener messagingNetworkCentral_Listener) {
        this.kl=messagingNetworkCentral_Listener;
    }
}
