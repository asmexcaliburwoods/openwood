package org.openmim.metanetwork;

import org.openmim.irc.mn.IRCMessagingNetwork2;
import org.openmim.mn2.MessagingNetwork2;
import org.openmim.mn2.controller.IMNetwork;
import org.openmim.mn2.model.IMListener;
import org.openmim.mn2.model.StatusRoomListenerExternal;
import org.openmim.mn2.model.IMNetworkBean;
import org.openmim.infrastructure.Context;

import java.util.List;
import java.util.ArrayList;

public class MetaNetwork {
    private IRCMessagingNetwork2 irc=new IRCMessagingNetwork2();
    private IMListener imListener;

    public IRCMessagingNetwork2 getIrc() {
        return irc;
    }

    public IMListener getImListener() {
        return imListener;
    }

    public MessagingNetwork2 getMessagingNetwork2ByType(IMNetwork.Type type){
        switch (type){
            case irc: return irc;
            default: throw new AssertionError();
        }
    }
    public void postLoad(IMListener imListener){
        this.imListener=imListener;
    }

    public void startReconnecting(Context context, StatusRoomListenerExternal statusRoomListenerExternal) {
        List<IMNetworkBean> nets=context.getConfigurationBean().getNetworksConfigured();
        if(nets!=null)for (IMNetworkBean bean : nets) {
            MessagingNetwork2 net=getMessagingNetwork2ByType(bean.getType());
            net.load(context, imListener, statusRoomListenerExternal);
            net.startReconnecting();
        }
    }

    public List<MessagingNetwork2> getNetworks(){
        List<MessagingNetwork2> nets=new ArrayList<MessagingNetwork2>(1);
        nets.add(irc);
        return nets;
    }
}
