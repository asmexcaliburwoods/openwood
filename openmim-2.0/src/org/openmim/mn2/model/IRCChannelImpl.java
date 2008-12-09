package org.openmim.mn2.model;

import org.openmim.mn2.controller.IRCChannelUtil;
import org.openmim.mn2.controller.MN2Factory;
import squirrel_util.Lang;
import squirrel_util.StringUtil;

public class IRCChannelImpl extends RoomImpl implements IRCChannel {
    private String channelName;
    private String channelNameCanonical;

    public IRCChannelImpl(String channelName, MN2Factory MN2Factory) {
        Lang.ASSERT(IRCChannelUtil.isChannelName(channelName), "channelName must be must be at least one char long, and must start with '#' or '&', but it is " + StringUtil.toPrintableString(channelName));
        this.channelName = channelName;
        this.channelNameCanonical = MN2Factory.getNameConvertor().toCanonicalIRCChannelName(channelName);
    }

    public String getChannelNameCanonical() {
        return channelNameCanonical;
    }

    public void setInviteOnly(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setChannelKeyUsed(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setPrivateChannelMode(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setSecret(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setChannelKey(String key) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setLimited(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setLimit(int limit) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setModerated(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setNoExternalMessages(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setOnlyOpsChangeTopic(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTopic(String topicText) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addRole(IRCChannelParticipant role) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void parts(IRCChannelParticipant user, String partsComment) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void joins(IRCChannelParticipant user) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void kicked(IRCChannelParticipant user, User kicker, String comment) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void meParts(IRCChannelParticipant me) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addIRCChannelListener(IRCChannelListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeIRCChannelListener(IRCChannelListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public String getChannelName() {
        return channelName;
    }

    public String toString() {
        return getChannelName();
    }
}
