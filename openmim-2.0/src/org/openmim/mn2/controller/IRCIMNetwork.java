package org.openmim.mn2.controller;

import org.openmim.exception_handler.ExceptionUtil;
import org.openmim.infrastructure.Context;
import org.openmim.irc.driver.*;
import org.openmim.irc.regexp.IRCMask;
import org.openmim.irc.regexp_util.IRCMaskUtil;
import org.openmim.mn2.MessagingNetwork2;
import org.openmim.mn2.model.*;
import squirrel_util.ExpectException;
import squirrel_util.Lang;
import squirrel_util.Logger;
import squirrel_util.StringUtil;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class IRCIMNetwork implements IMNetwork {
    private final String VERSION = "2008.8.16.001";
    private final StatusRoom statusRoom;
    private String key;
    private String keyCanonical;
    private MN2Factory MN2Factory;
    private List<Server> listOfIRCServersToKeepConnectionWith;
    private IRCClient client;
    private IRCServerImpl server;
    private Thread ircProcessorThread;
    private IMListener imListener;
    private Room curRoom;
    private StatusRoomListenerInternal statusRoomListenerInternal;
    private Context ctx;

    /**
     * @param key not canonical
     */
    public IRCIMNetwork(String key, Context ctx, IMListener imListener, StatusRoomListenerInternal statusRoomListenerInternal,
                        List<Server> listOfIRCServersToKeepConnectionWith) {
        statusRoom = new StatusRoomImpl(this, ctx);
        try {
            this.imListener = imListener;
            this.statusRoomListenerInternal = statusRoomListenerInternal;
            this.key = key;
            this.ctx = ctx;
            this.MN2Factory = ctx.getMetaNetwork().getIrc().getFactory();
            this.keyCanonical = MN2Factory.getNameConvertor().toCanonicalIMNetworkKey(key);
            this.listOfIRCServersToKeepConnectionWith = listOfIRCServersToKeepConnectionWith;
            syncPrint("ctor IRC v." + VERSION);
            init();
        } catch (Exception e) {
            ExceptionUtil.handleException(e);
        }
    }
    public void sendMessage(String recipLoginId,String plainText)throws Exception{
    	getIRCProtocol().sendMessage(recipLoginId, plainText);
    }

    public void load(Context context) {
        Logger.DEBUG = context.getConfigurationBean().getGlobalIRCParameters().isDebug();
        autoRejoinChannelOnKick = context.getConfigurationBean().getGlobalIRCParameters()
                .isAutoRejoinChannelsOnKick();
    }

    void init() {
        createAll();
    }

    private void syncPrint(String msg) {
        System.err.println(getClass().getSimpleName() + " " + Thread.currentThread().getName() + " " + msg);
    }

    private void createAll() {
        this.server = (IRCServerImpl) getServersStartedConnectingOrConnected().get(0);
        this.client = new IRCClient();
        ircController = getIRCProtocol().getLocalClient();        
    }

    public boolean isChannelNameValid(String channel) {
        return channel != null && channel.length() != 0 && (channel.startsWith("#") || channel.startsWith("&"));
    }

    public void addContact(String loginId, boolean chhannel) {
        if (chhannel) {
            try {
                client.join(loginId);
            } catch (IOException e) {
                ExceptionUtil.handleException(e);
            }
        }
    }

    public void joinRoom(ContactListLeaf leaf) throws IOException {
//        Room room = getCreateRoom(leaf.getLoginId());
        if (leaf.isChannel()) client.join(leaf.getLoginId());
//        return room;
    }

    public Type getType() {
        return IMNetwork.Type.irc;
    }

    public String getKey() {
        return key;
    }

    public String getCurrentLoginId() {
        return getActiveNick();
    }

    public List<Server> getServersStartedConnectingOrConnected() {
        return listOfIRCServersToKeepConnectionWith;
    }

    public IMNetworkBean toBean() {
        IRCNetworkBean bean = new IRCNetworkBean();
        bean.setKey(getKey());
        bean.setType(getType());
        ArrayList<ServerBean> list = new ArrayList<ServerBean>();
        for (Server server : listOfIRCServersToKeepConnectionWith) {
            IRCServerBean sb = (IRCServerBean) server.toBean(bean);
            list.add(sb);
        }
        bean.setServers(list);
        return bean;
    }
    public String toString(){return key;}
    public synchronized void startReconnecting() {
//        for (Server server : listOfIRCServersToKeepConnectionWith) {
//            server.startReconnecting();
//        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    statusRoom.connecting();
                    new Identd(server.getIdentdUserName());
                    connect();
                } catch (Throwable e) {
                    ExceptionUtil.handleException(e);
                }
            }
        }).start();
    }

    public MessagingNetwork2 getMN2() {
        return ctx.getMetaNetwork().getIrc();
    }

    public boolean isConnected() {
        return connected;
    }

    private boolean connected;

    private Runnable createIRCProcessorThread() {
        return new Runnable() {
            public void run() {
                try {
                    client.process();
                }
                catch (IOException sex) {
                    Logger.printException(sex);
                    if (!isConnected())
                        reportStatus("Error: " + sex);
                }
                catch (Throwable exception) {
                    ExceptionUtil.handleException(exception);
                    reportStatus("Error: " + exception);
                }
                finally {
                    try {
                        cleanupOnDisconnect();
                    } catch (Throwable e) {
                        ExceptionUtil.handleException(e);
                    }
                }
            }
        };
    }

    public void connect() throws IOException {
        syncPrint("waiting for connect...");
        synchronized (this) {
            syncPrint("connect");
            try {
                Lang.ASSERT(!isConnected(), "must be disconnected first");
                sendLoginSequence();
                ircProcessorThread = new Thread(createIRCProcessorThread());
                ircProcessorThread.start();
            }
            finally {
                syncPrint("connect finished");
            }
        }
    }

    private synchronized void stopNotifyListUpdaterThread() {
        if (notifyListUpdaterThread != null) {
            try {
                notifyListUpdaterThread.interrupt();
                notifyListUpdaterThread.join();
            }
            catch (Exception ex) {
                ExceptionUtil.handleException(ex);
            }
        }
    }

    private void cleanupOnDisconnect() {
        if (isConnected()) {
            syncPrint("waiting for cleanupOnDisconnect...");
            synchronized (this) {
                syncPrint("cleanupOnDisconnect");
                try {
                    setConnected(false);
                    stopNotifyListUpdaterThread();
                    reportStatus("cd/3");
                    try {
                        if (client != null)
                            client.close();
                        client = null;
                    }
                    catch (Exception exception) {
                        Logger.printException(exception);
                    }
                    reportStatus("cd/4");
                    try {
                        if (ircProcessorThread != null) {
                            ircProcessorThread.interrupt();
                            ircProcessorThread.join();
                            ircProcessorThread = null;
                        }
                    }
                    catch (Exception exception) {
                        Logger.printException(exception);
                    }
                    reportStatus(("Disconnected."));
                }
                catch (Exception exception1) {
                    Logger.printException(exception1);
                }
                finally {
                    syncPrint("cleanupOnDisconnect finished");
                }
            }
        }
    }

    private void setConnected(boolean b) {
        connected = b;
    }

    public synchronized NotifyListImpl getNotifyList() {
        if (notifyList == null) {
            notifyList = new NotifyListImpl();
            notifyList.addNotifyListListener(new NotifyListListener() {
                public void notifyListChanged(NotifyListChangedEvent ev) {
                    IRCIMNetwork.this.notifyListChanged();
                    if (ev.getId() == NotifyListChangedEvent.ITEM_ADDED || ev.getId() == NotifyListChangedEvent.ITEM_REMOVED) {
                        String note = ev.getNotifyListItem().getNote();
                        if (StringUtil.isNullOrTrimmedEmpty(note))
                            note = "";
                        else
                            note = " (" + note + ")";
                        String key = "Notify list item " + (ev.getId() == NotifyListChangedEvent.ITEM_ADDED ? "added" : "removed") + " for";
                        reportStatus((key) + " " + ev.getNotifyListItem().getUserMask() + note);
                    } else {
                        String key = "Notify list " + (ev.getId() == NotifyListChangedEvent.NOTIFY_LIST_ENABLED ? "enabled." : "disabled.");
                        reportStatus((key));
                    }

                }
            });
        }
        return notifyList;
    }

    public synchronized void notifyListChanged() {
        boolean runThread = getNotifyList().isEnabled() && getNotifyList().size() > 0;
        if (runThread) {
            if (notifyListUpdaterThread.isAlive()) {
                try {
                    sendUpdateNotifyListRequest();
                }
                catch (IOException ix) {
                    ExceptionUtil.handleException(ix);
                }
            } else {
                notifyListUpdaterThread.start();
            }
        } else {
            stopNotifyListUpdaterThread();
        }
    }

    private void sendLoginSequence() throws IOException {
        String nick = server.getNickNames().get(0);
        ancientNick = nick;
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(nick, "nick");
        try {
            reportStatus("Connecting...");
            //getResourceString("Connecting to") + " " + serverAddressToConnect + ":" + serverPortToConnect + "...");
//            nicksLastOnline_nicklow2nick = new Hashtable();
            client.init(server.getHostNameRedirD(), server.getHostNameRealServer(),
                    server.getRedirdPort(), nick, server.getIdentdUserName(),
                    server.getPassword(), server.getRealName(),
                    new FilteringIRCListener(new AppletIRCListener(), getIgnoreList()),
                    MN2Factory);
            setConnected(true);
            if (notifyList != null)
                notifyListChanged();
        }
        catch (RuntimeException rte) {
            reportStatus(("Can't connect to a server:") + " " + rte);
            throw rte;
        }
        catch (IOException ioe) {
            reportStatus(("Can't connect to a server:") + " " + ioe);
            throw ioe;
        }
    }

    public StatusRoom getStatusRoom() {
        return statusRoom;
    }

    private IRCController ircController;

    private class AppletIRCListener extends AbstractListener {

        public AppletIRCListener() {
        }

        public void actionMessageReceived(User sender, String s1, String s2) {
            imListener.actionMessage(IRCIMNetwork.this, prepareRoomForMessage(sender, s1), ((IRCUser) sender).getActiveNick(), s2);
        }

        public void cannotJoinChannel(String s, String s1) {
            imListener.cannotJoinChannel(IRCIMNetwork.this, s1, s);
        }

        public void connected() {
            //applet.reportStatus(applet.getResourceString("Connected."));
        }

        public void connecting() {
        }

        public void disconnected() {
            reportStatus(("Disconnected."));
        }

        public void handleAwayMessage(java.lang.String nick, java.lang.String comment) throws java.io.IOException {
            String msg = SYS_PREFIX + nick + " " + ("is away") + ": " + comment;
            imListener.awayMessage(IRCIMNetwork.this, nick, msg);
        }

        public void handleBanListEnd(String s) throws IOException {
            imListener.serverReplyBanListEnd(IRCIMNetwork.this, s);
        }

        public void handleBanListItem(String s, String s1) throws IOException {
            imListener.serverReplyBanListItem(IRCIMNetwork.this, s, s1);
        }

        public void handleException(Throwable tr) {
            ExceptionUtil.handleException(tr);
        }

        /**
         * handleIsonReply method comment.
         */
        public void handleIsonReply(java.lang.String serverAddr, java.lang.String[] nicksOnline) throws java.io.IOException {
            imListener.isonReply(IRCIMNetwork.this, serverAddr, nicksOnline);
        }

        public void handleListEnd() {
            imListener.listEnd(IRCIMNetwork.this);
        }

        public void handleListItem(String s, int i, String s1) {
            imListener.listItem(IRCIMNetwork.this, s, i, s1);
        }

        public void handleListStart() {
            imListener.listStart(IRCIMNetwork.this);
        }

        public void handleModeChangeRaw(String senderSpecification, String s, String s1, Vector vector) {
            imListener.modeChangeRaw(IRCIMNetwork.this, senderSpecification, s, s1, vector);
        }

        /**
         * handleNoSuchNickChannel method comment.
         */
        public void handleNoSuchNickChannel(java.lang.String nickOrChannel, java.lang.String comment) throws java.io.IOException {
            imListener.noSuchNickChannel(IRCIMNetwork.this, nickOrChannel, comment);
        }

        /**
         * handleNotice method comment.
         */
        public void handleNotice(java.lang.String sender, User user, java.lang.String text) throws java.io.IOException {
            String nick = user == null ? sender : ((IRCUser) user).getActiveNick();
            statusRoomListenerInternal.notice(statusRoom, nick, text);
        }

        public void handlePart(String chan, User c, String comment) {
            String s1 = ((IRCUser) c).getActiveNick();
            if (s1 != null)
                if (s1.equalsIgnoreCase(getActiveNick())) {
                    imListener.meParts(IRCIMNetwork.this, chan, comment);
                } else {
                    final IRCChannel channel = (IRCChannel) getRoom(chan);
                    channel.parts(ircController.getChannelRoleByChannelName((IRCUser) c, chan), comment);
                }
            else {
                Exception ex = new ExpectException("s1 is null");
                Logger.printException(ex);
            }
        }

        public void handleTopic(String channel, String topicText) {
            IRCChannel room = (IRCChannel) getCreateRoom(channel);
            room.setTopic(topicText);
        }

        public void handleWhoisChannelsOn(String s, String s1) {
            getCreateWhoisUserInfoFor(s).setChannelsOn(s1);
        }

        public void handleWhoisEnd(String s, String s1) {
            imListener.whoisEnd(IRCIMNetwork.this, s);
        }

        public void handleWhoisIdleTime(String s, int i, String s1) {
            getCreateWhoisUserInfoFor(s).setIdleTime(i);
        }

        public void handleWhoisOperator(String s, String s1) {
            getCreateWhoisUserInfoFor(s).setIrcOperator(true);
        }

        public void handleWhoisServer(String s, String s1, String s2) {
            UserInfo userinfo = getCreateWhoisUserInfoFor(s);
            userinfo.setServerHost(s1);
            userinfo.setServerInfo(s2);
        }

        public void handleWhoisUser(String s, String s1, String s2, String s3) {
            UserInfo userinfo = getCreateWhoisUserInfoFor(s);
            userinfo.setClientUserName(s1);
            userinfo.setClientHostName(s2);
            userinfo.setRealName(s3);
        }

        public void invalidNickName(String s, String s1) throws IOException {
            imListener.invalidNick(IRCIMNetwork.this, s, s1);
        }

        public void join(String channel) throws IOException {
            imListener.meJoined(IRCIMNetwork.this, channel);
        }

        public void join(String s, User c) throws IOException {
            ((IRCChannel) getRoom(s)).joins(ircController.getChannelRoleByChannelName((IRCUser) c, s));
        }

        public void kick(String s, String s1, String s2, String s3) throws IOException {
            ((IRCChannel) getRoom(s1)).kicked(ircController.getChannelRoleByChannelName(
                    ircController.getModifyCreateUser(s, null, null),
                    s1), ircController.getModifyCreateUser(s2, null, null), s3);
        }

        public void namReplyAddNick(String nick, char modifier) throws IOException {
            Lang.ASSERT_NOT_NULL(curRoom, "curRoom");
            IRCChannelParticipant rp = ircController.getChannelRoleByNickName((IRCChannel) curRoom, nick);
            if(rp==null)rp= (IRCChannelParticipant) ircController.createDefaultRole(
                    curRoom,ircController.getCreateUser(nick));
            switch (modifier) {
                case '+':
                    rp.setVoice(true);
                case '@':
                    rp.setOp(true);
                case '%':
                    rp.setHalfOp(true);
            }
            IRCChannel chan = (IRCChannel) curRoom;
            chan.addRoomRole(rp);
        }

        public void namReplyFinish() throws IOException {
            curRoom = null;
        }

        public void namReplyStart(String channelName) throws IOException {
            curRoom = getRoom(channelName);
            if (curRoom == null) {
                curRoom = createRoom(channelName, false);
            }
            Lang.ASSERT_NOT_NULL(curRoom, "curRoom");
        }

        public void nickChange(String s, String s1) throws IOException {
            imListener.nickChange(IRCIMNetwork.this, s, s1);
            super.nickChange(s, s1);
        }

        public Room prepareRoomForMessage(User sender, String nickOrChannelTo) {
            String nickFrom = ((IRCUser) sender).getActiveNick();
            //Chat.dbg("to: " + nickOrChannelTo);
            boolean isChannel = IRCChannelUtil.isChannelName(nickOrChannelTo);
            String roomName = isChannel ? nickOrChannelTo : nickFrom;
            Room room = getRoom(roomName);
            if (room == null) {
                room = createRoom(roomName, !isChannel);
                if (!isChannel)
                    imListener.meQueried(IRCIMNetwork.this, roomName);
            }
            return room;
        }

        public void quit(String s, String s1) throws IOException {
            imListener.quit(IRCIMNetwork.this, s, s1);
        }

        public void registering() {
            statusRoomListenerInternal.registering(statusRoom);
        }

        /**
         * setInviteOnly method comment.
         */
        public void setInviteOnly(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn) {
            IRCChannel IRCChannel = (IRCChannel) getRoom(channelName);
            if (IRCChannel != null)
                IRCChannel.setInviteOnly(isModeOn);
        }

        /**
         * setKeyOff method comment.
         */
        public void setKeyOff(java.lang.String senderSpec, java.lang.String channelName) {
            IRCChannel IRCChannel = (IRCChannel) getRoom(channelName);
            if (IRCChannel != null)
                IRCChannel.setChannelKeyUsed(false);
        }

        /**
         * setKeyOn method comment.
         */
        public void setKeyOn(java.lang.String senderSpec, java.lang.String channelName, java.lang.String key) {
            IRCChannel IRCChannel = (IRCChannel) getRoom(channelName);
            if (IRCChannel != null) {
                IRCChannel.setChannelKeyUsed(true);
                IRCChannel.setChannelKey(key);
            }
        }

        /**
         * setLimitOff method comment.
         */
        public void setLimitOff(java.lang.String senderSpec, java.lang.String channelName) {
            IRCChannel IRCChannel = (IRCChannel) getRoom(channelName);
            if (IRCChannel != null)
                IRCChannel.setLimited(false);
        }

        /**
         * setLimitOn method comment.
         */
        public void setLimitOn(java.lang.String senderSpec, java.lang.String channelName, int limit) {
            IRCChannel IRCChannel = (IRCChannel) getRoom(channelName);
            if (IRCChannel != null) {
                IRCChannel.setLimited(true);
                IRCChannel.setLimit(limit);
            }
        }

        /**
         * setModerated method comment.
         */
        public void setModerated(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn) {
            IRCChannel IRCChannel = (IRCChannel) getRoom(channelName);
            if (IRCChannel != null)
                IRCChannel.setModerated(isModeOn);
        }

        /**
         * setNoExternalMessages method comment.
         */
        public void setNoExternalMessages(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn) {
            IRCChannel IRCChannel = (IRCChannel) getRoom(channelName);
            if (IRCChannel != null)
                IRCChannel.setNoExternalMessages(isModeOn);
        }

        /**
         * setOnlyOpsChangeTopic method comment.
         */
        public void setOnlyOpsChangeTopic(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn) {
            IRCChannel IRCChannel = (IRCChannel) getRoom(channelName);
            if (IRCChannel != null)
                IRCChannel.setOnlyOpsChangeTopic(isModeOn);
        }

        /**
         * setOperator method comment.
         */
        public void setOperator(java.lang.String senderSpec, java.lang.String channelName, java.lang.String nickNameAffected, boolean isModeOn) {
            IRCChannel channel = (IRCChannel) getRoom(channelName);
            if (channel != null) {
                IRCChannelParticipant rp = ircController.getChannelRoleByNickName(channel, nickNameAffected);
                rp.setOp(isModeOn);
            }
        }

        /**
         * setPrivate method comment.
         */
        public void setPrivate(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn) {
            IRCChannel IRCChannel = (IRCChannel) getRoom(channelName);
            if (IRCChannel != null)
                IRCChannel.setPrivateChannelMode(isModeOn);
        }

        /**
         * setSecret method comment.
         */
        public void setSecret(java.lang.String senderSpec, java.lang.String channelName, boolean isModeOn) {
            IRCChannel IRCChannel = (IRCChannel) getRoom(channelName);
            if (IRCChannel != null)
                IRCChannel.setSecret(isModeOn);
        }

        /**
         * setVoice method comment.
         */
        public void setVoice(java.lang.String senderSpec, java.lang.String channelName, java.lang.String nickNameAffected, boolean isModeOn) {
            IRCChannel channel = (IRCChannel) getRoom(channelName);
            if (channel != null) {
                IRCChannelParticipant rp = ircController.getChannelRoleByNickName(channel, nickNameAffected);
                rp.setVoice(isModeOn);
            }

        }

        /**
         * shouldCreateQuery method comment.
         *
         * @param userFrom    org.openmim.irc.mvc.User_
         * @param textMessage java.lang.String
         * @return boolean
         */
        public boolean shouldCreateQuery(User userFrom, java.lang.String textMessage) {
            Lang.ASSERT_NOT_NULL(userFrom, "userFrom");
            return !(ignoreList.isIgnored(userFrom));
        }

        public void showStatus(String s) {
            reportStatus(s);
        }

        public void textMessageReceived(User userFrom, String nickOrChannelTo, String s2) {
            if (!getIgnoreList().isIgnored(userFrom)) {
                String nickFrom = ((IRCUser) userFrom).getActiveNick();
                imListener.message(IRCIMNetwork.this, prepareRoomForMessage(userFrom, nickOrChannelTo), nickFrom, s2);
            }
        }

        public void unhandledCommand(IRCMessage ircmessage) {
            Vector middleParts = ircmessage.getMiddleParts();
            String trailing = ircmessage.getTrailing();
            StringBuffer stringbuffer = new StringBuffer();
            String sender = ircmessage.getPrefix();
            if (sender != null && !sender.equalsIgnoreCase(client.getIrcServerAddress())) {
                try {
                    sender = ircmessage.getSender().getActiveNick();
                }
                catch (Exception _ex) {
//                    ExceptionUtil.handleException(_ex);
                }
                stringbuffer.append(sender).append(": ");
            }
            Enumeration enumeration = middleParts.elements();
            if (enumeration.hasMoreElements())
                enumeration.nextElement();
            //noinspection StatementWithEmptyBody
            for (; enumeration.hasMoreElements(); stringbuffer.append(enumeration.nextElement()).append(' ')) ;
            if (trailing != null)
                stringbuffer.append(trailing);
            imListener.unhandledCommand(stringbuffer.toString());
            System.err.println("*** " + Integer.toString(ircmessage.getCommand()) + " " + stringbuffer);
        }

        public void welcome(String newNick, String s) throws IOException {
            super.welcome(newNick, s);
            ancientNick = newNick;
            statusRoomListenerInternal.welcome(statusRoom, newNick, s);
        }
    }

    private UserInfo getCreateWhoisUserInfoFor(String nick) {
        UserInfo ui = nicklow2userInfo.get(nick.toLowerCase());
        if (ui == null) {
            ui = new UserInfo();
            nicklow2userInfo.put(nick.toLowerCase(), ui);
        }
        ui.setBeingLoaded(true);
        ui.setNick(nick);
        return ui;
    }

    private Room getRoom(String roomName) {
        return lowname2roompanel.get(roomName.toLowerCase());
    }

    private Room getCreateRoom(String roomName) {
        Room room = getRoom(roomName);
        if (room == null) room = createRoom(roomName, !roomName.startsWith("#") && !roomName.startsWith("&"));
        return room;
    }

    public static interface NotifyListListener {
        void notifyListChanged(NotifyListChangedEvent ev);
    }

    public static class NotifyListImpl {
        private Vector<NotifyListItem> items = new Vector<NotifyListItem>();
        private NotifyListListener notifyListListener;
        private boolean enabled = true;

        /**
         * NotifyListImpl constructor comment.
         */
        public NotifyListImpl() {
        }

        /**
         * add method comment.
         *
         * @param item ...
         */
        public synchronized void add(NotifyListItem item) {
            Lang.ASSERT_NOT_NULL(item, "item");
            remove(item.getUserMask());
            items.addElement(item);
            fireNotifyListChanged(item, NotifyListChangedEvent.ITEM_ADDED);
        }

        /**
         * addNotifyListListener method comment.
         */
        public synchronized void addNotifyListListener(NotifyListListener l) {
            Lang.ASSERT(this.notifyListListener == null, "multiple listeners not implemented");
            Lang.ASSERT_NOT_NULL(l, "l");
            notifyListListener = l;
        }

        /**
         * assignFrom method comment.
         */
        public synchronized void assignFrom(NotifyListImpl newValue) {
            Lang.ASSERT_NOT_NULL(newValue, "newValue");
            removeAll();
            Enumeration<NotifyListItem> e = newValue.getItems();
            while (e.hasMoreElements()) {
                add(e.nextElement());
            }
            //fireNotifyListChanged();
        }

        /**
         * getItems method comment.
         */
        public synchronized Object clone() {
            NotifyListImpl o = new NotifyListImpl();
            Enumeration<NotifyListItem> e = getItems();
            while (e.hasMoreElements()) {
                NotifyListItem item = e.nextElement();
                o.add((NotifyListItem) item.clone());
            }
            return o;
        }

        /**
         * elementAt method comment.
         *
         * @param pos int
         * @return squirrelchat.applet.NotifyListItem
         */
        public synchronized NotifyListItem elementAt(int pos) {
            return items.elementAt(pos);
        }

        /**
         * removeNotifyListListener method comment.
         */
        private synchronized void fireNotifyListChanged(int id) {
            if (notifyListListener != null) {
                notifyListListener.notifyListChanged(new NotifyListChangedEvent(this, id));
            }
        }

        /**
         * removeNotifyListListener method comment.
         */
        private synchronized void fireNotifyListChanged(NotifyListItem item, int id) {
            if (notifyListListener != null) {
                notifyListListener.notifyListChanged(new NotifyListChangedEvent(this, item, id));
            }
        }

        /**
         * getItemByNick method comment.
         */
        public synchronized NotifyListItem getItemByNick(java.lang.String nick) {
            Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(nick, "nick");
            Lang.ASSERT(nick.trim().length() == nick.length(), "nick.trim().length() must be == nick.length(): \"" + nick + "\"");
            Enumeration<NotifyListItem> e = getItems();
            while (e.hasMoreElements()) {
                NotifyListItem item = e.nextElement();
                if (item.getUserMask().equalsIgnoreCase(nick))
                    return item;
            }
            return null;
        }

        /**
         * getItems method comment.
         */
        public Enumeration<NotifyListItem> getItems() {
            return items.elements();
        }

        /**
         * Insert the method's description here.
         * Creation date: (04.11.00 08:03:41)
         *
         * @return boolean
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * remove method comment.
         */
        public synchronized NotifyListItem remove(String userMask) {
            Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(userMask, "userMask");
            Lang.ASSERT(userMask.trim().length() == userMask.length(), "userMask.trim() must be == userMask.length(): \"" + userMask + "\"");
            Enumeration<NotifyListItem> e = getItems();
            while (e.hasMoreElements()) {
                NotifyListItem item = e.nextElement();
                if (item.getUserMask().equalsIgnoreCase(userMask))
                    return remove(item);
            }
            return null;
        }

        /**
         * remove method comment.
         */
        public synchronized NotifyListItem remove(NotifyListItem item) {
            Lang.ASSERT_NOT_NULL(item, "item");
            Lang.ASSERT(items.removeElement(item), "item not found");
            fireNotifyListChanged(item, NotifyListChangedEvent.ITEM_REMOVED);
            return item;
        }

        /**
         * removeAll method comment.
         */
        public synchronized void removeAll() {
            while (!items.isEmpty()) {
                remove(items.elementAt(0));
            }
            Lang.ASSERT(items.isEmpty(), "Items must be empty");
        }

        /**
         * removeNotifyListListener method comment.
         *
         * @param l ...
         */
        public synchronized void removeNotifyListListener(NotifyListListener l) {
            Lang.ASSERT_NOT_NULL(l, "l");
            Lang.ASSERT(l.equals(this.notifyListListener), "l must be equal to this.notifyListListener here, but it is not.  l=" + l + "; this.nll=" + notifyListListener);
            notifyListListener = null;
        }

        /**
         * Insert the method's description here.
         * Creation date: (04.11.00 08:03:41)
         *
         * @param newEnabled boolean
         */
        public synchronized void setEnabled(boolean newEnabled) {
            if (enabled != newEnabled) {
                enabled = newEnabled;
                fireNotifyListChanged(newEnabled ? NotifyListChangedEvent.NOTIFY_LIST_ENABLED : NotifyListChangedEvent.NOTIFY_LIST_DISABLED);
            }
        }

        /**
         * getItems method comment.
         *
         * @return ...
         */
        public synchronized int size() {
            return items.size();
        }

        /**
         * getItems method comment.
         */
        public String toString() {
            return items.toString();
        }

    }

    public static interface NotifyListItem {

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         */
        Object clone();

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @return org.openmim.irc.regexp.IRCMask
         */
        org.openmim.irc.regexp.IRCMask getIrcMask();

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:47:22)
         *
         * @return java.lang.String
         */
        String getNote();

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:46:48)
         *
         * @return java.lang.String
         */
        String getUserMask();

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:47:22)
         *
         * @return java.lang.String
         */
        boolean isPerformWhoisNeeded();

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @param newNote java.lang.String
         */
        void setNote(java.lang.String newNote);

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @param newPerformWhoisNeeded boolean
         */
        void setPerformWhoisNeeded(boolean newPerformWhoisNeeded);

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @param newUserMask java.lang.String
         */
        void setUserMask(java.lang.String newUserMask);
    }

    public static class NotifyListChangedEvent extends EventObject {
        private final NotifyListItem notifyListItem;
        private final int id;
        public final static int ITEM_REMOVED = 0;
        public final static int ITEM_ADDED = 1;
        public final static int NOTIFY_LIST_ENABLED = 2;
        public final static int NOTIFY_LIST_DISABLED = 3;

        public NotifyListChangedEvent(NotifyListImpl notifyList, int id) {
            super(notifyList);
            Lang.ASSERT_NOT_NULL(notifyList, "notifyList");
            Lang.ASSERT(id == NOTIFY_LIST_ENABLED || id == NOTIFY_LIST_DISABLED, "here, id must be either " + NOTIFY_LIST_DISABLED + " or " + NOTIFY_LIST_ENABLED + ", but it is " + id);
            this.id = id;
            this.notifyListItem = null;
        }

        public NotifyListChangedEvent(NotifyListImpl notifyList, NotifyListItem item, int id) {
            super(notifyList);
            Lang.ASSERT_NOT_NULL(notifyList, "notifyList");
            Lang.ASSERT_NOT_NULL(item, "item");
            Lang.ASSERT(id == ITEM_REMOVED || id == ITEM_ADDED, "here, id must be either " + ITEM_REMOVED + " or " + ITEM_ADDED + ", but it is " + id);
            this.id = id;
            this.notifyListItem = item;
        }

        /**
         * Insert the method's description here.
         * Creation date: (03.12.00 18:50:20)
         *
         * @return int
         */
        public int getId() {
            return id;
        }

        /**
         * Insert the method's description here.
         * Creation date: (03.12.00 18:50:20)
         *
         * @return squirrelchat.applet.NotifyListItem
         */
        public NotifyListImpl getNotifyList() {
            return (NotifyListImpl) getSource();
        }

        /**
         * Insert the method's description here.
         * Creation date: (03.12.00 18:50:20)
         *
         * @return squirrelchat.applet.NotifyListItem
         */
        public NotifyListItem getNotifyListItem() {
            Lang.ASSERT(id == ITEM_REMOVED || id == ITEM_ADDED, "id must be either " + ITEM_REMOVED + " or " + ITEM_ADDED + ", but it is " + id);
            return notifyListItem;
        }
    }

    public void sendSilentUpdateNotifyListRequest() throws IOException {
        final int sz = getNotifyList().size();
        if (sz > 0)

        {
            Vector<String> v = new Vector<String>(sz);
            Enumeration<NotifyListItem> e = getNotifyList().getItems();
            while (e.hasMoreElements()) {
                NotifyListItem item = e.nextElement();
                v.addElement(item.getUserMask());
            }
            String[] nicks = new String[v.size()];
            v.copyInto(nicks);
            client.sendIsonRequest(nicks);
            reportStatus(("Notify list update request sent."));
        } else

            reportStatus("Notify list is empty, /notify ignored.");
    }

    final class NotifyListUpdaterThread extends Thread {
        private long ISON_REQUEST_PERIOD_MILLIS = 60 * 1000;

        public void run() {
            syncPrint(this + " thread spawned.");
            try {
                while (true) {
                    if (isConnected())
                        sendSilentUpdateNotifyListRequest();
                    sleep(ISON_REQUEST_PERIOD_MILLIS);
                }
            }
            catch (ThreadDeath td) {
                throw td;
            }
            catch (InterruptedException ix) {
            }
            catch (Throwable tr) {
                ExceptionUtil.handleException(tr);
            }
            finally {
                reportStatus("thread finished.");
            }
        }
    }

    public static int REQPARAM_CHANNEL_USERLIST_WIDTH_PIXELS;
    public static boolean REQPARAM_HIDE_QUIT_MESSAGES_DETAILS;
    public static boolean REQPARAM_HIDE_PART_MESSAGES_DETAILS;
    public static boolean REQPARAM_SHOW_ADDRESS_ON_PART;
    public static boolean REQPARAM_SHOW_ADDRESS_ON_JOIN;
    public static final String SYS_PREFIX = "*** ";
    public static String REQPARAM_AD_IMAGE_URLS_DELIMITED_BY_SPACES;
    public static String REQPARAM_AD_DOC_URLS_DELIMITED_BY_SPACES;
    public static long REQPARAM_AD_DELAY_MILLIS;
    public static int REQPARAM_AD_HEIGHT;
    public static int REQPARAM_AD_WIDTH;
    public static boolean REQPARAM_AD_DISABLED;
    public static boolean REQPARAM_AD_QUERY_AD_DISABLED;
    public static boolean REQPARAM_AD_CHANNEL_AD_DISABLED;
    public static boolean REQPARAM_AD_STATUS_AD_DISABLED;

    public Font font;
    private IgnoreList ignoreList;
    private Hashtable<String, Room> lowname2roompanel;
    private boolean autoRejoinChannelOnKick;
    private Map<String, UserInfo> nicklow2userInfo;
    private String ancientNick;
    private NotifyListImpl notifyList;
    private Hashtable<String, String> nicksLastOnline_nicklow2nick = new Hashtable<String, String>();
    //    private CommandInterpreter commandInterpreter = new CommandInterpreter();
    private NotifyListUpdaterThread notifyListUpdaterThread;

    //
    private String ipAddressToConnect;
    private String ipAddressOfRealServer;
    private int portToConnect;
    private String realname;

    {
        lowname2roompanel = new Hashtable<String, Room>();
        nicklow2userInfo = new Hashtable<String, UserInfo>();
    }

    private Room addRoomPanel(String nameLowercased, Room roompanel) {
        lowname2roompanel.put(nameLowercased, roompanel);
        return roompanel;
    }

    public void channelClicked(String channelName) {
        try {
            Room roompanel = getRoom(channelName);
            if (roompanel == null)
                sendJoinTo(channelName);
            imListener.channelClicked(IRCIMNetwork.this, channelName);
        }
        catch (Exception exception) {
            handleException(exception);
        }
    }

    public NotifyListItem createNotifyListItem(String userMask, String note, boolean performWhois) {
        return new NotifyListItemImpl(userMask, note, performWhois);
    }

    public class NotifyListItemImpl implements NotifyListItem {
        private String userMask;
        private IRCMask ircMask;
        private String note;
        private boolean performWhoisNeeded;

        /**
         * NotifyListItemImpl constructor comment.
         */
        private NotifyListItemImpl(String userMask, IRCMask ircMask, String note, boolean performWhois) {
            Lang.ASSERT_NOT_NULL(userMask, "userMask");
            Lang.ASSERT_NOT_NULL(ircMask, "ircMask");
            this.userMask = userMask;
            this.ircMask = ircMask;
            setNote(note);
            setPerformWhoisNeeded(performWhois);
        }

        /**
         * NotifyListItemImpl constructor comment.
         */
        public NotifyListItemImpl(String userMask, String note, boolean performWhois) {
            setUserMask(userMask);
            setNote(note);
            setPerformWhoisNeeded(performWhois);
        }

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @return java.lang.String
         */
        public Object clone() {
            return new NotifyListItemImpl(userMask, (IRCMask) ircMask.clone(), note, performWhoisNeeded);
        }

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @return org.openmim.irc.regexp.IRCMask
         */
        public org.openmim.irc.regexp.IRCMask getIrcMask() {
            return ircMask;
        }

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @return java.lang.String
         */
        public java.lang.String getNote() {
            return note;
        }

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @return java.lang.String
         */
        public java.lang.String getUserMask() {
            return userMask;
        }

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @return boolean
         */
        public boolean isPerformWhoisNeeded() {
            return performWhoisNeeded;
        }

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @param newNote java.lang.String
         */
        public void setNote(java.lang.String newNote) {
            Lang.ASSERT_NOT_NULL(newNote, "note");
            note = newNote;
        }

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @param newPerformWhoisNeeded boolean
         */
        public void setPerformWhoisNeeded(boolean newPerformWhoisNeeded) {
            performWhoisNeeded = newPerformWhoisNeeded;
        }

        /**
         * Insert the method's description here.
         * Creation date: (22 окт 2000 20:54:47)
         *
         * @param newUserMask java.lang.String
         */
        public void setUserMask(java.lang.String newUserMask) {
            Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(newUserMask, "userMask");
            ircMask = new IRCMask(newUserMask);
            userMask = newUserMask;
        }

        public String toString() {
            return "\n" + userMask + " (" + ircMask + "): " + note + ", whois " + (performWhoisNeeded ? "on" : "off");
        }
    }

    public Room createRoom(String roomName, boolean isQuery) {
        ircController=getIRCProtocol().getLocalClient();
        Room bmroom;
        if (isQuery) {
            bmroom = ircController.onJoinedQuery(ircController.getCreateUser(roomName));
        } else {
            bmroom = ircController.onMeJoinedChannel(roomName,ircController.getCreateUser(getActiveNick())).getRoom();
        }
        reportStatus(("Joined to") + " " + roomName);
        String roomName_low = roomName.toLowerCase();
        addRoomPanel(roomName_low, bmroom);
        if (!isQuery) {
            IRCChannel chan = (IRCChannel) bmroom;
            chan.joins(ircController.getChannelRoleByNickName(chan, getActiveNick()));
        }
        return bmroom;
    }

    public void dbg(String s) {
        Logger.log(getClass().getSimpleName() + " " + s);
    }

    private void destroyAll() {
        dbg("Chat.destroyAll");
        disconnectAndClose();
        removeChildren();
        dbg("Chat.destroyAll done");
    }

    public void disconnectAndClose() {
        reportStatus(("Disconnecting..."));
        removeChildren();
        cleanupOnDisconnect();
    }

    public boolean getAutoRejoinChannelOnKick() {
        return autoRejoinChannelOnKick;
    }

    public IRCClient getIRCProtocol() {
        return client;
    }

