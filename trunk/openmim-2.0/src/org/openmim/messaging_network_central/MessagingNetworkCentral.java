package org.openmim.messaging_network_central;

import org.openmim.infrastructure.Context;
import org.openmim.messaging_network2.model.ConfigurationBean;
import org.openmim.messaging_network3.MetaNetwork;

public class MessagingNetworkCentral {
    private Context ctx;
    private StatusRoomListenerExternalGUIImpl statusRoomListenerExternalGUI;
    private IMListenerImpl imListener;

    private MessagingNetworkCentral(ConfigurationBean configurationBean) {
        statusRoomListenerExternalGUI = new StatusRoomListenerExternalGUIImpl();
        ctx=new Context(new MetaNetwork(), configurationBean, statusRoomListenerExternalGUI);
        ctx.getMetaNetwork().postLoad(imListener=new IMListenerImpl());
    }

    public static MessagingNetworkCentral create(ConfigurationBean configurationBean) {
        return new MessagingNetworkCentral(configurationBean);
    }

    public void doAll(MessagingNetworkCentral_Listener messagingNetworkCentral_Listener){
        statusRoomListenerExternalGUI.setGUI(messagingNetworkCentral_Listener);
        ctx.setKernelListener(messagingNetworkCentral_Listener);
        imListener.setKernelListener(messagingNetworkCentral_Listener);
        ctx.getMetaNetwork().startReconnecting(ctx, ctx.getStatusRoomListenerExternal());
    }

    public Context getCtx() {
        return ctx;
    }
}
