package org.openmim.messaging_network2.model;

//
public interface IRCChannel extends Room {

    String getChannelName();

    String getChannelNameCanonical();

    void setInviteOnly(boolean b);

    void setChannelKeyUsed(boolean b);

    void setPrivateChannelMode(boolean b);

    void setSecret(boolean b);

    void setChannelKey(String key);

    void setLimited(boolean b);

    void setLimit(int limit);

    void setModerated(boolean b);

    void setNoExternalMessages(boolean b);

    void setOnlyOpsChangeTopic(boolean b);

    void setTopic(String topicText);

    void addRole(IRCChannelParticipant role);
    void parts(IRCChannelParticipant user, String partsComment);
    void joins(IRCChannelParticipant user);
    void kicked(IRCChannelParticipant user, User kicker, String comment);
    void meParts(IRCChannelParticipant me);

    public static interface IRCChannelListener{}
    void addIRCChannelListener(IRCChannelListener listener);
    void removeIRCChannelListener(IRCChannelListener listener);
}