//    public CommandInterpreter getCommandInterpreter() {
//        return commandInterpreter;
//    }

    public final String getCopyrightText() {
        return "Copyright (C) 2000, 2008 Evgenii Philippov, " + getCopyrightURL();
    }

    public final String getCopyrightURL() {
        return "http://sf.net/projects/openmim/";
    }

    protected synchronized IgnoreList getIgnoreList() {
        if (ignoreList == null)
            ignoreList = new IgnoreList();
        Lang.ASSERT_NOT_NULL(ignoreList, "ignoreList");
        return ignoreList;
    }

    public String getActiveNick() {
        IRCClient ircclient = getIRCProtocol();
        if (ircclient != null)
            return getIRCProtocol().getNick();
        else
            return ancientNick;
    }

    private synchronized NotifyListUpdaterThread getNotifyListUpdaterThread() {
        if (notifyListUpdaterThread == null) {
            notifyListUpdaterThread = new NotifyListUpdaterThread();
        }
        return notifyListUpdaterThread;
    }

    public Enumeration<Room> getRoomPanels() {
        return lowname2roompanel.elements();
    }

    public void handleException(Throwable tr) {
        ExceptionUtil.handleException(tr);
        reportStatus(("Error:") + " " + tr);
    }

    public void handleIsonReply(java.lang.String serverAddr, java.lang.String[] nicksOnline) throws java.io.IOException {
        synchronized (getNotifyList()) {
            Hashtable<String, String> oldNickList = nicksLastOnline_nicklow2nick;
            nicksLastOnline_nicklow2nick = new Hashtable<String, String>();
            Lang.ASSERT_NOT_NULL(oldNickList, "oldNickList");
            Lang.ASSERT_NOT_NULL(nicksOnline, "nicksOnline");
            for (int i = 0; i < nicksOnline.length; i++) {
                Lang.ASSERT_NOT_NULL(nicksOnline[i], "nicksOnline[" + i + "]");
                String nick = nicksOnline[i].trim();
                if (nick.length() == 0)
                    continue;
                String lownick = nick.toLowerCase();
                nicksLastOnline_nicklow2nick.put(lownick, nick);
                String oldNick = oldNickList.remove(lownick);
                if (oldNick == null)
                    onNickLoggedOn(nick);
            }
            Enumeration<String> e = oldNickList.elements();
            while (e.hasMoreElements()) {
                String oldNick = e.nextElement();
                onNickLoggedOff(oldNick);
            }
        }
    }

    public void handleModeChangeRaw(String senderSpecification, String s, String modeFor, Vector vector) {
        IRCChannel channel = (IRCChannel) getRoom(s);
        if (channel != null) {
            IRCChannelParticipant rp = ircController.getChannelRoleByNickName(channel, modeFor);
            rp.modeChange(senderSpecification, modeFor, vector);
        } else {
//            for (RoomListenerInternal rl : getStatusRoom().getRoomListeners())
//                rl.modeChange(senderSpecification, modeFor, vector);
        }
    }

    public boolean isIgnored(User_ user) {
        Lang.ASSERT_NOT_NULL(user, "user");
        if (getIgnoreList().isIgnored(user.getNick() + "!*@*"))
            return true;
        User c = user.getUser();
        if (c != null)
            return getIgnoreList().isIgnored(c);
        else
            return false;
    }

    public boolean isMe(String nick) {
        if (nick == null)
            return false;
        else
            return nick.equalsIgnoreCase(getActiveNick());
    }

    public Room join(String s) throws IOException {
        Room roompanel = getRoom(s);
        if (roompanel == null) {
            roompanel = createRoom(s, false);
            Lang.ASSERT_NOT_NULL(roompanel, "roomPanel just created after joining to room");
        } else ;
//            for (RoomListenerInternal rl : roompanel.getRoomListeners())
//                rl.joined(getIRCProtocol().getLocalClient());      //todo
        getIRCProtocol().sendChannelBasicModesRequest(s);
        Lang.ASSERT_NOT_NULL(roompanel, "roomPanel returned after joining to room");
        return roompanel;
    }

    void nickChange(String oldNick, String newNick) {
        if (oldNick.equalsIgnoreCase(getActiveNick())) {
            imListener.nickChange(IRCIMNetwork.this, oldNick, newNick);
            setNick(newNick);
        }
        for (Enumeration<Room> e = lowname2roompanel.elements(); e.hasMoreElements();) {
            Room rp = e.nextElement();
//            for (RoomListenerInternal rl : rp.getRoomListeners())
//                rl.nickChange(oldNick, newNick); //todo
        }
    }

    public void nickClicked(String s) {
        Room roompanel = getRoom(s);
        if (roompanel == null) {
            if (!s.equalsIgnoreCase(getActiveNick()))
                createRoom(s, true);
        }
    }

    public synchronized void notifyListChanged(NotifyListImpl notifyList) {
        boolean runThread = getNotifyList().isEnabled() && getNotifyList().size() > 0;
        if (runThread) {
            if (getNotifyListUpdaterThread().isAlive()) {
                try {
                    sendUpdateNotifyListRequest();
                }
                catch (IOException ix) {
                    handleException(ix);
                }
            } else {
                getNotifyListUpdaterThread().start();
            }
        } else {
            stopNotifyListUpdaterThread();
        }
    }

    /**
     * Insert the method's description here.
     * Creation date: (22 окт 2000 23:09:12)
     *
     * @return squirrelchat.applet.NotifyListEditor
     */
    public void notifyListChanged(NotifyListChangedEvent ev) {
        notifyListChanged(ev.getNotifyList());
        if (ev.getId() == NotifyListChangedEvent.ITEM_ADDED || ev.getId() == NotifyListChangedEvent.ITEM_REMOVED) {
            String note = ev.getNotifyListItem().getNote();
            if (StringUtil.isNullOrTrimmedEmpty(note))
                note = "";
            else
                note = " (" + note + ")";
            String key = "Notify list item " + (ev.getId() == NotifyListChangedEvent.ITEM_ADDED ? "added" : "removed") + " for";
            reportStatus((key) + " " + ev.getNotifyListItem().getUserMask() + note);
        } else {
            String key = "Notify list " + (ev.getId() == NotifyListChangedEvent.NOTIFY_LIST_ENABLED ? "enabled." : "disabled.");
            reportStatus((key));
        }
    }

    public void onDeOp(boolean flag) {
    }

    public void onDeVoice(boolean flag) {
    }

    public void onHighlight() {
    }

    public void onListLoaded() {
    }

    protected void onMeKicked(String s, String s1, String s2) {
        if (autoRejoinChannelOnKick && client != null)
            try {
                imListener.kickedTryingToRejoin(IRCIMNetwork.this, s);
                client.join(s);
            }
            catch (Exception exception) {
                handleException(exception);
            }
    }

    public void onMeQueried() {
    }

    public void onNickLoggedOff(String nick) {
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(nick, "nick");
        NotifyListItem item = getNotifyList().getItemByNick(nick);
        String note = (item == null ? null : StringUtil.mkNullAndTrim(item.getNote()));
        imListener.userQuit(IRCIMNetwork.this, isMe(nick), nick, note);
    }

    public void onNickLoggedOn(String nick) {
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(nick, "nick");
        NotifyListItem item = getNotifyList().getItemByNick(nick);
        String note = (item == null ? null : StringUtil.mkNullAndTrim(item.getNote()));
        imListener.userLoggedOn(IRCIMNetwork.this, isMe(nick), nick, note);
        if (item != null && item.isPerformWhoisNeeded()) {
            retrieveUserProperties(nick);
        }
    }

    public void onOp(boolean flag) {
    }

    public void onPart(String s, String s1) {
        reportStatus(("Parted from the room") + " " + s + " (" + s1 + ")");
        final Room room = removeRoom(s);
//        for (RoomListenerInternal rl : room.getRoomListeners())
//            rl.meParts();//todo

    }

    public void onServerReplyBanListEnd(String s) throws IOException {
    }

    public void onServerReplyBanListItem(String s, String s1) throws IOException {
    }

    public void onSomeoneIsKicked() {
    }

    public void onSomeoneJoinedChan() {
    }

    public void onSomeoneParts() {
    }

    public void onVoice(boolean flag) {
    }

    protected void quit(String s, String s1) {
    }

    public void reconnect() throws Exception {
        if (isConnected())
            disconnectAndClose();
        removeChildren();
        connect();
    }

    protected void removeChildren() {
    }

    Room removeRoom(String s) {
        String s1 = s.toLowerCase();
        return removeRoomLow(s1);
    }

    Room removeRoomLow(String roomNameLowercased) {
        return lowname2roompanel.remove(roomNameLowercased);
    }

    public synchronized void removeWhoisUserInfoFor(String s) {
        String s1 = s.toLowerCase();
        nicklow2userInfo.remove(s1);
    }

    public void renameRoom(String oldName, String newName) {
        if (!oldName.equalsIgnoreCase(newName)) {
            String oldNameLow = oldName.toLowerCase();
            Room rp = getRoom(oldName);
            removeRoomLow(oldNameLow);
            String newNameLow = newName.toLowerCase();
            addRoomPanel(newNameLow, rp);
        }
    }

    public void reportStatus(String s) {
        syncPrint(s);
//        getStatusRoom().reportStatus(s);
    }

    public void sendJoinTo(String s) throws IOException {
        reportStatus(("Joining room") + " " + s + "...");
        client.join(s);
    }

    public void sendUpdateNotifyListRequest() throws IOException {
        final int sz = getNotifyList().size();
        if (sz > 0) {
            Vector<String> v = new Vector<String>(sz);
            Enumeration<NotifyListItem> e = getNotifyList().getItems();
            while (e.hasMoreElements()) {
                NotifyListItem item = e.nextElement();
                v.addElement(item.getUserMask());
            }
            String[] nicks = new String[v.size()];
            v.copyInto(nicks);
            getIRCProtocol().sendIsonRequest(nicks);
            reportStatus("Notify list update request sent.");
        } else
            reportStatus("Notify list is empty, /notify ignored.");
    }

    public void setIgnored(User client, boolean isIgnored) {
        Lang.ASSERT_NOT_NULL(client, "client");
        String mask = IRCMaskUtil.createDefaultIRCMask((IRCUser) client);
        setIgnored(new IRCMask(mask), isIgnored);
    }

    public void setIgnored(User_ nickName, boolean isIgnored) {
        setIgnored(getIRCProtocol().getLocalClient().getCreateUser(nickName.getNick()), isIgnored);
    }

    public void setIgnored(org.openmim.irc.regexp.IRCMask mask, boolean isIgnored) {
        Lang.ASSERT_NOT_NULL(mask, "mask");
        imListener.willBeIgnored(IRCIMNetwork.this, mask, isIgnored);
        getIgnoreList().setIgnored(mask, isIgnored);
    }

    public void setIgnored(String mask, boolean isIgnored) {
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(mask, "mask");
        setIgnored(new org.openmim.irc.regexp.IRCMask(mask), isIgnored);
    }

    private void setNick(String nick) {
        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(nick, "nick");
        this.ancientNick = nick;
    }

    public synchronized void retrieveUserProperties(String s) {
        String s1 = s.toLowerCase();
        UserInfo userinfo = nicklow2userInfo.get(s1);
        //sys(("User_ properties: retrieving information about") + " " + s, isMe(s), Room.makeColorPrefix(Chat.REQPARAM_COLORCODE_DEFAULT_MSG_FG));
        try {
            getIRCProtocol().sendCommand("whois", new String[]{s, s}, null);
        }
        catch (java.io.IOException ex) {
            handleException(ex);
        }
    }

    public void urlClicked(String url) {
        try {
            imListener.urlClicked(url);
        }
        catch (Exception exception) {
            Logger.printException(exception);
        }
    }

    public void welcome() throws IOException {
//        String s = getParameter("room");
//        String s1 = getParameter("nickserv.password");//todo
//        if (s1 != null && s1.trim().length() > 0)
//            getIRCProtocol().sendNickServIdentify(s1, null);
//        sendJoinTo(s);//todo
    }

