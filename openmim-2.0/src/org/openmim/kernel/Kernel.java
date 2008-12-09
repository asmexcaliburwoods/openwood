package org.openmim.kernel;

import org.openmim.infrastructure.Context;
import org.openmim.metanetwork.MetaNetwork;
import org.openmim.mn2.model.ConfigurationBean;

public class Kernel {
    private Context ctx;
    private StatusRoomListenerExternalGUIImpl statusRoomListenerExternalGUI;
    private IMListenerImpl imListener;

    private Kernel(ConfigurationBean configurationBean) {
        statusRoomListenerExternalGUI = new StatusRoomListenerExternalGUIImpl();
        ctx=new Context(new MetaNetwork(), configurationBean, statusRoomListenerExternalGUI);
        ctx.getMetaNetwork().postLoad(imListener=new IMListenerImpl());
    }

    public static Kernel create(ConfigurationBean configurationBean) {
        return new Kernel(configurationBean);
    }

    public void doAll(KernelListener kernelListener){
        statusRoomListenerExternalGUI.setGUI(kernelListener);
        ctx.setKernelListener(kernelListener);
        imListener.setKernelListener(kernelListener);
        ctx.getMetaNetwork().startReconnecting(ctx, ctx.getStatusRoomListenerExternal());
    }

    public Context getCtx() {
        return ctx;
    }
}
