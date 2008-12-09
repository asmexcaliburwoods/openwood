package org.openmim.mn2.model;

import squirrel_util.Lang;

//
public class QueryImpl extends RoomImpl implements Query {
    private RoomParticipant myParty;
    private RoomParticipant me;

    public QueryImpl(RoomParticipant me, RoomParticipant myParty) {
        super();
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
