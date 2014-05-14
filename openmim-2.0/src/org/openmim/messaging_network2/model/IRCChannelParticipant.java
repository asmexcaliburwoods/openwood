package org.openmim.messaging_network2.model;

import java.util.List;

public interface IRCChannelParticipant extends RoomParticipant {

    IRCChannel getChannel();

    void setVoice(boolean b);

    void setOp(boolean b);

    void setHalfOp(boolean b);

    void modeChange(String senderSpecification, String modeFor, List<String> vector);

    public static interface IRCChannelParticipantListener{}
    void addIRCChannelParticipantListener(IRCChannelParticipantListener listener);
    void removeIRCChannelParticipantListener(IRCChannelParticipantListener listener);
}
