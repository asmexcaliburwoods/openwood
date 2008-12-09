package org.openmim.kernel;

import org.openmim.mn2.model.IMListener;
import org.openmim.mn2.controller.IMNetwork;
import org.openmim.mn2.model.Room;
import org.openmim.irc.regexp.IRCMask;

import java.util.Vector;

public class IMListenerImpl implements IMListener {
    private KernelListener kl;

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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void serverReplyBanListStart(IMNetwork net) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void isonReply(IMNetwork net,String serverAddr, String[] nicksOnline) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void listEnd(IMNetwork net) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void listItem(IMNetwork net,String s, int i, String s1) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void listStart(IMNetwork net) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void modeChangeRaw(IMNetwork net, String senderSpecification, String s, String s1, Vector vector) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void meParts(IMNetwork net, String s, String s2) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void whoisEnd(IMNetwork net, String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void meJoined(IMNetwork net,String channel) {
        kl.onMeJoined(net,channel);
    }

    public void invalidNick(IMNetwork net, String s, String s1) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void nickChange(IMNetwork net,String s, String s1) {
        kl.onNickChange(net,s,s1);
    }

    public void meQueried(IMNetwork net, String roomName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void quit(IMNetwork net, String s, String s1) {
        kl.onQuit(net,s,s1);
    }

    public void cannotJoinChannel(IMNetwork net, String s1, String s2) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void onVoice(IMNetwork net, String channel) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void onDeVoice(IMNetwork net, String channel) {
        //To change body of implemented methods use File | Settings | File Templates.
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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void channelClicked(IMNetwork net, String channelName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void urlClicked(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void noSuchNickChannel(IMNetwork net, String nickOrChannel, String comment) {
        kl.onNoSuchNickChannel(net,nickOrChannel,comment);
    }

    public void setKernelListener(KernelListener kernelListener) {
        this.kl=kernelListener;
    }
}
