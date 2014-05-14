package org.openmim.messaging_network2.model;

import java.util.List;


public class IRCChannelParticipantImpl
        extends RoomParticipantImpl
        implements IRCChannelParticipant {

    public IRCChannel getChannel() {
        return (IRCChannel) getRoom();
    }

    public void setVoice(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setOp(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setHalfOp(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void modeChange(String senderSpecification, String modeFor, List<String> vector) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addIRCChannelParticipantListener(IRCChannelParticipantListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeIRCChannelParticipantListener(IRCChannelParticipantListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
