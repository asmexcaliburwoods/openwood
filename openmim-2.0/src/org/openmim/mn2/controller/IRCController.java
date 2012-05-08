package org.openmim.mn2.controller;

import org.openmim.mn2.model.*;
import squirrel_util.Lang;
import squirrel_util.Logger;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IRCController {
    private MN2Factory MN2Factory;
    private String hostName;
    private String activeNick;
    private IRCUser myUser;
	private final IMNetwork net;

    //QueryUserImpl(
    public IRCController(IMNetwork net, String nickName, String realName, String userName, String loginPassword, MN2Factory mn2Factory) {
    	this.net=net;
        resolveLocalInetAddress();
        this.MN2Factory = mn2Factory;
        activeNick=nickName;
        myUser=getCreateUser(activeNick);
    }



    /**
     * Gets existing User_ from the localClient,
     * or, if it does not exist, creates a new IRCUserImpl,
     * but does not associates a localClient with this new created instance.
     * This association should be done using localClient's joinXxx() methods
     * instead, if needed.
     * <p/>
     * The operation getCreateUser(String nickName) is needed to restore
     * User_'s association links if they exist.
     * <p/>
     * Parameter nickName cannot be null and
     * must represent a valid IRC nickname.<br>
     */
    public IRCUser getCreateUser(String nickName) {
        return getModifyCreateUser(nickName, null, null);
    }

    /**
     * Gets existing User_ from the localClient,
     * or, if it does not exist, creates a new IRCUserImpl,
     * but does not associates a localClient with this new created instance.
     * This association should be done using localClient's joinXxx() methods
     * instead, if needed.
     * <p/>
     * If the client exists but does not know its login name/host name,
     * and these are already known, they are assigned here.
     * <p/>
     * The operation getModifyCreateClient(...) is needed to restore
     * (and update) User_'s association links if they exist.
     * <p/>
     * Parameter nickName cannot be null and
     * must represent a valid IRC nickname.<br>
     * Parameters userName, hostName can be null.
     */
    public IRCUser getModifyCreateUser(String activeNickName, String userName, String hostName) {
        IRCUser currentUser = getUserByActiveNickName(activeNickName);
        if (currentUser == null){
            currentUser = new IRCUserImpl(activeNickName);
            activeNickCanonical2user.put(activeNickName.toLowerCase(),currentUser);
        }
        if (currentUser.getUserName() == null && userName != null)
            currentUser.setUserName(userName);
        if (currentUser.getHostName() == null && hostName != null)
            currentUser.setHostName(hostName);
        return currentUser;
    }

    private IRCUser getUserByActiveNickName(String nickName) {
        return activeNickCanonical2user.get(nickName.toLowerCase());
    }

    Server createServer(String hostNameOfRealServer, String redirdHostName,
                           int redirdPort, String realName, List<String> nickNames,
                           String password, String identdUserName){
        return null;
    }
    /**
     * joinChannel method comment.
     */
    public IRCChannelParticipant onMeJoinedChannel(String channelName,IRCUser user) {
        IRCChannel IRCChannel = new IRCChannelImpl(net, channelName, MN2Factory);
        channelNameCanonical2channel.put(channelName.toLowerCase(),IRCChannel);
        IRCChannelParticipant participant = (IRCChannelParticipant) createDefaultRole(IRCChannel,user);
        IRCChannel.addRole(participant);
        return participant;
    }

    public RoomParticipant createDefaultRole(Room room, IRCUser user) {
        RoomParticipant rp;
        if(room instanceof IRCChannel)rp=new IRCChannelParticipantImpl();
        else rp=new RoomParticipantImpl();
        rp.setUser(user);
        if(room!=null)rp.setRoom(room);
        return rp;
    }

    /**
     * onJoinedQuery method comment.
     */
    public Query onJoinedQuery(IRCUser userFrom) {
        Lang.ASSERT_NOT_NULL(userFrom, "userFrom");
        final RoomParticipant my = createDefaultRole(null, getMyUser());
        final RoomParticipant his = createDefaultRole(null, userFrom);
        Query query = new QueryImpl(net, my, his);
        my.setRoom(query);
        his.setRoom(query);
        query.addRoomRole(my);
        query.addRoomRole(his);
        return query;
    }

    private void resolveLocalInetAddress() {
        try {
            setHostName(InetAddress.getLocalHost().getHostName());
        }
        catch (java.net.UnknownHostException ex) {
            Logger.printException(ex);
            setHostName("localhost");
        }
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public IRCChannelParticipant getChannelRoleByChannelName(IRCUser user, String channelName) {
        return getChannelRoleByNickName(getChannelJoinedByChannelName(channelName),user.getActiveNick());
    }

    public void onQuit(IRCUser userFrom) {
        for (IRCChannel channel : channelNameCanonical2channel.values()) {
            Set<RoomParticipant> set=channel.getRoomParticipants();
            for (RoomParticipant participant : set) {
                IRCUser user2= (IRCUser) participant.getUser();
                if(user2.getActiveNick().equalsIgnoreCase(userFrom.getActiveNick())){
                    channel.deleteRoomRole(participant);
                    break;
                }
            }
        }
    }

    private Map<IRCUser,Query> myParty2query=new HashMap<IRCUser, Query>();
    private Map<String,IRCChannel> channelNameCanonical2channel=new HashMap<String, IRCChannel>();
    private Map<String,IRCUser> activeNickCanonical2user=new HashMap<String, IRCUser>();

    public IRCChannel getChannelJoinedByChannelName(String channelName) {
        return channelNameCanonical2channel.get(channelName.toLowerCase());
    }

    public IRCChannelParticipant getChannelRoleByNickName(IRCChannel ircChannel, String nick) {
        Set<RoomParticipant> set=ircChannel.getRoomParticipants();
        for (RoomParticipant participant : set) {
            IRCUser user= (IRCUser) participant.getUser();
            if(user.getActiveNick().equalsIgnoreCase(nick))return (IRCChannelParticipant) participant;
        }
        return null;
    }

    public void welcome_setNickName(String s) {
        activeNick=s;
    }

    public String getActiveNick() {
        return activeNick;
    }

    public IRCUser getMyUser() {
        return myUser;
    }
}
