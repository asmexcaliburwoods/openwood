package org.openmim.irc.irc_messaging_network;

import org.openmim.infrastructure.Context;
import org.openmim.messaging_network2.MessagingNetwork2;
import org.openmim.messaging_network2.MessagingNetwork2Listener;
import org.openmim.messaging_network2.controller.IMNetwork;
import org.openmim.messaging_network2.controller.MN2Factory;
import org.openmim.messaging_network2.model.*;

import com.egplab.exception_handling.ExceptionUtil;

import java.util.ArrayList;
import java.util.List;

public class IRCMessagingNetwork2 implements MessagingNetwork2{
    private MessagingNetwork2Listener listener;
    private MN2Factory mn2Factory=createFactory();
    private List<IMNetwork> imNetworks;

    public List<IMNetwork> getIMNetworks() {
        return imNetworks;
    }

    public boolean supportsChannels() {
        return true;
    }

    public String getDisplayName() {
        return "IRC";
    }

    private MN2Factory createFactory() {
        return IRCMN2FactoryImpl.createFactory();
    }

    public MN2Factory getFactory() {
        return mn2Factory;
    }

    public void setListener(MessagingNetwork2Listener listener) {
        this.listener=listener;
    }

    public void load(Context ctx, IMListener imListener, StatusRoomListenerExternal statusRoomListenerExternal) {
        this.imNetworks=new ArrayList<IMNetwork>();
        final List<IMNetworkBean> imNetworkBeans = ctx.getConfigurationBean().getNetworksConfigured();
        for (IMNetworkBean bean : imNetworkBeans) {
            String key=bean.getKey();
            String keyCanon=getFactory().getNameConvertor().toCanonicalIMNetworkKey(key);
            IMNetwork imNetwork=getFactory().createIMNetwork(bean.getType(),key,
                    imListener,
                    new MyStatusRoomListenerInternal(statusRoomListenerExternal),
                    getFactory().createListOfServersToKeepConnectionWith(
                            ctx.getConfigurationBean().
                                    getNetworkKeyCanonical2ListOfServersToKeepConnectionWith().
                                    get(keyCanon),
                            ctx.getConfigurationBean()),
                    ctx);
            imNetworks.add(imNetwork);
        }
    }

    public void startReconnecting() {
        for (IMNetwork imNetwork : imNetworks) {
            try {
                imNetwork.startReconnecting();
            } catch (Exception e) {
                ExceptionUtil.handleException(e);
            }
        }
    }

    private static class MyStatusRoomListenerInternal implements StatusRoomListenerInternal {
        private StatusRoomListenerExternal statusRoomListenerExternal;

        private MyStatusRoomListenerInternal(StatusRoomListenerExternal statusRoomListenerExternal) {
            this.statusRoomListenerExternal=statusRoomListenerExternal;
        }

        public void notice(StatusRoom room, String nick, String text) {
            try {
                getCreateRoom(room).notice(nick,text);
            } catch (Exception e) {
                ExceptionUtil.handleException(e);
            }
        }

        private StatusRoom getCreateRoom(StatusRoom room) {
            try {
                statusRoomListenerExternal.onGetCreateStatusRoom(room);
            } catch (Exception e) {
                ExceptionUtil.handleException(e);
            }
            return room;
        }

        public void welcome(StatusRoom statusRoom, String newNick, String text) {
            try {
                getCreateRoom(statusRoom).welcome(newNick, text);
            } catch (Exception e) {
                ExceptionUtil.handleException(e);
            }
            try {
                statusRoomListenerExternal.connected(statusRoom.getIMNetwork());
            } catch (Exception e) {
                ExceptionUtil.handleException(e);
            }
        }

        public void registering(StatusRoom statusRoom) {
            try {
                getCreateRoom(statusRoom).registering();
            } catch (Exception e) {
                ExceptionUtil.handleException(e);
            }
        }
    }
}