//    public final class CommandInterpreter {
//        public final class NotifyCommand implements Command {
//            private NotifyList notifyList;
//            private Chat notifyListWindowProducer;
//            private Chat updater;
//            private NotifyListItemFactory MN2Factory;
//
//            /**
//             * NotifyCommand constructor comment.
//             */
//            public NotifyCommand(NotifyList notifyList, NotifyListItemFactory MN2Factory, Chat notifyListWindowProducer) {
//                Lang.ASSERT_NOT_NULL(notifyList, "notifyList");
//                Lang.ASSERT_NOT_NULL(MN2Factory, "MN2Factory");
//                Lang.ASSERT_NOT_NULL(notifyListWindowProducer, "notifyListWindowProducer");
//                this.notifyList = notifyList;
//                this.MN2Factory = MN2Factory;
//                this.notifyListWindowProducer = notifyListWindowProducer;
//                this.updater = notifyListWindowProducer;
//            }
//
//            /**
//             * Performs a notify command.
//             * <p/>
//             * /notify [-shr] [(on|off|[+]ircmask) [note]]
//             * <p/>
//             * You can turn notify on and off by typing /notify on
//             * or off respectively.
//             * <p/>
//             * The -sh switches can be used to show or hide the
//             * notify list window respectively.
//             * <p/>
//             * The -r switch removes the specified nickname from
//             * your notify list.
//             * <p/>
//             * The note is optional and allows you to specify
//             * a little note for each nickname.
//             * <p/>
//             * If you prefix a nickname with a + sign then
//             * a /whois on the nickname will be done as leave of the notify.
//             * <p/>
//             * You can manually force mIRC to update the notify list
//             * by typing /notify with no parameters.
//             */
//            public void perform(char[] options, java.lang.String[] args, CommandContext context) throws ExpectException {
//                StatusReporter reporter = context.getStatusReporter();
//                Lang.ASSERT_NOT_NULL(options, "options");
//                Lang.ASSERT_NOT_NULL(args, "args");
//
//                //
//                boolean s = false;
//                boolean h = false;
//                boolean r = false;
//                for (int i = 0; i < options.length; i++) {
//                    switch (options[i]) {
//                        case 's':
//                            Lang.EXPECT(h == false, "Cannot specify both -s and -h.  Command ignored.");
//                            s = true;
//                            break;
//                        case 'h':
//                            Lang.EXPECT(s == false, "Cannot specify both -s and -h.  Command ignored.");
//                            h = true;
//                            break;
//                        case 'r':
//                            r = true;
//                            break;
//                        default:
//                            Lang.EXPECT(false, "Invalid option switch.  Syntax: /notify [-shr] [(on|off|[+]ircmask) [note]].  Command ignored.");
//                    }
//                }
//
//                //
//                boolean noargs = false;
//                //
//                boolean on = false;
//                boolean off = false;
//                boolean performWhois = false;
//                String firstArg = null;
//                String note = null;
//
//                //  /notify [-shr] [(on|off|[+]ircmask) [note]]
//                switch (args.length) {
//                    case 0:
//                        Lang.EXPECT(!r, "-r: Item to remove not specified. Command ignored.");
//                        noargs = true;
//                        break;
//                    case 1:
//                    case 2:
//                        firstArg = args[0];
//                        Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(firstArg, "args[0]");
//                        //
//                        if (firstArg.equalsIgnoreCase("on")) {
//                            Lang.EXPECT(!r, "'-r' switch cannot be used in conjunction with 'on'.  Command ignored.");
//                            Lang.EXPECT(args.length == 1, "Invalid number of args: " + args.length + ", must be 1");
//                            on = true;
//                        } else if (firstArg.equalsIgnoreCase("off")) {
//                            Lang.EXPECT(!r, "'-r' switch cannot be used in conjunction with 'off'.  Command ignored.");
//                            Lang.EXPECT(args.length == 1, "Invalid number of args: " + args.length + ", must be 1");
//                            off = true;
//                        } else {
//                            if (firstArg.charAt(0) == '+') {
//                                Lang.EXPECT(!r, "'-r' switch cannot be used in conjunction with '+'.  Command ignored.");
//                                performWhois = true;
//                                firstArg = firstArg.substring(1);
//                                Lang.ASSERT_NOT_NULL(firstArg, "firstArg after substring");
//                                Lang.EXPECT(firstArg.length() > 0, "ircmask not specified.  Command ignored.");
//                            }
//                            Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(firstArg, "firstArg");
//                            if (args.length == 2) {
//                                note = args[1];
//                                Lang.ASSERT_NOT_NULL(note, "note");
//                            } else
//                                note = "";
//                        }
//                        break;
//                    default:
//                        Lang.EXPECT(false, "Invalid number of args: " + args.length + ", must be 0, 1, or 2.");
//                }
//
////analysis finished, actually perform things
//                if (s)
//                    performShowNotifyListWindow(true);
//                else {
//                    if (h)
//                        performShowNotifyListWindow(false);
//                }
//
////
//                if (noargs) {
//                    if (!s && !h)
//                        performUpdateNotifyList(context);
//                } else {
//                    if (on)
//                        notifyList.setEnabled(true);
//                    else {
//                        if (off)
//                            notifyList.setEnabled(false);
//                        else {
//                            if (r)
//                                performRemoveItem(firstArg);
//                            else {
//                                performAddItem(firstArg, performWhois, note);
//                            }
//                        }
//                    }
//                }
//            }
//
//            protected void performAddItem(String ircMask, boolean performWhois, String note) {
//                notifyList.add(MN2Factory.createNotifyListItem(ircMask, note, performWhois));
//            }
//
//            protected void performRemoveItem(String ircMask) {
//                notifyList.remove(ircMask);
//            }
//
//            protected void performShowNotifyListWindow(boolean show) {
//                notifyListWindowProducer.getNotifyListEditor().setVisible(show);
//            }
//
//            protected void performUpdateNotifyList(CommandContext context) {
//                try {
//                    updater.sendUpdateNotifyListRequest(context.getStatusReporter());
//                }
//                catch (Exception ex) {
//                    context.getRoomPanel().handleException(ex);
//                }
//            }
//
//        }
//
//        public class ClearCommand implements Command {
//
//            /**
//             * ClearCommand constructor comment.
//             */
//            public ClearCommand() {
//                super();
//            }
//
//            /**
//             * perform method comment.
//             */
//            public void perform(char[] options, java.lang.String[] args, CommandContext context) throws ExpectException {
//                Lang.ASSERT_NOT_NULL(options, "options");
//                Lang.ASSERT_NOT_NULL(args, "args");
//                Lang.ASSERT_NOT_NULL(context, "context");
//                Lang.EXPECT(options.length == 0, "No options allowed.");
//                Lang.EXPECT(args.length == 0, "No arguments allowed.");
//                context.getRoomPanel().clearTextArea();
//            }
//        }
//
//        public final class ChangeServerCommand implements Command {
//            private Chat applet;
//
//            /**
//             * NotifyCommand constructor comment.
//             */
//            public ChangeServerCommand(Chat applet) {
//                Lang.ASSERT_NOT_NULL(applet, "applet");
//                this.applet = applet;
//            }
//
//            /**
//             * Performs a change server command.
//             * <p/>
//             * "/server" serveraddress [[":"] serverport]
//             */
//            public void perform(char[] options, java.lang.String[] args, CommandContext context) throws ExpectException {
//                Lang.ASSERT_NOT_NULL(context, "context");
//                final StatusReporter reporter = context.getStatusReporter();
//                Lang.ASSERT_NOT_NULL(options, "options");
//                Lang.ASSERT_NOT_NULL(args, "args");
//
//                //
//                Lang.EXPECT(options.length == 0, "No option switches allowed. Command ignored.");
//                Lang.EXPECT(args.length > 0, "Server address is not specified. Command syntax: /server serveraddress [[\":\"] serverport]. Command ignored.");
//                Lang.EXPECT(args.length <= 3, "Too many arguments. Command syntax: /server serveraddress [[\":\"] serverport]. Command ignored.");
//                //
//                StringBuffer args_sb = new StringBuffer(args[0]);
//                if (args.length >= 2)
//                    args_sb.append(" ").append(args[1]);
//                if (args.length >= 3)
//                    args_sb.append(" ").append(args[2]);
//                StringTokenizer st = new StringTokenizer(args_sb.toString(), " :\u0009\r\n");
//                Lang.EXPECT(st.hasMoreTokens(), "Server address is not specified. Command syntax: /server serveraddress [[\":\"] serverport]. Command ignored.");
//                String serverAddr = st.nextToken();
//                Lang.ASSERT_NOT_NULL_NOR_EMPTY(serverAddr, "serverAddr");
//                int serverPort = 6667;
//                if (st.hasMoreTokens()) {
//                    String port_s = st.nextToken();
//                    try {
//                        serverPort = Integer.parseInt(port_s);
//                    }
//                    catch (NumberFormatException ex) {
//                        Lang.EXPECT(false, "Invalid port specified: " + StringUtil.toPrintableString(port_s) + ". Must be integer number like \"6667\". Command ignored.");
//                    }
//                    Lang.EXPECT(!st.hasMoreTokens(), "Too many arguments. Command syntax: /server serveraddress [[\":\"] serverport]. Command ignored.");
//                }
//
//                //
//                performServerChange(serverAddr, serverPort);
//            }
//
//            protected void performServerChange(String serveraddress, int serverport) {
//                if (applet.isConnected())
//                    applet.disconnect();
//                applet.connect(serveraddress, serverport);
//            }
//        }
//
//        private NotifyCommand notifyCommand;
//        private ClearCommand clearCommand;
//        private ChangeServerCommand changeServerCommand;
//
//        /**
//         * CommandInterpreter constructor comment.
//         */
//        public CommandInterpreter() {
//        }
//
//        /**
//         * Insert the method's description here.
//         * Creation date: (24.11.00 08:23:20)
//         *
//         * @return squirrelchat.applet.NotifyCommand
//         */
//        protected synchronized ChangeServerCommand getChangeServerCommand() {
//            if (changeServerCommand == null) {
//                changeServerCommand = new ChangeServerCommand();
//            }
//            return changeServerCommand;
//        }
//
//        /**
//         * Insert the method's description here.
//         * Creation date: (24.11.00 08:23:20)
//         *
//         * @return squirrelchat.applet.NotifyCommand
//         */
//        protected synchronized ClearCommand getClearCommand() {
//            if (clearCommand == null) {
//                clearCommand = new ClearCommand();
//            }
//            return clearCommand;
//        }
//
//        /**
//         * Insert the method's description here.
//         * Creation date: (24.11.00 08:23:20)
//         *
//         * @return squirrelchat.applet.NotifyCommand
//         */
//        protected synchronized NotifyCommand getNotifyCommand() {
//            if (notifyCommand == null) {
//                notifyCommand = new NotifyCommand(applet.getNotifyList(), applet, applet);
//            }
//            return notifyCommand;
//        }
//
//        /**
//         * Returns true if (client-side) "/notify ..." style command
//         * is recognized.  Returns false if the command is external
//         * (that is, if it has not been recognized).
//         * <p/>
//         * The command is synchronously processed if recognized.
//         */
//        public boolean processCommandIfKnown(String s, CommandContext context) throws ExpectException {
//            Lang.ASSERT_NOT_NULL(context, "context");
//            StatusReporter reporter = context.getStatusReporter();
//            Lang.ASSERT_NOT_NULL(reporter, "reporter");
//            if (s == null || s.length() == 0 || s.charAt(0) != '/')
//                return false;
//            StringTokenizer st = new StringTokenizer(s, " ");
//            Lang.ASSERT(st.hasMoreTokens(), s + " must have at least one token");
//            String command = st.nextToken().substring(1).toLowerCase();
//            if (command.length() == 0)
//                return false;
//            StringBuffer optionChars_sb = new StringBuffer();
//            Vector args_v = new Vector();
//            while (st.hasMoreTokens()) {
//                String tok = st.nextToken();
//                if (tok.length() == 0)
//                    continue;
//                if (tok.charAt(0) == '-') {
//                    optionChars_sb.append(tok.substring(1));
//                } else
//                    args_v.addElement(tok);
//            }
//            String[] args = new String[args_v.size()];
//            args_v.copyInto(args);
//            char[] optionChars = optionChars_sb.toString().toCharArray();
//            if ("notify".equals(command)) {
//                getNotifyCommand().perform(optionChars, args, context);
//                return true;
//            }
//            if ("server".equals(command)) {
//                getChangeServerCommand().perform(optionChars, args, context);
//                return true;
//            }
//            if ("clear".equals(command)) {
//                getClearCommand().perform(optionChars, args, context);
//                return true;
//            }
//            return false;
//        }
//    }

}