package org.openmim.mn2.model;

import org.openmim.infrastructure.Context;
import org.openmim.mn2.controller.IMNetwork;

import java.util.List;
import java.util.Vector;

public class StatusRoomImpl extends RoomImpl implements StatusRoom{
    private List<StatusRoomListenerInternal> statusRoomListenersInternal;
    private StatusRoomListenerExternal listenerStatusRoom;
    private IMNetwork imNetwork;

    public StatusRoomImpl(IMNetwork imNetwork, Context ctx) {
        this.imNetwork = imNetwork;
        setStatusRoomListener(ctx.getStatusRoomListenerExternal());
    }

    public IMNetwork getIMNetwork() {
        return imNetwork;
    }

    public void connecting() {
        getStatusRoomListener().connecting(this);
    }

    public void setInviteOnly(boolean b) {
    }

    public void setChannelKeyUsed(boolean b) {
    }

    public void setPrivateChannelMode(boolean b) {
    }

    public void setSecret(boolean b) {
    }

    public void addUser(String nickNameAffected, boolean operator, boolean modeOn) {
    }

    public void setChannelKey(String key) {
    }

    public void setLimited(boolean b) {
    }

    public void setLimit(int limit) {
    }

    public void setModerated(boolean b) {
    }

    public void setNoExternalMessages(boolean b) {
    }

    public void setOnlyOpsChangeTopic(boolean b) {
    }

    public void reportStatus(String s) {
    }

    protected synchronized void addRole_internal(RoomParticipant participant) {
    }

    public synchronized void delete() {
    }

    public synchronized void deleteRole_internal(RoomParticipant participant) {
    }

    public void addStatusRoomListener(StatusRoomListenerInternal listenerInternal) {
        this.statusRoomListenersInternal.add(listenerInternal);
    }

    public List<StatusRoomListenerInternal> getStatusRoomListeners() {
        return statusRoomListenersInternal;
    }

    public void setStatusRoomListener(StatusRoomListenerExternal listener) {
        this.listenerStatusRoom=listener;
    }

    public StatusRoomListenerExternal getStatusRoomListener() {
        return listenerStatusRoom;
    }

    public void modeChange(String senderSpecification, String modeFor, Vector vector) {
        
    }

    public void notice(String nick, String text) {
        getStatusRoomListener().notice(this,nick,text);
    }

    public void welcome(String newNick, String text) {
        getStatusRoomListener().welcome(this,newNick,text);
    }

    public void registering() {
        getStatusRoomListener().registering(this);
    }
}
