package org.openmim.messaging_network2.model;

import org.openmim.messaging_network2.controller.IMNetwork;

import com.egplab.utils.Lang;

//
public class QueryImpl extends RoomImpl implements Query {
    private RoomParticipant myParty;
    private RoomParticipant me;

    public QueryImpl(IMNetwork n, RoomParticipant me, RoomParticipant myParty) {
        super(n);
        Lang.ASSERT_NOT_NULL(me, "me");
        Lang.ASSERT_NOT_NULL(myParty, "myParty");
        this.me = me;
        this.myParty = myParty;
    }

    public RoomParticipant getMe() {
        return me;
    }

    public RoomParticipant getMyParty() {
        return myParty;
    }

    public String toString() {
        return getMe().getUser()+" with "+getMyParty().getUser();
    }
}
